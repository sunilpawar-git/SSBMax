package com.ssbmax.ui.tests.oir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.OIRSetProgress
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OIRSetListUiState(
    val sets: List<OIRSetProgress> = emptyList(),
    val nextSetNumber: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class OIRSetListViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OIRSetListUiState())
    val uiState: StateFlow<OIRSetListUiState> = _uiState.asStateFlow()

    init {
        loadSets()
    }

    fun loadSets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val user = observeCurrentUser().first()
            val userId = user?.id ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Authentication required.") }
                return@launch
            }

            try {
                val progressResult = testContentRepository.getOIRSetProgressList(userId)
                val nextSetResult = testContentRepository.getNextOIRSetNumber(userId)

                if (progressResult.isFailure) {
                    throw progressResult.exceptionOrNull() ?: Exception("Failed to load set progress")
                }

                val sets = progressResult.getOrNull() ?: emptyList()
                val nextSet = nextSetResult.getOrNull() ?: 1

                _uiState.update { it.copy(
                    sets = sets,
                    nextSetNumber = nextSet,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load OIR set list")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load sets. Please retry.") }
            }
        }
    }
}
