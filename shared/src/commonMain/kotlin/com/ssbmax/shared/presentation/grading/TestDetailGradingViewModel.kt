package com.ssbmax.shared.presentation.grading

import com.ssbmax.shared.domain.model.GradingStatus
import com.ssbmax.shared.domain.model.NotificationPriority
import com.ssbmax.shared.domain.model.NotificationType
import com.ssbmax.shared.domain.model.SSBMaxNotification
import com.ssbmax.shared.domain.model.TestSubmission
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.NotificationRepository
import com.ssbmax.shared.domain.repository.TestSubmissionRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * KMP port of the Android `app/.../ui/grading/TestDetailGradingViewModel.kt`
 * (Assessor grading flow).
 *
 * Follows the plain-class + own-`CoroutineScope` ViewModel pattern this phase
 * already established -- NOT `androidx.lifecycle.ViewModel`; the screen uses
 * `koinInject()`, not `koinViewModel()`.
 *
 * Deviations from the Android original:
 * - `ObserveCurrentUserUseCase` (an extra layer) replaced with
 *   [AuthRepository.currentUser]`.value` directly, matching this session's
 *   [com.ssbmax.shared.presentation.submissions.SubmissionsListViewModel] precedent.
 * - `java.util.UUID.randomUUID()` (JVM-only) replaced with the same
 *   `Clock.System.now()` + `kotlin.random.Random` ID-generation pattern
 *   already used by [com.ssbmax.shared.presentation.oir.OIRTestViewModel]
 *   (see this plan's own note on the `String.format`/UUID-style JVM-only
 *   gotchas).
 * - `System.currentTimeMillis()` (JVM-only) replaced with `Clock.System.now().toEpochMilliseconds()`.
 *
 * Everything else (grade validation, notification-on-grade-submit flow) is
 * an unchanged port.
 */
class TestDetailGradingViewModel(
    private val testSubmissionRepository: TestSubmissionRepository,
    private val notificationRepository: NotificationRepository,
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(GradingUiState())
    val uiState: StateFlow<GradingUiState> = _uiState.asStateFlow()

    fun close() {
        scope.cancel()
    }

    /**
     * Load submission details for grading. Also loads student name from profile.
     */
    fun loadSubmission(submissionId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            testSubmissionRepository.getSubmissionById(submissionId)
                .onSuccess { submission ->
                    val studentName = loadStudentName(submission.userId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            submission = submission,
                            studentName = studentName,
                            grade = submission.instructorScore ?: submission.aiPreliminaryScore ?: 0f,
                            remarks = submission.instructorFeedback ?: "",
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load submission"
                        )
                    }
                }
        }
    }

    private suspend fun loadStudentName(userId: String): String {
        return userProfileRepository.getUserProfile(userId)
            .first()
            .getOrNull()
            ?.fullName
            ?: "Unknown Student"
    }

    fun updateGrade(grade: Float) {
        _uiState.update { it.copy(grade = grade) }
    }

    fun updateRemarks(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    /**
     * Submit grading with notification to student
     */
    fun submitGrading() {
        val submission = _uiState.value.submission ?: return

        if (_uiState.value.grade < 0 || _uiState.value.grade > 100) {
            _uiState.update { it.copy(error = "Grade must be between 0 and 100") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val instructorId = authRepository.currentUser.value?.id ?: run {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = "You must be logged in to submit grading"
                    )
                }
                return@launch
            }

            val updatedSubmission = submission.copy(
                instructorScore = _uiState.value.grade,
                finalScore = _uiState.value.grade,
                instructorFeedback = _uiState.value.remarks,
                gradedAt = Clock.System.now().toEpochMilliseconds(),
                gradingStatus = GradingStatus.GRADED,
                instructorId = instructorId
            )

            testSubmissionRepository.updateSubmission(updatedSubmission)
                .onSuccess {
                    sendGradingNotification(updatedSubmission)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = error.message ?: "Failed to submit grading"
                        )
                    }
                }
        }
    }

    private suspend fun sendGradingNotification(submission: TestSubmission) {
        val notification = SSBMaxNotification(
            id = generateNotificationId(),
            userId = submission.userId,
            type = NotificationType.GRADING_COMPLETE,
            priority = NotificationPriority.HIGH,
            title = "Test Graded - ${submission.testType.displayName}",
            message = "Your ${submission.testType.displayName} test has been graded. Score: ${submission.finalScore?.toInt() ?: 0}/100",
            actionUrl = "ssbmax://submission/${submission.id}",
            actionData = mapOf(
                "submissionId" to submission.id,
                "testType" to submission.testType.name,
                "grade" to (submission.finalScore?.toString() ?: "0")
            ),
            isRead = false,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )

        notificationRepository.saveNotification(notification)
            .onSuccess {
                // A backend push (FCM) trigger is out of this repository-layer port's
                // scope, same as the Android original's own TODO -- the notification
                // is persisted (Firestore + local), not yet pushed.
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        gradingSubmitted = true,
                        error = null
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = "Grading saved but notification failed: ${error.message}"
                    )
                }
            }
    }

    private fun generateNotificationId(): String =
        "grading_notif_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100000, 999999)}"

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSubmittedState() {
        _uiState.update { it.copy(gradingSubmitted = false) }
    }
}

/**
 * UI state for grading screen
 */
data class GradingUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submission: TestSubmission? = null,
    val studentName: String = "",
    val grade: Float = 0f,
    val remarks: String = "",
    val gradingSubmitted: Boolean = false,
    val error: String? = null
)
