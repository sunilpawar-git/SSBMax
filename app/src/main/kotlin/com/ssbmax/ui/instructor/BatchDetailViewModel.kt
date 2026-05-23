package com.ssbmax.ui.instructor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.Batch
import com.ssbmax.core.domain.model.StudentPerformance
import com.ssbmax.core.domain.repository.BatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatchDetailViewModel @Inject constructor(
    private val batchRepository: BatchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val batchId: String = savedStateHandle["batchId"] ?: ""

    private val _uiState = MutableStateFlow(BatchDetailUiState())
    val uiState: StateFlow<BatchDetailUiState> = _uiState.asStateFlow()

    init {
        loadBatchDetails()
    }

    private fun loadBatchDetails() {
        if (batchId.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid batch ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Observe batch details
            launch {
                batchRepository.getBatch(batchId).collect { result ->
                    result.onSuccess { batch ->
                        _uiState.update { it.copy(batch = batch, isLoading = false) }
                    }.onFailure { exception ->
                        _uiState.update { it.copy(error = exception.message ?: "Failed to load batch info", isLoading = false) }
                    }
                }
            }

            // Observe enrolled students
            launch {
                batchRepository.getStudentsInBatch(batchId).collect { result ->
                    result.onSuccess { students ->
                        _uiState.update { it.copy(students = students) }
                    }
                }
            }
        }
    }
}

data class BatchDetailUiState(
    val isLoading: Boolean = true,
    val batch: Batch? = null,
    val students: List<StudentPerformance> = emptyList(),
    val error: String? = null
)
