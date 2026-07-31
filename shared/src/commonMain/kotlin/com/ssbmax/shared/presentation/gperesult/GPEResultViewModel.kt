package com.ssbmax.shared.presentation.gperesult

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.gto.GTOResult
import com.ssbmax.shared.domain.model.gto.GTOSubmission
import com.ssbmax.shared.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.scoring.EntryType
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.validation.SSBRecommendationUIModel
import com.ssbmax.shared.domain.validation.ValidationIntegration
import com.ssbmax.shared.presentation.gto.common.toAnalysisStatus
import com.ssbmax.shared.presentation.gto.common.toOLQAnalysisResult
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
 * KMP port of `app/.../ui/tests/gpe/GPESubmissionResultViewModel.kt`. Same
 * shape as [com.ssbmax.shared.presentation.lecturetteresult.LecturetteResultViewModel]
 * (see that file's doc comment for the GTO-result-shape-to-[UnifiedResultUiState]
 * mapping rationale) -- GPE's result flow is structurally identical to
 * GD's/Lecturette's, differing only in the submission subtype observed.
 */
class GPEResultViewModel(
    private val gtoRepository: GTORepository,
    private val logger: DomainLogger
) : ViewModel() {
    private val tag = "GPEResultViewModel"

    private val _uiState = MutableStateFlow(GPEResultUiState())
    val uiState: StateFlow<GPEResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                gtoRepository.observeSubmission(submissionId).collect { submission ->
                    if (submission == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Submission not found") }
                        return@collect
                    }
                    val gpeSubmission = submission as? GTOSubmission.GPESubmission
                    _uiState.update { it.copy(isLoading = false, submission = gpeSubmission) }

                    if (submission.status == GTOSubmissionStatus.COMPLETED) {
                        loadResult(submissionId)
                        currentCoroutineContext().cancel()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Failed to load GPE submission", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load submission. Please try again.") }
            }
        }
    }

    private suspend fun loadResult(submissionId: String) {
        try {
            gtoRepository.getTestResult(submissionId)
                .onSuccess { result ->
                    val ssbRecommendation = result.olqScores.takeIf { it.isNotEmpty() }?.let { scores ->
                        val validationResult = ValidationIntegration.validateScores(scores = scores, entryType = EntryType.NDA)
                        SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
                    }
                    _uiState.update { it.copy(result = result, ssbRecommendation = ssbRecommendation) }
                }
                .onFailure { logger.w(tag, "GPE result not yet available: ${it.message}") }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(tag, "Failed to load GPE result", e)
        }
    }

    fun retry(submissionId: String) = loadSubmission(submissionId)
}

data class GPEResultUiState(
    override val isLoading: Boolean = true,
    val submission: GTOSubmission.GPESubmission? = null,
    val result: GTOResult? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : UnifiedResultUiState {
    override val analysisStatus: AnalysisStatus
        get() = submission?.status?.toAnalysisStatus() ?: AnalysisStatus.PENDING_ANALYSIS
    override val olqResult: OLQAnalysisResult?
        get() = result?.toOLQAnalysisResult(TestType.GTO_GPE)

    val formattedTimeSpent: String
        get() {
            val timeSpent = submission?.timeSpent ?: 0
            return "${timeSpent / 60}m ${timeSpent % 60}s"
        }
}
