package com.ssbmax.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.auth.SignOutUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Global app-level ViewModel
 * Manages current authenticated user state and sign out
 */
class AppViewModel(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    /**
     * Current authenticated user
     * Null if user is not signed in
     */
    val currentUser: StateFlow<SSBMaxUser?> = observeCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
        }
    }
}
