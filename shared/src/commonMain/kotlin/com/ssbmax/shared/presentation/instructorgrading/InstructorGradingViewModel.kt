package com.ssbmax.shared.presentation.instructorgrading

import com.ssbmax.shared.domain.model.GradingQueueItem
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.GradingQueueRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/instructor/InstructorGradingViewModel.kt`
 * (Grading Queue dashboard).
 *
 * A real `androidx.lifecycle.ViewModel` using `viewModelScope`; the screen
 * uses `koinViewModel()`.
 *
 * Deviation from the Android original: `ObserveCurrentUserUseCase` (an extra
 * layer) replaced with [AuthRepository.currentUser]`.value` directly, matching
 * this phase's established precedent for every other ported ViewModel that
 * needed "who is the current user" (e.g. `TestDetailGradingViewModel`,
 * `SubmissionsListViewModel`). Everything else (queue loading, filter-by-type,
 * pending/under-review counts) is an unchanged port.
 */
class InstructorGradingViewModel(
    private val gradingQueueRepository: GradingQueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(InstructorGradingUiState())
    val uiState: StateFlow<InstructorGradingUiState> = _uiState.asStateFlow()

    init {
        loadPendingSubmissions()
    }

    fun loadPendingSubmissions(filterType: TestType? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val instructorId = authRepository.currentUser.value?.id ?: run {
                _uiState.update {
                    it.copy(isLoading = false, error = "Please login to view grading queue")
                }
                return@launch
            }

            val flow = if (filterType != null) {
                gradingQueueRepository.observeSubmissionsByTestType(filterType)
            } else {
                gradingQueueRepository.observePendingSubmissions(instructorId)
            }

            flow
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load submissions: ${error.message}")
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
                .onEach { submissions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            submissions = submissions,
                            filteredType = filterType,
                            error = null
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun filterByType(type: TestType?) {
        _uiState.update { it.copy(filteredType = type) }
        loadPendingSubmissions(type)
    }

    fun refresh() {
        loadPendingSubmissions(_uiState.value.filteredType)
    }
}

/**
 * UI State for Instructor Grading Dashboard
 */
data class InstructorGradingUiState(
    val isLoading: Boolean = true,
    val submissions: List<GradingQueueItem> = emptyList(),
    val filteredType: TestType? = null,
    val error: String? = null
) {
    val pendingCount: Int
        get() = submissions.count { it.status == SubmissionStatus.SUBMITTED_PENDING_REVIEW }

    val underReviewCount: Int
        get() = submissions.count { it.status == SubmissionStatus.UNDER_REVIEW }

    val groupedByTestType: Map<TestType, List<GradingQueueItem>>
        get() = submissions.groupBy { it.testType }
}
