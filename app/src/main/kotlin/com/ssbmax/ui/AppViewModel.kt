package com.ssbmax.ui

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.FCMToken
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.auth.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global app-level ViewModel.
 * Manages current authenticated user state, sign out, and FCM token sync.
 *
 * FCM Token Sync Strategy:
 * Observes [currentUser] and synchronizes the device's FCM registration token
 * to Firestore whenever a fresh login is detected. This resolves the first-install
 * race condition where [onNewToken] fires before the user is authenticated.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Current authenticated user. Null if user is not signed in.
     */
    val currentUser: StateFlow<SSBMaxUser?> = observeCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        observeLoginAndSyncFcmToken()
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
        }
    }

    /**
     * Watches for new login events and syncs the FCM token to Firestore.
     * Uses [distinctUntilChanged] on the user ID so re-subscribing composables
     * don't trigger redundant uploads.
     */
    private fun observeLoginAndSyncFcmToken() {
        viewModelScope.launch {
            currentUser
                .map { it?.id }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { userId -> syncFcmTokenForUser(userId) }
        }
    }

    private suspend fun syncFcmTokenForUser(userId: String) {
        val tokenResult = notificationRepository.getCurrentFCMToken()
        tokenResult.onSuccess { rawToken ->
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            val fcmToken = FCMToken(
                userId = userId,
                token = rawToken,
                deviceId = deviceId,
                platform = "android",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            notificationRepository.saveFCMToken(fcmToken)
        }
    }
}
