package com.ssbmax.shared.presentation.tatresult

import com.ssbmax.shared.domain.model.TATSubmission
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.scoring.EntryType
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.validation.SSBRecommendationUIModel
import com.ssbmax.shared.domain.validation.ValidationIntegration
import com.ssbmax.shared.ui.components.result.UnifiedResultUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of `app/.../ui/tests/tat/TATSubmissionResultViewModel.kt`.
 *
 * Unlike the Android original (which parses a raw Firestore `Map<String,
 * Any>` snapshot by hand -- `parseTATSubmission`/`parseOLQResult`, ~120 lines
 * of manual `as?` casts), this port uses [SubmissionRepository.observeTATSubmission]
 * / [SubmissionRepository.getTATResult], which already return typed
 * [TATSubmission]/[OLQAnalysisResult] domain models (the GitLive repository
 * impl does the Firestore decode internally -- see `GitLiveSubmissionRepository`,
 * Phase 2). This is the same simplification
 * [com.ssbmax.shared.presentation.ppdtresult.PPDTSubmissionResultViewModel]'s
 * doc comment documents for PPDT ("the GTO pattern"): observe in real time,
 * and once `analysisStatus == COMPLETED`, fetch the full result separately
 * and stop observing.
 *
 * `usesPartialAssessment` (`docs/architecture/TAT_Pipeline.md` §10a) is
 * preserved as a derived property on the UiState, exactly mirroring the
 * Android original's `TATSubmissionResultUiState.usesPartialAssessment` --
 * this is the one field genuinely specific to TAT among the OLQ-based test
 * types (PPDT/WAT/SRT/SDT don't batch multiple stories, so partial-assessment
 * degradation doesn't apply to them the same way).
 *
 * `CancellationException` contract preserved verbatim from PPDT's port: a
 * navigate-away cancels [scope], which surfaces as `CancellationException`
 * inside `collect` -- must be re-thrown, not treated as a load failure.
 */
class TATSubmissionResultViewModel(
    private val submissionRepository: SubmissionRepository,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "TATSubmissionResultViewModel"

    private val _uiState = MutableStateFlow(TATSubmissionResultUiState())
    val uiState: StateFlow<TATSubmissionResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                submissionRepository.observeTATSubmission(submissionId).collect { submission ->
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
                logger.e(tag, "Failed to load TAT submission result", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load submission") }
            }
        }
    }

    /** Loads OLQ result from `psych_results` (GTO pattern) once status == COMPLETED. */
    private suspend fun loadResult(submissionId: String) {
        try {
            val result = submissionRepository.getTATResult(submissionId)
            if (result.isSuccess) {
                val olqResult = result.getOrNull()
                val ssbRecommendation = olqResult?.olqScores?.takeIf { it.isNotEmpty() }?.let { scores ->
                    val validationResult = ValidationIntegration.validateScores(
                        scores = scores,
                        entryType = EntryType.NDA
                    )
                    SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
                }
                _uiState.update { currentState ->
                    currentState.submission?.let { submission ->
                        currentState.copy(submission = submission.copy(olqResult = olqResult), ssbRecommendation = ssbRecommendation)
                    } ?: currentState
                }
            } else {
                logger.w(tag, "TAT OLQ result not yet available: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(tag, "Failed to load TAT OLQ result", e)
        }
    }

    fun close() {
        scope.cancel()
    }
}

data class TATSubmissionResultUiState(
    override val isLoading: Boolean = true,
    val submission: TATSubmission? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : UnifiedResultUiState {
    override val analysisStatus: AnalysisStatus
        get() = submission?.analysisStatus ?: AnalysisStatus.PENDING_ANALYSIS
    override val olqResult: OLQAnalysisResult?
        get() = submission?.olqResult
    val usesPartialAssessment: Boolean
        get() = submission?.olqResult?.usedPartialAssessment == true
}
