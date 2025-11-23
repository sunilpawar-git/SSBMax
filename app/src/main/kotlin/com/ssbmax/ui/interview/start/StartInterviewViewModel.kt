package com.ssbmax.ui.interview.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.BuildConfig
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.usecase.CheckInterviewPrerequisitesUseCase
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Start Interview screen
 *
 * Responsibilities:
 * - Check prerequisites (PIQ, OIR, PPDT, subscription)
 * - Create interview session
 * - Navigate to session screen
 *
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker
 * - Uses viewModelScope for all coroutines (auto-cancelled)
 * - No static references or context leaks
 */
@HiltViewModel
class StartInterviewViewModel @Inject constructor(
    private val checkPrerequisites: CheckInterviewPrerequisitesUseCase,
    private val interviewRepository: InterviewRepository,
    private val submissionRepository: SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartInterviewUiState())
    val uiState: StateFlow<StartInterviewUiState> = _uiState.asStateFlow()

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("StartInterviewViewModel")
    }

    /**
     * Select interview mode (text or voice)
     */
    fun selectMode(mode: InterviewMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    /**
     * Check if user meets all prerequisites
     */
    fun checkEligibility() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Checking eligibility...",
                    error = null
                )
            }

            try {
                // Get current user
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Authentication required"
                        )
                    }
                    return@launch
                }

                // Check prerequisites (bypass subscription check in debug builds)
                val result = checkPrerequisites(
                    userId = userId,
                    desiredMode = _uiState.value.selectedMode,
                    bypassSubscriptionCheck = BuildConfig.DEBUG
                )

                if (result.isFailure) {
                    ErrorLogger.log(
                        throwable = result.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to check interview prerequisites"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to check eligibility"
                        )
                    }
                    return@launch
                }

                val prerequisiteResult = result.getOrNull()
                if (prerequisiteResult == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to check eligibility"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        prerequisiteResult = prerequisiteResult,
                        isEligible = prerequisiteResult.isEligible
                    )
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception checking interview eligibility")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    /**
     * Create interview session
     *
     * Prerequisites must be met before calling this.
     * Fetches latest PIQ submission ID automatically.
     */
    fun createSession(consentGiven: Boolean) {
        if (!_uiState.value.canStartInterview()) {
            _uiState.update { it.copy(error = "Prerequisites not met") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Creating interview session...",
                    error = null
                )
            }

            try {
                // Get current user
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Authentication required"
                        )
                    }
                    return@launch
                }

                // Fetch latest PIQ submission ID
                val piqResult = submissionRepository.getLatestPIQSubmission(userId)
                if (piqResult.isFailure) {
                    ErrorLogger.log(
                        throwable = piqResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to fetch latest PIQ submission"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to fetch PIQ data"
                        )
                    }
                    return@launch
                }

                val piqSubmission = piqResult.getOrNull()
                val piqSnapshotId = piqSubmission?.id ?: run {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "No PIQ submission found"
                        )
                    }
                    return@launch
                }

                // Create session
                // NOTE: Force TEXT_BASED mode until voice recording UI is implemented
                val result = interviewRepository.createSession(
                    userId = userId,
                    mode = InterviewMode.TEXT_BASED,  // Force TEXT mode for now
                    piqSnapshotId = piqSnapshotId,
                    consentGiven = consentGiven
                )

                if (result.isFailure) {
                    ErrorLogger.log(
                        throwable = result.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to create interview session"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to create session"
                        )
                    }
                    return@launch
                }

                val session = result.getOrNull()
                if (session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to create session"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        sessionId = session.id,
                        isSessionCreated = true
                    )
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception creating interview session")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
