package com.ssbmax.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.auth.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global app-level ViewModel
 * Manages current authenticated user state and sign out
 */
@HiltViewModel
class AppViewModel @Inject constructor(
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
