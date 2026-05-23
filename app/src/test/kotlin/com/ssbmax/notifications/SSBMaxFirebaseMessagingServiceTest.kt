package com.ssbmax.notifications

import com.google.firebase.messaging.RemoteMessage
import com.ssbmax.core.domain.model.NotificationType
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SSBMaxFirebaseMessagingServiceTest : BaseViewModelTest() {

    private lateinit var service: SSBMaxFirebaseMessagingService
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase

    @Before
    fun setup() {
        notificationRepository = mockk(relaxed = true)
        observeCurrentUser = mockk(relaxed = true)
        mockkObject(FCMNotificationHelper)
        every { FCMNotificationHelper.showNotification(any(), any(), any(), any(), any(), any()) } just runs

        service = spyk(SSBMaxFirebaseMessagingService()).apply {
            this.notificationRepository = this@SSBMaxFirebaseMessagingServiceTest.notificationRepository
            this.observeCurrentUser = this@SSBMaxFirebaseMessagingServiceTest.observeCurrentUser
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onNewToken saves token to repository when user logged in`() = runTest {
        val testUser = SSBMaxUser("user_123", "user@test.com", "Rahul Kumar", role = UserRole.STUDENT)
        coEvery { observeCurrentUser() } returns flowOf(testUser)

        service.onNewToken("mock_fcm_token_123")
        advanceUntilIdle()

        coVerify(exactly = 1) { notificationRepository.saveFCMToken(match {
            it.token == "mock_fcm_token_123" && it.userId == "user_123"
        }) }
    }

    @Test
    fun `onMessageReceived parses data payload and displays push notification`() = runTest {
        val testUser = SSBMaxUser("user_123", "user@test.com", "Rahul Kumar", role = UserRole.STUDENT)
        coEvery { observeCurrentUser() } returns flowOf(testUser)

        val data = mapOf(
            "title" to "Test Announcement",
            "message" to "This is a test notification",
            "type" to "GENERAL_ANNOUNCEMENT",
            "actionUrl" to "ssbmax://general",
            "notificationId" to "notif_999"
        )
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true) {
            every { this@mockk.data } returns data
            every { notification } returns null
        }

        service.onMessageReceived(mockRemoteMessage)
        advanceUntilIdle()

        coVerify(exactly = 1) { notificationRepository.saveNotification(match {
            it.id == "notif_999" && it.title == "Test Announcement" && it.type == NotificationType.GENERAL_ANNOUNCEMENT
        }) }

        verify(exactly = 1) { FCMNotificationHelper.showNotification(
            context = any(),
            type = NotificationType.GENERAL_ANNOUNCEMENT,
            title = "Test Announcement",
            message = "This is a test notification",
            actionUrl = "ssbmax://general",
            notificationId = "notif_999"
        ) }
    }
}
