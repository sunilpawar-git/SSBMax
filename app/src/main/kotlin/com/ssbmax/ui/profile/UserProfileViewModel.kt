package com.ssbmax.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for UserProfileScreen.
 * Manages user profile data, validation, and persistence.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Loads the current user's profile from Firestore
     */
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "User not authenticated"
                    ) 
                }
                return@launch
            }

            userProfileRepository.getUserProfile(currentUser.id)
                .collect { result ->
                    result.fold(
                        onSuccess = { profile ->
                            _uiState.update { 
                                it.copy(
                                    profile = profile,
                                    isLoading = false,
                                    error = null
                                ) 
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message
                                ) 
                            }
                        }
                    )
                }
        }
    }

    /**
     * Updates profile fields in UI state
     */
    fun updateFullName(name: String) {
        _uiState.update { it.copy(fullName = name, error = null) }
    }

    fun updateAge(age: String) {
        val ageInt = age.toIntOrNull()
        _uiState.update { it.copy(age = ageInt, error = null) }
    }

    fun updateGender(gender: Gender) {
        _uiState.update { it.copy(gender = gender, error = null) }
    }

    fun updateEntryType(entryType: EntryType) {
        _uiState.update { it.copy(entryType = entryType, error = null) }
    }

    /**
     * Validates and saves the profile
     */
    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Validation
            val validationError = validateProfile(state)
            if (validationError != null) {
                _uiState.update { it.copy(error = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUser = authRepository.currentUser.first()
            if (currentUser == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "User not authenticated"
                    ) 
                }
                return@launch
            }

            val profile = UserProfile(
                userId = currentUser.id,
                fullName = state.fullName,
                age = state.age!!,
                gender = state.gender!!,
                entryType = state.entryType!!,
                profilePictureUrl = state.profilePictureUrl
            )

            val result = if (state.profile == null) {
                userProfileRepository.saveUserProfile(profile)
            } else {
                userProfileRepository.updateUserProfile(profile)
            }

            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            error = null
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        ) 
                    }
                }
            )
        }
    }

    private fun validateProfile(state: UserProfileUiState): String? {
        return when {
            state.fullName.isBlank() -> "Full name is required"
            state.age == null -> "Age is required"
            state.age !in 18..35 -> "Age must be between 18 and 35"
            state.gender == null -> "Gender is required"
            state.entryType == null -> "Entry type is required"
            else -> null
        }
    }

    /**
     * Resets the saved state after navigation
     */
    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
}

/**
 * UI state for UserProfileScreen
 */
data class UserProfileUiState(
    val profile: UserProfile? = null,
    val fullName: String = "",
    val age: Int? = null,
    val gender: Gender? = null,
    val entryType: EntryType? = null,
    val profilePictureUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

