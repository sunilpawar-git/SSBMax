package com.ssbmax.shared.presentation.ppdtresult

import com.ssbmax.shared.domain.model.PPDTSubmission
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.scoring.EntryType
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.validation.SSBRecommendationUIModel
import com.ssbmax.shared.domain.validation.ValidationIntegration
import com.ssbmax.shared.ui.components.result.UnifiedResultUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of `app/.../ui/tests/ppdt/PPDTSubmissionResultViewModel.kt`.
 *
 * This is the async-analysis *observation* half of the PPDT vertical (see
 * [com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger] for the
 * *scheduling* half, which is deliberately NOT wired to a real worker yet).
 * The Android original's own doc comment already calls this the "GTO
 * pattern": observe `submissions/{id}` in real time via
 * [SubmissionRepository.observePPDTSubmission]; the moment `analysisStatus`
 * flips to [AnalysisStatus.COMPLETED], fetch the full result from
 * `ppdt_results/{id}` via [SubmissionRepository.getPPDTResult] and cancel the
 * listener. This is exactly the pattern the KMP migration plan's task brief
 * for this session asked to be identified and preserved: nothing here awaits
 * a WorkManager job directly, it only watches a Firestore status field —
 * which ports to KMP with zero behavioral change, regardless of whatever
 * eventually writes that status field on either platform.
 *
 * `CancellationException` contract preserved verbatim from the Android
 * original: a navigate-away cancels `viewModelScope`, which surfaces as
 * `CancellationException` inside the `collect` — that must be re-thrown, not
 * treated as a load failure (Android bug #10 in `PPDT_Pipeline.md`'s known-
 * issues table; re-introducing it here would be a regression, not a fresh
 * bug).
 */
class PPDTSubmissionResultViewModel(
    private val submissionRepository: SubmissionRepository,
    private val logger: DomainLogger
) : ViewModel() {
    private val tag = "PPDTSubmissionResultViewModel"

    private val _uiState = MutableStateFlow(PPDTSubmissionResultUiState())
    val uiState: StateFlow<PPDTSubmissionResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                submissionRepository.observePPDTSubmission(submissionId).collect { submission ->
                    if (submission == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Submission not found") }
                        return@collect
                    }
                    _uiState.update { it.copy(isLoading = false, submission = submission) }

                    if (submission.analysisStatus == AnalysisStatus.COMPLETED) {
                        loadResult(submissionId)
                        currentCoroutineContext().cancel() // terminal state -- stop observing Firestore
                    }
                }
            } catch (e: CancellationException) {
                throw e // CE is a control signal, not a fault
            } catch (e: Exception) {
                logger.e(tag, "Failed to load PPDT submission result", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load submission") }
            }
        }
    }

    /** Loads OLQ result from `ppdt_results` (GTO pattern) once status == COMPLETED. */
    private suspend fun loadResult(submissionId: String) {
        try {
            val result = submissionRepository.getPPDTResult(submissionId)
            if (result.isSuccess) {
                val olqResult = result.getOrNull()
                val ssbRecommendation = olqResult?.olqScores?.takeIf { it.isNotEmpty() }?.let { scores ->
                    val validationResult = ValidationIntegration.validateScores(
                        scores = scores,
                        entryType = EntryType.NDA // TODO: Get from user profile (carried forward from Android original)
                    )
                    SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
                }
                _uiState.update { currentState ->
                    currentState.submission?.let { submission ->
                        currentState.copy(submission = submission.copy(olqResult = olqResult), ssbRecommendation = ssbRecommendation)
                    } ?: currentState
                }
            } else {
                logger.w(tag, "PPDT OLQ result not yet available: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(tag, "Failed to load PPDT OLQ result", e)
        }
    }
}

data class PPDTSubmissionResultUiState(
    override val isLoading: Boolean = true,
    val submission: PPDTSubmission? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : UnifiedResultUiState {
    override val analysisStatus: AnalysisStatus
        get() = submission?.analysisStatus ?: AnalysisStatus.PENDING_ANALYSIS
    override val olqResult: OLQAnalysisResult?
        get() = submission?.olqResult
}
