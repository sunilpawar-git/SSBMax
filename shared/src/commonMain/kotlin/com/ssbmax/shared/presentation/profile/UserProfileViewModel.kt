package com.ssbmax.shared.presentation.profile

import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/profile/UserProfileViewModel.kt`.
 *
 * Phase 1 of the KMP-convergence plan: a real `androidx.lifecycle.ViewModel`
 * using `viewModelScope`, converged with `app`'s existing ViewModel idiom and
 * this module's own DI (`viewModelOf`) / screen (`koinViewModel()`)
 * conventions — no more manual `CoroutineScope` + `close()`.
 *
 * Deviation from the Android original: `android.util.Log` calls replaced
 * with [DomainLogger], same seam every other ported ViewModel in this phase
 * uses. Behavior (auth-state-reactive profile loading, validation, save)
 * otherwise unchanged.
 *
 * Also fetches [SubscriptionTier] via [GetSubscriptionTierUseCase] (the
 * `SubscriptionRepository`-backed SSOT) for the drawer's tier badge --
 * `UserProfile.subscriptionType`/`UserProfileDto.subscriptionType` used to
 * feed that badge but was only ever written as a hardcoded FREE default at
 * profile creation, never updated, so the badge showed "Free" forever
 * regardless of the real tier. Same root cause as the "Current Plan" bug
 * [com.ssbmax.shared.presentation.settings.SettingsViewModel] fixed.
 */
class UserProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val getSubscriptionTier: GetSubscriptionTierUseCase,
    private val logger: DomainLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "UserProfileViewModel"
    }

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { currentUser ->
                if (currentUser != null) {
                    loadProfileForUser(currentUser.id)
                } else {
                    _uiState.update {
                        it.copy(
                            profile = null,
                            subscriptionTier = null,
                            isLoading = false,
                            error = "Please sign in to view your profile"
                        )
                    }
                }
            }
        }
    }

    private fun loadProfileForUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getSubscriptionTier(userId)
                .onSuccess { tier -> _uiState.update { it.copy(subscriptionTier = tier) } }
                .onFailure { error -> logger.e(TAG, "Failed to load subscription tier for user: $userId", error) }

            userProfileRepository.getUserProfile(userId)
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
                            logger.e(TAG, "Failed to load user profile for user: $userId", error)
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

    fun loadProfile() {
        val currentUser = authRepository.currentUser.value
        if (currentUser != null) {
            loadProfileForUser(currentUser.id)
        } else {
            logger.w(TAG, "loadProfile() called but no current user")
        }
    }

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

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value

            val validationError = validateProfile(state)
            if (validationError != null) {
                _uiState.update { it.copy(error = validationError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUser = authRepository.currentUser.value
            if (currentUser == null) {
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

            val result = if (state.profile == null) {
                userProfileRepository.saveUserProfile(profile)
            } else {
                userProfileRepository.updateUserProfile(profile)
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            isSaved = true,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(TAG, "Failed to save profile: ${error.message}", error)
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

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
}

/**
 * UI state for UserProfileScreen
 */
data class UserProfileUiState(
    val profile: UserProfile? = null,
    val subscriptionTier: SubscriptionTier? = null,
    val fullName: String = "",
    val age: Int? = null,
    val gender: Gender? = null,
    val entryType: EntryType? = null,
    val profilePictureUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)
