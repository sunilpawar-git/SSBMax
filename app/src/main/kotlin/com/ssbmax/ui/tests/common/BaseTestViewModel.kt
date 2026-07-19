package com.ssbmax.ui.tests.common

import androidx.lifecycle.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

abstract class BaseTestViewModel(
    protected val observeCurrentUser: ObserveCurrentUserUseCase,
    protected val subscriptionManager: SubscriptionManager,
    protected val securityLogger: SecurityEventLogger,
    protected val workManager: WorkManager
) : ViewModel() {

    private val _navigationEvents = Channel<TestNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // Generation token for timer race condition prevention (B1 fix).
    // Subclasses increment before each timer launch; store in UiState.timerStartTime.
    protected var timerGeneration = 0L

    protected fun sendNavigationEvent(event: TestNavigationEvent) {
        _navigationEvents.trySend(event)
    }

    protected fun enqueueAnalysisWork(uniqueTag: String, workRequest: OneTimeWorkRequest) {
        workManager.enqueueUniqueWork(uniqueTag, ExistingWorkPolicy.KEEP, workRequest)
    }

    override fun onCleared() {
        super.onCleared()
        _navigationEvents.close()
    }
}
