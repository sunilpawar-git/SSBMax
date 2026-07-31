package com.ssbmax.shared.presentation.phase2detail

import com.ssbmax.shared.domain.model.Phase2Progress
import com.ssbmax.shared.domain.model.TestProgress
import com.ssbmax.shared.domain.model.TestStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestProgressRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/phase/Phase2DetailViewModel.kt`.
 * Fetches Phase 2 (Psychology/GTO/Interview) progress from
 * [TestProgressRepository]. Note (unchanged from the Android original): the
 * domain model groups TAT/WAT/SRT/SD under one `psychologyProgress`, but the
 * UI still lists them as four separate test cards sharing that same progress.
 *
 * Uses a real `androidx.lifecycle.ViewModel` with `viewModelScope` (Phase 1 of
 * the KMP-convergence plan, see
 * [com.ssbmax.shared.presentation.oir.OIRTestViewModel]'s doc comment for the
 * precedent this mirrors). `ErrorLogger.log` (Android-only, Crashlytics-backed)
 * replaced with [DomainLogger], same seam every other ported ViewModel in
 * this phase uses.
 */
class Phase2DetailViewModel(
    private val testProgressRepository: TestProgressRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val logger: DomainLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(Phase2DetailUiState())
    val uiState: StateFlow<Phase2DetailUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "Phase2DetailViewModel"
    }

    init {
        loadPhase2Tests()
    }

    private fun loadPhase2Tests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val currentUser = observeCurrentUser().first()
                val userId = currentUser?.id

                if (userId == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Please login to view Phase 2 progress")
                    }
                    return@launch
                }

                testProgressRepository.getPhase2Progress(userId)
                    .catch { error ->
                        logger.e(TAG, "Error loading Phase 2 progress", error)
                        _uiState.update {
                            it.copy(isLoading = false, error = "Failed to load Phase 2 progress: ${error.message}")
                        }
                    }
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = Phase2Progress(
                            psychologyProgress = TestProgress(TestType.TAT),
                            gtoProgress = TestProgress(TestType.GTO_GD),
                            interviewProgress = TestProgress(TestType.IO)
                        )
                    )
                    .collect { phase2Progress ->
                        val allTests = buildPhase2Tests(phase2Progress)
                        val completedTests = allTests.filter {
                            it.status == TestStatus.COMPLETED || it.status == TestStatus.GRADED
                        }
                        val averageScore = if (completedTests.isNotEmpty()) {
                            completedTests.mapNotNull { it.latestScore }.average().toFloat()
                        } else {
                            0f
                        }

                        _uiState.value = Phase2DetailUiState(
                            tests = allTests,
                            averageScore = averageScore,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                logger.e(TAG, "Error in loadPhase2Tests", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    private fun buildPhase2Tests(phase2Progress: Phase2Progress): List<Phase2Test> {
        val psychologyTests = listOf(
            createPhase2Test(
                type = TestType.TAT,
                name = "TAT",
                subtitle = "Thematic Apperception Test",
                description = "You will be shown 12 pictures one by one. Write a story for each picture describing " +
                    "what led to the situation, what is happening, and what the outcome will be.",
                durationMinutes = 30,
                questionCount = 12,
                baseProgress = phase2Progress.psychologyProgress
            ),
            createPhase2Test(
                type = TestType.WAT,
                name = "WAT",
                subtitle = "Word Association Test",
                description = "You will be shown 60 words one by one for 15 seconds each. Write the first thought " +
                    "that comes to your mind as a sentence.",
                durationMinutes = 15,
                questionCount = 60,
                baseProgress = phase2Progress.psychologyProgress
            ),
            createPhase2Test(
                type = TestType.SRT,
                name = "SRT",
                subtitle = "Situation Reaction Test",
                description = "You will be given 60 real-life situations. Write what you would do in each situation " +
                    "as quickly and honestly as possible.",
                durationMinutes = 30,
                questionCount = 60,
                baseProgress = phase2Progress.psychologyProgress
            ),
            createPhase2Test(
                type = TestType.SD,
                name = "SD",
                subtitle = "Self Description",
                description = "Write about yourself, your parents, teachers, and friends' opinions about you. " +
                    "Be honest and introspective in your responses.",
                durationMinutes = 15,
                questionCount = 5,
                baseProgress = phase2Progress.psychologyProgress
            )
        )

        val gtoTests = listOf(
            createPhase2Test(
                type = TestType.GTO_GD,
                name = "GTO Tasks",
                subtitle = "Group Testing Officer",
                description = "Group tasks including Group Discussion, Group Planning Exercise, Progressive Group Task, " +
                    "Half Group Task, Lecturette, and Command Task. Assessed on leadership, cooperation, and problem-solving.",
                durationMinutes = 180,
                questionCount = 8,
                baseProgress = phase2Progress.gtoProgress
            )
        )

        val ioTests = listOf(
            createPhase2Test(
                type = TestType.IO,
                name = "Personal Interview",
                subtitle = "Interviewing Officer",
                description = "A personal interview with the Interviewing Officer to assess your personality, knowledge, " +
                    "current affairs awareness, and motivation for joining the armed forces.",
                durationMinutes = 45,
                questionCount = 1,
                baseProgress = phase2Progress.interviewProgress
            )
        )

        return psychologyTests + gtoTests + ioTests
    }

    private fun createPhase2Test(
        type: TestType,
        name: String,
        subtitle: String,
        description: String,
        durationMinutes: Int,
        questionCount: Int,
        baseProgress: TestProgress
    ): Phase2Test {
        return Phase2Test(
            type = type,
            name = name,
            subtitle = subtitle,
            description = description,
            durationMinutes = durationMinutes,
            questionCount = questionCount,
            status = baseProgress.status,
            latestScore = baseProgress.latestScore,
            attemptsCount = 0
        )
    }
}

/**
 * UI State for Phase 2 Detail Screen
 */
data class Phase2DetailUiState(
    val tests: List<Phase2Test> = emptyList(),
    val averageScore: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class Phase2Test(
    val type: TestType,
    val name: String,
    val subtitle: String,
    val description: String,
    val durationMinutes: Int,
    val questionCount: Int,
    val status: TestStatus,
    val latestScore: Float?,
    val attemptsCount: Int
)
