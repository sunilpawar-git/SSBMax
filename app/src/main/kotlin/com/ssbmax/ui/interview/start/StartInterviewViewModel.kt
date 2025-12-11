package com.ssbmax.ui.interview.start

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.BuildConfig
import com.ssbmax.R
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.usecase.CheckInterviewPrerequisitesUseCase
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Start Interview screen
 *
 * Optimizes interview start by checking for pre-generated questions (instant start)
 * and showing progress during on-demand generation (30-60s).
 * Also loads past interview results for history display.
 */
@HiltViewModel
class StartInterviewViewModel @Inject constructor(
    private val checkPrerequisites: CheckInterviewPrerequisitesUseCase,
    private val interviewRepository: InterviewRepository,
    private val submissionRepository: SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val questionCacheRepository: com.ssbmax.core.domain.model.interview.QuestionCacheRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "StartInterviewVM"
    }

    private val _uiState = MutableStateFlow(StartInterviewUiState())
    val uiState: StateFlow<StartInterviewUiState> = _uiState.asStateFlow()

    init {
        trackMemoryLeaks("StartInterviewViewModel")
        loadInterviewHistory()
    }

    /**
     * Load past interview results for history display
     */
    private fun loadInterviewHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }

            try {
                val user = observeCurrentUser().first()
                val userId = user?.id ?: return@launch

                Log.d(TAG, "Loading interview history for user: $userId")

                // Collect past results from the Flow
                interviewRepository.getUserResults(userId)
                    .catch { e ->
                        ErrorLogger.log(e, "Failed to load interview history")
                        _uiState.update { it.copy(isLoadingHistory = false) }
                    }
                    .collect { results ->
                        Log.d(TAG, "Loaded ${results.size} past interview results")
                        _uiState.update {
                            it.copy(
                                pastResults = results.sortedByDescending { r -> r.completedAt },
                                isLoadingHistory = false
                            )
                        }
                    }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading interview history")
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    fun checkEligibility() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = context.getString(R.string.interview_checking_eligibility),
                    error = null
                )
            }

            try {
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    setError(R.string.interview_error_auth)
                    return@launch
                }

                val result = checkPrerequisites(
                    userId = userId,
                    bypassSubscriptionCheck = BuildConfig.DEBUG
                )

                if (result.isFailure) {
                    ErrorLogger.log(
                        throwable = result.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to check interview prerequisites"
                    )
                    setError(R.string.interview_error_generic)
                    return@launch
                }

                val prerequisiteResult = result.getOrNull()
                if (prerequisiteResult == null) {
                    setError(R.string.interview_error_generic)
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
                setError(R.string.interview_error_generic)
            }
        }
    }

    fun createSession(consentGiven: Boolean) {
        if (!_uiState.value.canStartInterview()) {
            _uiState.update { it.copy(error = context.getString(R.string.interview_prerequisites_not_met)) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = context.getString(R.string.interview_loading_checking_cache),
                    error = null
                )
            }

            try {
                val user = observeCurrentUser().first()
                val userId = user?.id ?: run {
                    setError(R.string.interview_error_auth)
                    return@launch
                }

                // Fetch latest PIQ submission ID
                _uiState.update { it.copy(loadingMessage = context.getString(R.string.interview_loading_piq_data)) }

                val piqResult = submissionRepository.getLatestPIQSubmission(userId)
                if (piqResult.isFailure) {
                    ErrorLogger.log(
                        throwable = piqResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to fetch latest PIQ submission"
                    )
                    setError(R.string.interview_error_fetch_piq)
                    return@launch
                }

                val piqSubmission = piqResult.getOrNull()
                val piqSnapshotId = piqSubmission?.id ?: run {
                    setError(R.string.interview_error_no_piq)
                    return@launch
                }

                // Check if cached questions are available for instant start
                _uiState.update { it.copy(loadingMessage = context.getString(R.string.interview_loading_checking_cache)) }

                val cachedQuestions = questionCacheRepository.getPIQQuestions(
                    piqSnapshotId = piqSnapshotId,
                    limit = 1,
                    excludeUsed = true
                ).getOrDefault(emptyList())

                val hasCachedQuestions = cachedQuestions.isNotEmpty()

                Log.d(TAG, if (hasCachedQuestions) {
                    "✅ Cached questions available - instant start!"
                } else {
                    "⚠️ No cached questions - will generate on-demand (30-60s)"
                })

                // Create session - repository checks cache first (instant) or generates on-demand (30-60s)
                _uiState.update {
                    it.copy(
                        loadingMessage = if (hasCachedQuestions) {
                            context.getString(R.string.interview_loading_preparing)
                        } else {
                            context.getString(R.string.interview_loading_generating_questions)
                        },
                        isGeneratingQuestions = !hasCachedQuestions
                    )
                }

                val result = interviewRepository.createSession(
                    userId = userId,
                    mode = InterviewMode.VOICE_BASED,
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
                            isGeneratingQuestions = false,
                            loadingMessage = null,
                            error = context.getString(R.string.interview_error_create_session)
                        )
                    }
                    return@launch
                }

                val session = result.getOrNull()
                if (session == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGeneratingQuestions = false,
                            loadingMessage = null,
                            error = context.getString(R.string.interview_error_create_session)
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isGeneratingQuestions = false,
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
                        isGeneratingQuestions = false,
                        loadingMessage = null,
                        error = context.getString(R.string.interview_error_generic)
                    )
                }
            }
        }
    }

    private fun setError(stringResId: Int) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isGeneratingQuestions = false,
                loadingMessage = null,
                error = context.getString(stringResId)
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
