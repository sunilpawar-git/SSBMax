package com.ssbmax.shared.presentation.interviewstart

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.model.interview.QuestionCacheRepository
import com.ssbmax.shared.domain.usecase.CheckInterviewPrerequisitesUseCase
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of `app/.../ui/interview/start/StartInterviewViewModel.kt`.
 *
 * Uses a real `androidx.lifecycle.ViewModel` with `viewModelScope` (Phase 1 of
 * the KMP-convergence plan, converged with `app`'s existing ViewModel idiom
 * and this module's `viewModelOf`/`koinViewModel()` DI/screen conventions --
 * see [com.ssbmax.shared.presentation.oir.OIRTestViewModel]'s doc comment for
 * the precedent this mirrors). No manual `CoroutineScope`/`close()` needed.
 *
 * Deviations from the Android original, all deliberate and documented:
 * - `android.util.Log` -> [DomainLogger]; `Context.getString(R.string...)`
 *   for `loadingMessage`/`error` -> plain hardcoded English strings, same
 *   documented convention every other Phase 5 ViewModel uses (see
 *   `PPDTTestViewModel`'s doc comment) rather than a KMP string-resource
 *   plumbing exercise for values that never reach the UI as anything but
 *   opaque display text.
 * - `BuildConfig.DEBUG`/`BuildConfig.BYPASS_INTERVIEW_PREREQUISITES` (Android
 *   `BuildConfig`-only) are NOT ported -- always calls
 *   [CheckInterviewPrerequisitesUseCase] with both bypass flags `false`,
 *   same "fails safe, more restrictive" precedent as
 *   `CheckTestEligibilityUseCase`'s own doc comment.
 * - `trackMemoryLeaks`/`MemoryLeakTracker` (Android-only) dropped -- no KMP
 *   equivalent, and `viewModelScope` already cancels automatically in
 *   `onCleared`, same precedent as `OIRTestViewModel`'s doc comment.
 */
class StartInterviewViewModel(
    private val checkPrerequisites: CheckInterviewPrerequisitesUseCase,
    private val interviewRepository: InterviewRepository,
    private val submissionRepository: SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val questionCacheRepository: QuestionCacheRepository,
    private val logger: DomainLogger
) : ViewModel() {
    private val tag = "StartInterviewViewModel"

    private val _uiState = MutableStateFlow(StartInterviewUiState())
    val uiState: StateFlow<StartInterviewUiState> = _uiState.asStateFlow()

    init {
        loadInterviewHistory()
    }

    /** Load past interview results for history display. See [PendingInterviewSession]'s doc comment for the pre-existing dead-field gap this carries forward. */
    private fun loadInterviewHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            try {
                val userId = observeCurrentUser().first()?.id ?: return@launch

                interviewRepository.getUserResults(userId)
                    .catch { e ->
                        logger.e(tag, "Failed to load interview history", e)
                        _uiState.update { it.copy(isLoadingHistory = false) }
                    }
                    .collect { results ->
                        _uiState.update {
                            it.copy(
                                pastResults = results.sortedByDescending { r -> r.completedAt },
                                isLoadingHistory = false
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Exception loading interview history", e)
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    fun checkEligibility() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadingMessage = "Checking eligibility...", error = null)
            }

            try {
                val userId = observeCurrentUser().first()?.id ?: run {
                    setError("Authentication required")
                    return@launch
                }

                val result = checkPrerequisites(
                    userId = userId,
                    bypassSubscriptionCheck = false,
                    bypassPrerequisites = false
                )

                if (result.isFailure) {
                    logger.e(tag, "Failed to check interview prerequisites", result.exceptionOrNull())
                    setError("An error occurred")
                    return@launch
                }

                val prerequisiteResult = result.getOrNull()
                if (prerequisiteResult == null) {
                    setError("An error occurred")
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Exception checking interview eligibility", e)
                setError("An error occurred")
            }
        }
    }

    fun createSession(consentGiven: Boolean) {
        if (!_uiState.value.canStartInterview()) {
            _uiState.update { it.copy(error = "Prerequisites not met") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadingMessage = "Checking for cached questions...", error = null)
            }

            try {
                val userId = observeCurrentUser().first()?.id ?: run {
                    setError("Authentication required")
                    return@launch
                }

                _uiState.update { it.copy(loadingMessage = "Loading PIQ data...") }

                val piqResult = submissionRepository.getLatestPIQSubmission(userId)
                if (piqResult.isFailure) {
                    logger.e(tag, "Failed to fetch latest PIQ submission", piqResult.exceptionOrNull())
                    setError("Failed to fetch PIQ data")
                    return@launch
                }

                val piqSnapshotId = piqResult.getOrNull()?.id ?: run {
                    setError("No PIQ submission found")
                    return@launch
                }

                _uiState.update { it.copy(loadingMessage = "Checking for cached questions...") }

                val cachedQuestions = questionCacheRepository.getPIQQuestions(
                    piqSnapshotId = piqSnapshotId,
                    limit = 1,
                    excludeUsed = true
                ).getOrDefault(emptyList())

                val hasCachedQuestions = cachedQuestions.isNotEmpty()
                logger.d(
                    tag,
                    if (hasCachedQuestions) "Cached questions available - instant start"
                    else "No cached questions - will generate on-demand (30-60s)"
                )

                _uiState.update {
                    it.copy(
                        loadingMessage = if (hasCachedQuestions) "Preparing interview..." else "Generating personalized questions (30-60s)...",
                        isGeneratingQuestions = !hasCachedQuestions
                    )
                }

                val result = interviewRepository.createSession(
                    userId = userId,
                    mode = InterviewMode.VOICE_BASED,
                    piqSnapshotId = piqSnapshotId,
                    consentGiven = consentGiven
                )

                val session = result.getOrNull()
                if (result.isFailure || session == null) {
                    logger.e(tag, "Failed to create interview session", result.exceptionOrNull())
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGeneratingQuestions = false,
                            loadingMessage = null,
                            error = "Failed to create session"
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Exception creating interview session", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isGeneratingQuestions = false,
                        loadingMessage = null,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(isLoading = false, isGeneratingQuestions = false, loadingMessage = null, error = message)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
