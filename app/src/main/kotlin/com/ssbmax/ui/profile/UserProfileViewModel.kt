package com.ssbmax.ui.profile

import android.util.Log
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

    companion object {
        private const val TAG = "UserProfileViewModel"
    }

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🔄 UserProfileViewModel initialized")
        // Start observing auth state changes reactively
        observeAuthState()
    }

    /**
     * Observe authentication state changes and load profile when user is available
     */
    private fun observeAuthState() {
        Log.d(TAG, "👀 Starting auth state observation")
        viewModelScope.launch {
            authRepository.currentUser.collect { currentUser ->
                Log.d(TAG, "🔄 Auth state changed - user: ${currentUser?.id}, email: ${currentUser?.email}")
                if (currentUser != null) {
                    loadProfileForUser(currentUser.id)
                } else {
                    Log.d(TAG, "🚪 User signed out - clearing profile")
                    _uiState.update {
                        it.copy(
                            profile = null,
                            isLoading = false,
                            error = "Please sign in to view your profile"
                        )
                    }
                }
            }
        }
    }

    /**
     * Loads the current user's profile from Firestore for a specific user
     */
    private fun loadProfileForUser(userId: String) {
        Log.d(TAG, "📥 loadProfileForUser() called for user: $userId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            Log.d(TAG, "🔍 Loading profile for user: $userId")

            // Collect profile updates reactively
            userProfileRepository.getUserProfile(userId)
                .collect { result ->
                    result.fold(
                        onSuccess = { profile ->
                            Log.d(TAG, "✅ Profile loaded successfully: fullName=${profile?.fullName}, age=${profile?.age}")
                            _uiState.update {
                                it.copy(
                                    profile = profile,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "❌ Failed to load profile: ${error.message}", error)
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
     * Public method to manually refresh profile (for backwards compatibility)
     */
    fun loadProfile() {
        val currentUser = authRepository.currentUser.value
        if (currentUser != null) {
            loadProfileForUser(currentUser.id)
        } else {
            Log.w(TAG, "❌ loadProfile() called but no current user")
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
        Log.d(TAG, "💾 saveProfile() called")
        viewModelScope.launch {
            val state = _uiState.value
            Log.d(TAG, "📝 Current state: fullName=${state.fullName}, age=${state.age}, gender=${state.gender}, entryType=${state.entryType}")

            // Validation
            val validationError = validateProfile(state)
            if (validationError != null) {
                Log.w(TAG, "❌ Validation failed: $validationError")
                _uiState.update { it.copy(error = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUser = authRepository.currentUser.value
            Log.d(TAG, "👤 Current user for save: ${currentUser?.id}")
            if (currentUser == null) {
                Log.w(TAG, "❌ No current user found for save operation")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Please sign in to save your profile"
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
            Log.d(TAG, "📋 Created profile: $profile")

            val result = if (state.profile == null) {
                Log.d(TAG, "🆕 Saving new profile")
                userProfileRepository.saveUserProfile(profile)
            } else {
                Log.d(TAG, "🔄 Updating existing profile")
                userProfileRepository.updateUserProfile(profile)
            }

            result.fold(
                onSuccess = {
                    Log.d(TAG, "✅ Profile saved successfully")
                    _uiState.update {
                        it.copy(
                            profile = profile,  // Update the profile field with saved data
                            isLoading = false,
                            isSaved = true,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to save profile: ${error.message}", error)
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

