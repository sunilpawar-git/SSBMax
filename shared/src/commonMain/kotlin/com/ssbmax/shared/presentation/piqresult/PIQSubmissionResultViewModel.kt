package com.ssbmax.shared.presentation.piqresult

import com.ssbmax.shared.domain.model.PIQSubmission
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of `app/.../ui/tests/piq/PIQSubmissionResultViewModel.kt`. Plain-class +
 * own-`CoroutineScope` pattern, same shape as every other ported result ViewModel
 * this phase (e.g. [com.ssbmax.shared.presentation.sdtresult.SDTSubmissionResultViewModel]).
 *
 * Fetches by `submissionId` via [SubmissionRepository.getSubmission] (ID-based
 * navigation, per this project's mandatory lint rule 7) and hand-parses the raw
 * map via [parsePIQSubmission] -- see that file's doc comment for why this
 * doesn't delegate to the shared data layer's `PIQDataDto.toDomain()` mapper.
 */
class PIQSubmissionResultViewModel(
    private val submissionRepository: SubmissionRepository,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tag = "PIQSubmissionResultViewModel"

    private val _uiState = MutableStateFlow(PIQSubmissionResultUiState())
    val uiState: StateFlow<PIQSubmissionResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        logger.e(tag, "PIQ submission not found: $submissionId", null)
                        _uiState.update { it.copy(isLoading = false, submission = null, error = "Submission not found") }
                        return@onSuccess
                    }

                    val submission = try {
                        parsePIQSubmission(data)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.e(tag, "PIQ submission parsing failed: $submissionId", e)
                        null
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false, submission = submission,
                            error = if (submission == null) "Submission not found" else null
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    logger.e(tag, "Failed to load PIQ submission result", error)
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load submission") }
                }
        }
    }

    fun close() {
        scope.cancel()
    }
}

data class PIQSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: PIQSubmission? = null,
    val error: String? = null
)
