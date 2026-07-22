package com.ssbmax.shared.presentation.srtresult

import com.ssbmax.shared.domain.model.SRTSubmission
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
 * KMP port of `app/.../ui/tests/srt/SRTSubmissionResultViewModel.kt`.
 *
 * Same simplification as [com.ssbmax.shared.presentation.watresult.WATSubmissionResultViewModel]'s
 * doc comment describes: the Android original hand-parses a raw Firestore
 * `Map<String, Any>` snapshot (~120 lines of `as?` casts plus a "prevent
 * regression from COMPLETED+OLQ to incomplete state" guard against
 * conflicting snapshots); this port uses
 * [SubmissionRepository.observeSRTSubmission]/[SubmissionRepository.getSRTResult],
 * which already return typed [SRTSubmission]/[OLQAnalysisResult] domain
 * models. Observe in real time; once `analysisStatus == COMPLETED`, fetch the
 * full result separately and stop observing.
 *
 * `CancellationException` contract preserved verbatim from TAT/WAT's port: a
 * navigate-away cancels [scope], which surfaces as `CancellationException`
 * inside `collect` -- must be re-thrown, not treated as a load failure.
 */
class SRTSubmissionResultViewModel(
    private val submissionRepository: SubmissionRepository,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "SRTSubmissionResultViewModel"

    private val _uiState = MutableStateFlow(SRTSubmissionResultUiState())
    val uiState: StateFlow<SRTSubmissionResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                submissionRepository.observeSRTSubmission(submissionId).collect { submission ->
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
                logger.e(tag, "Failed to load SRT submission result", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load submission") }
            }
        }
    }

    /** Loads OLQ result from `psych_results` (GTO pattern) once status == COMPLETED. */
    private suspend fun loadResult(submissionId: String) {
        try {
            val result = submissionRepository.getSRTResult(submissionId)
            if (result.isSuccess) {
                val olqResult = result.getOrNull()
                val ssbRecommendation = olqResult?.olqScores?.takeIf { it.isNotEmpty() }?.let { scores ->
                    val validationResult = ValidationIntegration.validateScores(scores = scores, entryType = EntryType.NDA)
                    SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
                }
                _uiState.update { currentState ->
                    currentState.submission?.let { submission ->
                        currentState.copy(submission = submission.copy(olqResult = olqResult), ssbRecommendation = ssbRecommendation)
                    } ?: currentState
                }
            } else {
                logger.w(tag, "SRT OLQ result not yet available: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(tag, "Failed to load SRT OLQ result", e)
        }
    }

    fun close() {
        scope.cancel()
    }
}

data class SRTSubmissionResultUiState(
    override val isLoading: Boolean = true,
    val submission: SRTSubmission? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : UnifiedResultUiState {
    override val analysisStatus: AnalysisStatus
        get() = submission?.analysisStatus ?: AnalysisStatus.PENDING_ANALYSIS
    override val olqResult: OLQAnalysisResult?
        get() = submission?.olqResult
}
