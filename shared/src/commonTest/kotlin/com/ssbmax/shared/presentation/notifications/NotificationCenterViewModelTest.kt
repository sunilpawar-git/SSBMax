@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.notifications

import com.ssbmax.shared.domain.model.NotificationType
import com.ssbmax.shared.domain.model.SSBMaxNotification
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeNotificationRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization test for [NotificationCenterViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current load -> group-by-date -> filter -> mark-read/delete state machine.
 */
class NotificationCenterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var notificationRepository: FakeNotificationRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        notificationRepository = FakeNotificationRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun notification(
        id: String,
        type: NotificationType = NotificationType.GENERAL_ANNOUNCEMENT,
        isRead: Boolean = false
    ) = SSBMaxNotification(
        id = id,
        userId = testUser().id,
        type = type,
        title = "Title $id",
        message = "Message $id",
        isRead = isRead,
        createdAt = 0L
    )

    private fun buildViewModel() = NotificationCenterViewModel(
        notificationRepository = notificationRepository,
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository)
    )

    @Test
    fun `loads and groups notifications by date on init`() = runTest(testDispatcher) {
        notificationRepository.notificationsFlow = flowOf(listOf(notification("n1"), notification("n2")))
        notificationRepository.unreadCountFlow = flowOf(2)

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
        assertEquals(1, state.notifications.size) // both created at epoch 0 -> same date bucket
        assertEquals(2, state.notifications.first().notifications.size)
        assertEquals(2, state.unreadCount)
    }

    @Test
    fun `blocks unauthenticated access with a sign-in prompt`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Please sign in to view notifications", state.error)
    }

    @Test
    fun `updateFilter narrows the notification list to unread only`() = runTest(testDispatcher) {
        notificationRepository.notificationsFlow = flowOf(
            listOf(notification("n1", isRead = true), notification("n2", isRead = false))
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateFilter(NotificationFilter.UNREAD)
        testDispatcher.scheduler.advanceUntilIdle()

        val allNotifications = viewModel.uiState.value.notifications.flatMap { it.notifications }
        assertEquals(NotificationFilter.UNREAD, viewModel.selectedFilter.value)
        assertEquals(1, allNotifications.size)
        assertEquals("n2", allNotifications.first().id)
    }

    @Test
    fun `markAsRead delegates to the repository`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.markAsRead("n1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(notificationRepository.markedRead.contains("n1"))
    }

    @Test
    fun `deleteNotification surfaces an error on failure`() = runTest(testDispatcher) {
        notificationRepository.deleteResult = Result.failure(Exception("delete failed"))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteNotification("n1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("delete failed", viewModel.uiState.value.error)
        assertTrue(notificationRepository.deleted.contains("n1"))
    }

    @Test
    fun `clearError resets the error field`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.error != null)

        viewModel.clearError()

        assertEquals(null, viewModel.uiState.value.error)
    }
}
