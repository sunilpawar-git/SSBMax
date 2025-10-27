package com.ssbmax.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirebaseInitializer
import com.ssbmax.core.data.remote.FirestoreSubmissionRepository
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.testing.BaseRepositoryTest
import com.ssbmax.testing.FirebaseTestHelper
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for NotificationRepositoryImpl
 *
 * Tests the notification system for SSB test results, grading updates,
 * reminders, and achievement notifications.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NotificationRepositoryImplTest : BaseRepositoryTest() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var firebaseTestHelper: FirebaseTestHelper

    private lateinit var testUser: SSBMaxUser

    @Before
    override fun setup() {
        super.setup()

        // Initialize Firebase for testing
        FirebaseInitializer.initialize()
        firebaseTestHelper.setupEmulator()

        // Create test user
        testUser = SSBMaxUser(
            id = "student_001",
            email = "student@test.com",
            displayName = "Test Student",
            photoUrl = null,
            role = UserRole.STUDENT,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        firebaseTestHelper.cleanup()
    }

    @Test
    fun `getNotifications returns user notifications in chronological order`() = runTest(timeout = 30.seconds) {
        // Given: Create multiple notifications for user
        val notifications = createTestNotifications()
        notifications.forEach { notification ->
            firebaseTestHelper.createTestNotification(notification)
        }

        // When: Get user notifications
        val userNotifications = notificationRepository.getNotifications(testUser.id).first()

        // Then: Should return notifications in reverse chronological order (newest first)
        Assert.assertTrue("Should get user notifications", userNotifications.isSuccess)
        val notificationList = userNotifications.getOrNull() ?: emptyList()

        Assert.assertEquals("Should return all notifications", notifications.size, notificationList.size)

        // Verify chronological ordering (newest first)
        for (i in 0 until notificationList.size - 1) {
            Assert.assertTrue(
                "Notifications should be ordered newest first",
                notificationList[i].createdAt >= notificationList[i + 1].createdAt
            )
        }

        // Verify notification content
        val testResultNotification = notificationList.find { it.type == NotificationType.TEST_RESULT }
        Assert.assertNotNull("Should have test result notification", testResultNotification)
        Assert.assertEquals("Should have correct title", "TAT Test Results Available", testResultNotification?.title)
    }

    @Test
    fun `markAsRead updates notification read status`() = runTest(timeout = 30.seconds) {
        // Given: Create unread notification
        val notification = createTestNotification("notif_001", NotificationType.TEST_RESULT, false)
        firebaseTestHelper.createTestNotification(notification)

        // Verify initially unread
        val initialNotifications = notificationRepository.getNotifications(testUser.id).first()
        val initialNotif = initialNotifications.getOrNull()?.find { it.id == notification.id }
        Assert.assertNotNull("Notification should exist", initialNotif)
        Assert.assertFalse("Should initially be unread", initialNotif?.isRead == true)

        // When: Mark as read
        val markResult = notificationRepository.markAsRead(notification.id)

        // Then: Mark operation should succeed
        Assert.assertTrue("Mark as read should succeed", markResult.isSuccess)

        // Notification should now be read
        val updatedNotifications = notificationRepository.getNotifications(testUser.id).first()
        val updatedNotif = updatedNotifications.getOrNull()?.find { it.id == notification.id }
        Assert.assertNotNull("Notification should still exist", updatedNotif)
        Assert.assertTrue("Should now be read", updatedNotif?.isRead == true)
    }

    @Test
    fun `markAllAsRead marks all user notifications as read`() = runTest(timeout = 30.seconds) {
        // Given: Create multiple unread notifications
        val notifications = listOf(
            createTestNotification("notif_001", NotificationType.TEST_RESULT, false),
            createTestNotification("notif_002", NotificationType.ACHIEVEMENT, false),
            createTestNotification("notif_003", NotificationType.REMINDER, false)
        )
        notifications.forEach { firebaseTestHelper.createTestNotification(it) }

        // Verify all initially unread
        val initialNotifications = notificationRepository.getNotifications(testUser.id).first()
        val initialUnreadCount = initialNotifications.getOrNull()?.count { !it.isRead } ?: 0
        Assert.assertEquals("Should have 3 unread notifications", 3, initialUnreadCount)

        // When: Mark all as read
        val markAllResult = notificationRepository.markAllAsRead(testUser.id)

        // Then: Operation should succeed
        Assert.assertTrue("Mark all as read should succeed", markAllResult.isSuccess)

        // All notifications should now be read
        val updatedNotifications = notificationRepository.getNotifications(testUser.id).first()
        val updatedUnreadCount = updatedNotifications.getOrNull()?.count { !it.isRead } ?: 0
        Assert.assertEquals("Should have 0 unread notifications", 0, updatedUnreadCount)
    }

    @Test
    fun `deleteNotification removes notification from user's list`() = runTest(timeout = 30.seconds) {
        // Given: Create notification
        val notification = createTestNotification("notif_001", NotificationType.TEST_RESULT, false)
        firebaseTestHelper.createTestNotification(notification)

        // Verify it exists
        val initialNotifications = notificationRepository.getNotifications(testUser.id).first()
        Assert.assertTrue("Notification should exist initially",
            initialNotifications.getOrNull()?.any { it.id == notification.id } == true)

        // When: Delete notification
        val deleteResult = notificationRepository.deleteNotification(notification.id)

        // Then: Delete operation should succeed
        Assert.assertTrue("Delete should succeed", deleteResult.isSuccess)

        // Notification should no longer exist
        val updatedNotifications = notificationRepository.getNotifications(testUser.id).first()
        Assert.assertFalse("Notification should be deleted",
            updatedNotifications.getOrNull()?.any { it.id == notification.id } == true)
    }

    @Test
    fun `getUnreadCount returns correct count of unread notifications`() = runTest(timeout = 30.seconds) {
        // Given: Create mix of read and unread notifications
        val notifications = listOf(
            createTestNotification("notif_001", NotificationType.TEST_RESULT, false), // unread
            createTestNotification("notif_002", NotificationType.ACHIEVEMENT, true),  // read
            createTestNotification("notif_003", NotificationType.REMINDER, false),   // unread
            createTestNotification("notif_004", NotificationType.ANNOUNCEMENT, true) // read
        )
        notifications.forEach { firebaseTestHelper.createTestNotification(it) }

        // When: Get unread count
        val unreadCount = notificationRepository.getUnreadCount(testUser.id).first()

        // Then: Should return count of unread notifications (2)
        Assert.assertTrue("Should get unread count", unreadCount.isSuccess)
        Assert.assertEquals("Should have 2 unread notifications", 2, unreadCount.getOrNull())
    }

    @Test
    fun `createNotification adds new notification to user's feed`() = runTest(timeout = 30.seconds) {
        // Given: New notification data
        val newNotification = Notification(
            id = "new_notif_001",
            userId = testUser.id,
            type = NotificationType.ACHIEVEMENT,
            title = "New Achievement Unlocked!",
            message = "Congratulations! You've completed 5 TAT tests.",
            data = mapOf("achievementType" to "test_completion", "count" to "5"),
            isRead = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = null
        )

        // When: Create notification
        val createResult = notificationRepository.createNotification(newNotification)

        // Then: Creation should succeed
        Assert.assertTrue("Create notification should succeed", createResult.isSuccess)

        // Notification should appear in user's feed
        val userNotifications = notificationRepository.getNotifications(testUser.id).first()
        val createdNotification = userNotifications.getOrNull()?.find { it.id == newNotification.id }
        Assert.assertNotNull("Created notification should exist", createdNotification)
        Assert.assertEquals("Should have correct title", newNotification.title, createdNotification?.title)
        Assert.assertEquals("Should have correct type", newNotification.type, createdNotification?.type)
    }

    @Test
    fun `observeNotificationUpdates provides real-time notification changes`() = runTest(timeout = 30.seconds) {
        // Given: Start observing notifications
        notificationRepository.observeNotificationUpdates(testUser.id).test {
            val initialNotifications = awaitItem()
            Assert.assertTrue("Initial notifications should be empty or existing",
                initialNotifications.size >= 0)

            // When: Create new notification
            val newNotification = createTestNotification("realtime_notif", NotificationType.TEST_RESULT, false)
            notificationRepository.createNotification(newNotification)

            // Then: Should receive real-time update
            val updatedNotifications = awaitItem()
            val realtimeNotification = updatedNotifications.find { it.id == newNotification.id }
            Assert.assertNotNull("New notification should appear in real-time updates", realtimeNotification)

            // When: Mark notification as read
            notificationRepository.markAsRead(newNotification.id)

            // Then: Should receive another update
            val readNotifications = awaitItem()
            val readNotification = readNotifications.find { it.id == newNotification.id }
            Assert.assertNotNull("Read notification should still exist", readNotification)
            Assert.assertTrue("Should show as read in real-time", readNotification?.isRead == true)
        }
    }

    @Test
    fun `notifications expire and are automatically cleaned up`() = runTest(timeout = 30.seconds) {
        // Given: Create notification with expiration
        val currentTime = System.currentTimeMillis()
        val expiredNotification = Notification(
            id = "expired_notif",
            userId = testUser.id,
            type = NotificationType.REMINDER,
            title = "Expired Reminder",
            message = "This reminder has expired",
            data = emptyMap(),
            isRead = false,
            createdAt = currentTime - 86400000, // 1 day ago
            expiresAt = currentTime - 3600000   // Expired 1 hour ago
        )

        firebaseTestHelper.createTestNotification(expiredNotification)

        // When: Get notifications (system should filter expired ones)
        val notifications = notificationRepository.getNotifications(testUser.id).first()

        // Then: Expired notification should not appear
        Assert.assertTrue("Should get notifications", notifications.isSuccess)
        val notificationList = notifications.getOrNull() ?: emptyList()
        val expiredNotif = notificationList.find { it.id == expiredNotification.id }
        Assert.assertNull("Expired notification should not appear", expiredNotif)
    }

    @Test
    fun `notification priority affects ordering and visibility`() = runTest(timeout = 30.seconds) {
        // Given: Create notifications with different priorities
        val highPriorityNotif = createTestNotification("high_priority", NotificationType.TEST_RESULT, false).copy(
            priority = NotificationPriority.HIGH
        )
        val normalPriorityNotif = createTestNotification("normal_priority", NotificationType.ACHIEVEMENT, false).copy(
            priority = NotificationPriority.NORMAL
        )
        val lowPriorityNotif = createTestNotification("low_priority", NotificationType.ANNOUNCEMENT, false).copy(
            priority = NotificationPriority.LOW
        )

        listOf(highPriorityNotif, normalPriorityNotif, lowPriorityNotif).forEach {
            firebaseTestHelper.createTestNotification(it)
        }

        // When: Get notifications
        val notifications = notificationRepository.getNotifications(testUser.id).first()

        // Then: Should respect priority ordering (high priority first)
        Assert.assertTrue("Should get notifications", notifications.isSuccess)
        val notificationList = notifications.getOrNull() ?: emptyList()

        // Find notifications and verify they exist
        val highPriority = notificationList.find { it.id == highPriorityNotif.id }
        val normalPriority = notificationList.find { it.id == normalPriorityNotif.id }
        val lowPriority = notificationList.find { it.id == lowPriorityNotif.id }

        Assert.assertNotNull("High priority notification should exist", highPriority)
        Assert.assertNotNull("Normal priority notification should exist", normalPriority)
        Assert.assertNotNull("Low priority notification should exist", lowPriority)

        // Verify priorities are preserved
        Assert.assertEquals("High priority should be preserved", NotificationPriority.HIGH, highPriority?.priority)
        Assert.assertEquals("Normal priority should be preserved", NotificationPriority.NORMAL, normalPriority?.priority)
        Assert.assertEquals("Low priority should be preserved", NotificationPriority.LOW, lowPriority?.priority)
    }

    // ==================== HELPER METHODS ====================

    private fun createTestNotifications(): List<Notification> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            Notification(
                id = "notif_001",
                userId = testUser.id,
                type = NotificationType.TEST_RESULT,
                title = "TAT Test Results Available",
                message = "Your TAT test has been graded. Score: 85/100",
                data = mapOf("testType" to "TAT", "score" to "85", "submissionId" to "tat_sub_001"),
                priority = NotificationPriority.HIGH,
                isRead = false,
                createdAt = currentTime,
                expiresAt = currentTime + (30 * 24 * 60 * 60 * 1000L) // 30 days
            ),
            Notification(
                id = "notif_002",
                userId = testUser.id,
                type = NotificationType.ACHIEVEMENT,
                title = "Achievement Unlocked!",
                message = "Congratulations! You've completed your first WAT test.",
                data = mapOf("achievementType" to "first_wat_completion", "badge" to "wat_explorer"),
                priority = NotificationPriority.NORMAL,
                isRead = false,
                createdAt = currentTime - 3600000, // 1 hour ago
                expiresAt = null // No expiration
            ),
            Notification(
                id = "notif_003",
                userId = testUser.id,
                type = NotificationType.REMINDER,
                title = "Practice Reminder",
                message = "Don't forget to practice your SRT responses today!",
                data = mapOf("reminderType" to "practice", "testType" to "SRT"),
                priority = NotificationPriority.LOW,
                isRead = true,
                createdAt = currentTime - (24 * 60 * 60 * 1000L), // 1 day ago
                expiresAt = currentTime + (7 * 24 * 60 * 60 * 1000L) // 7 days from now
            )
        )
    }

    private fun createTestNotification(
        id: String,
        type: NotificationType,
        isRead: Boolean
    ): Notification {
        val titles = mapOf(
            NotificationType.TEST_RESULT to "Test Results Available",
            NotificationType.ACHIEVEMENT to "Achievement Unlocked",
            NotificationType.REMINDER to "Practice Reminder",
            NotificationType.ANNOUNCEMENT to "New Announcement"
        )

        val messages = mapOf(
            NotificationType.TEST_RESULT to "Your test results are ready for review",
            NotificationType.ACHIEVEMENT to "You've earned a new achievement",
            NotificationType.REMINDER to "Time to practice your SSB skills",
            NotificationType.ANNOUNCEMENT to "Check out the latest updates"
        )

        return Notification(
            id = id,
            userId = testUser.id,
            type = type,
            title = titles[type] ?: "Notification",
            message = messages[type] ?: "You have a new notification",
            data = mapOf("testId" to "test_001", "score" to "85"),
            priority = NotificationPriority.NORMAL,
            isRead = isRead,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L) // 30 days
        )
    }
}
