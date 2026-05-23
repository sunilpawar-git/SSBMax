package com.ssbmax.ui.instructor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.Batch
import com.ssbmax.core.domain.repository.BatchRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CreateBatchViewModel @Inject constructor(
    private val batchRepository: BatchRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBatchUiState())
    val uiState: StateFlow<CreateBatchUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onMaxStudentsChanged(maxStudents: String) {
        _uiState.update { it.copy(maxStudents = maxStudents) }
    }

    fun createBatch() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(error = "Batch name cannot be empty") }
            return
        }

        val maxStudentsLimit = currentState.maxStudents.toIntOrNull() ?: 50
        if (maxStudentsLimit <= 0) {
            _uiState.update { it.copy(error = "Max students must be greater than 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUser = observeCurrentUser().first()
            val instructorId = currentUser?.id ?: run {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "User not logged in or not an instructor"
                ) }
                return@launch
            }

            val batchId = UUID.randomUUID().toString()
            val inviteCode = generateInviteCode()

            val batch = Batch(
                id = batchId,
                name = currentState.name.trim(),
                description = currentState.description.trim().ifEmpty { null },
                instructorId = instructorId,
                inviteCode = inviteCode,
                maxStudents = maxStudentsLimit
            )

            val result = batchRepository.createBatch(batch)
            if (result.isSuccess) {
                _uiState.update { it.copy(
                    isLoading = false,
                    isSuccess = true,
                    createdBatchId = batchId
                ) }
            } else {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to create batch"
                ) }
            }
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}

data class CreateBatchUiState(
    val name: String = "",
    val description: String = "",
    val maxStudents: String = "50",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val createdBatchId: String? = null
)
