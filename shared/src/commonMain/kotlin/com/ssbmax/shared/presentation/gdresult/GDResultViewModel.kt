package com.ssbmax.shared.presentation.gdresult

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
 * KMP port of `app/.../ui/tests/gto/gd/GDResultViewModel.kt`. Same shape as
 * [com.ssbmax.shared.presentation.srtresult.SRTSubmissionResultViewModel],
 * but the underlying data comes from [GTORepository.observeSubmission]/
 * [GTORepository.getTestResult] (GTO's own pre-existing Phase 2 repository
 * shape) rather than `SubmissionRepository`'s psych-test-style embedded
 * `olqResult` -- mapped to the shared [UnifiedResultUiState] contract via
 * [toAnalysisStatus]/[toOLQAnalysisResult] so [GDResultScreen] can reuse
 * [com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate] like
 * every other test-type result screen.
 */
class GDResultViewModel(
    private val gtoRepository: GTORepository,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "GDResultViewModel"

    private val _uiState = MutableStateFlow(GDResultUiState())
    val uiState: StateFlow<GDResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                gtoRepository.observeSubmission(submissionId).collect { submission ->
                    val gdSubmission = submission as? GTOSubmission.GDSubmission
                    if (submission == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Submission not found") }
                        return@collect
                    }
                    _uiState.update { it.copy(isLoading = false, submission = gdSubmission) }

                    if (submission.status == GTOSubmissionStatus.COMPLETED) {
                        loadResult(submissionId)
                        currentCoroutineContext().cancel() // terminal state -- stop observing Firestore
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Failed to load GD submission", e)
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
                .onFailure { logger.w(tag, "GD result not yet available: ${it.message}") }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(tag, "Failed to load GD result", e)
        }
    }

    fun retry(submissionId: String) = loadSubmission(submissionId)

    fun close() {
        scope.cancel()
    }
}

data class GDResultUiState(
    override val isLoading: Boolean = true,
    val submission: GTOSubmission.GDSubmission? = null,
    val result: GTOResult? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : UnifiedResultUiState {
    override val analysisStatus: AnalysisStatus
        get() = submission?.status?.toAnalysisStatus() ?: AnalysisStatus.PENDING_ANALYSIS
    override val olqResult: OLQAnalysisResult?
        get() = result?.toOLQAnalysisResult(TestType.GTO_GD)

    val formattedTimeSpent: String
        get() {
            val timeSpent = submission?.timeSpent ?: 0
            return "${timeSpent / 60}m ${timeSpent % 60}s"
        }
}
