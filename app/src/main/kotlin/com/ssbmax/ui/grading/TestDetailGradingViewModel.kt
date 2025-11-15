package com.ssbmax.ui.grading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.NotificationPriority
import com.ssbmax.core.domain.model.NotificationType
import com.ssbmax.core.domain.model.SSBMaxNotification
import com.ssbmax.core.domain.model.TestSubmission
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.repository.TestSubmissionRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Test Detail Grading Screen (Assessor)
 * Handles grading submission and sending notifications to students
 */
@HiltViewModel
class TestDetailGradingViewModel @Inject constructor(
    private val testSubmissionRepository: TestSubmissionRepository,
    private val notificationRepository: NotificationRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GradingUiState())
    val uiState: StateFlow<GradingUiState> = _uiState.asStateFlow()

    /**
     * Load submission details for grading
     */
    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            testSubmissionRepository.getSubmissionById(submissionId)
                .onSuccess { submission ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    submission = submission,
                    grade = submission.instructorScore ?: submission.aiPreliminaryScore ?: 0f,
                    remarks = submission.instructorFeedback ?: "",
                    error = null
                )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load submission"
                    )
                }
        }
    }

    /**
     * Update grade value
     */
    fun updateGrade(grade: Float) {
        _uiState.value = _uiState.value.copy(grade = grade)
    }

    /**
     * Update remarks text
     */
    fun updateRemarks(remarks: String) {
        _uiState.value = _uiState.value.copy(remarks = remarks)
    }

    /**
     * Submit grading with notification to student
     */
    fun submitGrading() {
        val submission = _uiState.value.submission ?: return

        if (_uiState.value.grade < 0 || _uiState.value.grade > 100) {
            _uiState.value = _uiState.value.copy(error = "Grade must be between 0 and 100")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            // Get current instructor ID from authenticated user
            val currentUser = observeCurrentUser().first()
            val instructorId = currentUser?.id ?: run {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "You must be logged in to submit grading"
                )
                return@launch
            }

            // Update submission with grade and remarks
            val updatedSubmission = submission.copy(
                instructorScore = _uiState.value.grade,
                finalScore = _uiState.value.grade,
                instructorFeedback = _uiState.value.remarks,
                gradedAt = System.currentTimeMillis(),
                gradingStatus = com.ssbmax.core.domain.model.GradingStatus.GRADED,
                instructorId = instructorId
            )

            testSubmissionRepository.updateSubmission(updatedSubmission)
                .onSuccess {
                    // Send notification to student
                    sendGradingNotification(updatedSubmission)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = error.message ?: "Failed to submit grading"
                    )
                }
        }
    }

    /**
     * Send FCM notification to student about grading completion
     */
    private suspend fun sendGradingNotification(submission: TestSubmission) {
        val notification = SSBMaxNotification(
            id = UUID.randomUUID().toString(),
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
            createdAt = System.currentTimeMillis()
        )

        notificationRepository.saveNotification(notification)
            .onSuccess {
                // TODO: Trigger Cloud Function to send FCM push notification
                // This would typically call a backend API to send the push notification
                // For now, we've saved it to the local database
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    gradingSubmitted = true,
                    error = null
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Grading saved but notification failed: ${error.message}"
                )
            }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset submitted state (for navigation)
     */
    fun resetSubmittedState() {
        _uiState.value = _uiState.value.copy(gradingSubmitted = false)
    }
}

/**
 * UI state for grading screen
 */
data class GradingUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submission: TestSubmission? = null,
    val grade: Float = 0f,
    val remarks: String = "",
    val gradingSubmitted: Boolean = false,
    val error: String? = null
)
