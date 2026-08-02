package com.ssbmax.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.ssbmax.core.data.FirebaseTestHelper
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.shared.domain.model.FCMToken
import com.ssbmax.shared.domain.model.NotificationPriority
import com.ssbmax.shared.domain.model.NotificationType
import com.ssbmax.shared.domain.model.SSBMaxNotification
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for [NotificationRepositoryImpl] with Firebase Emulator + an in-memory Room DB.
 *
 * Ported (KMP Phase 7) from a Hilt/`BaseRepositoryTest`-based version that predated the
 * Phase 3 Hilt->Koin migration and never compiled afterwards (missing `hilt-android-testing`
 * dependency, deleted `ThemePreferenceManager` import, and an API surface -
 * `createNotification`/`observeNotificationUpdates`/priority ordering - that no longer matches
 * [com.ssbmax.shared.domain.repository.NotificationRepository]'s real methods, which split reads
 * between Room (`getNotifications`/`getUnreadCount`, local-only) and Firestore (everything else)).
 * Rewritten against the actual current interface using this module's plain-JUnit +
 * `FirebaseTestHelper.getEmulatorFirestore()` pattern, plus the `Room.inMemoryDatabaseBuilder`
 * pattern already used by this module's DAO androidTests (e.g. `TestResultDaoTest`).
 *
 * Prerequisites: Firebase Emulator running (`firebase emulators:start --only firestore`).
 */
class NotificationRepositoryImplTest {

    private lateinit var database: SSBDatabase
    private lateinit var repository: NotificationRepositoryImpl
    private val testUserId = "student_${System.nanoTime()}"
    private val seededNotificationIds = mutableListOf<String>()
    private val seededTokenKeys = mutableListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firestore = FirebaseTestHelper.getEmulatorFirestore()
        database = Room.inMemoryDatabaseBuilder(context, SSBDatabase::class.java).build()
        repository = NotificationRepositoryImpl(
            firestore = firestore,
            firebaseMessaging = FirebaseMessaging.getInstance(),
            notificationDao = database.notificationDao()
        )
    }

    @After
    fun tearDown() = runTest {
        database.close()
        val firestore = FirebaseTestHelper.getEmulatorFirestore()
        seededNotificationIds.forEach { id ->
            try {
                firestore.collection("notifications").document(id).delete().await()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        seededTokenKeys.forEach { key ->
            try {
                firestore.collection("fcmTokens").document(key).delete().await()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        try {
            firestore.collection("notificationPreferences").document(testUserId).delete().await()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    // Why this matters: `getNotifications` reads only from the local Room cache, not Firestore -
    // if `saveNotification` didn't also write through to Room, the user's feed would show nothing.
    @Test
    fun saveNotification_makes_it_retrievable_from_local_feed() = runTest {
        val notification = testNotification("notif_1")

        val result = repository.saveNotification(notification)

        assertTrue("Save should succeed", result.isSuccess)
        val feed = repository.getNotifications(testUserId).first()
        assertTrue("Saved notification should appear in the feed", feed.any { it.id == notification.id })
    }

    // Why this matters: the unread badge count is a real-time signal users act on -
    // it must track markAsRead exactly, not just eventually.
    @Test
    fun markAsRead_flips_local_read_status_and_decrements_unread_count() = runTest {
        val notification = testNotification("notif_2")
        repository.saveNotification(notification)
        assertEquals("Should start unread", 1, repository.getUnreadCount(testUserId).first())

        val result = repository.markAsRead(notification.id)

        assertTrue("Mark as read should succeed", result.isSuccess)
        assertEquals("Unread count should drop to 0", 0, repository.getUnreadCount(testUserId).first())
        val updated = repository.getNotifications(testUserId).first().first { it.id == notification.id }
        assertTrue("Notification should now be read", updated.isRead)
    }

    // Why this matters: a "mark all as read" action is only useful if it is actually atomic
    // across every notification for that user, not just the most recent one.
    @Test
    fun markAllAsRead_clears_every_unread_notification_for_the_user() = runTest {
        repository.saveNotification(testNotification("notif_3"))
        repository.saveNotification(testNotification("notif_4"))
        repository.saveNotification(testNotification("notif_5"))
        assertEquals(3, repository.getUnreadCount(testUserId).first())

        val result = repository.markAllAsRead(testUserId)

        assertTrue("Mark all as read should succeed", result.isSuccess)
        assertEquals("All notifications should now be read", 0, repository.getUnreadCount(testUserId).first())
    }

    // Why this matters: dismissed notifications must actually disappear from the feed the
    // user sees, not just get flagged.
    @Test
    fun deleteNotification_removes_it_from_the_local_feed() = runTest {
        val notification = testNotification("notif_6")
        repository.saveNotification(notification)

        val result = repository.deleteNotification(notification.id)

        assertTrue("Delete should succeed", result.isSuccess)
        val feed = repository.getNotifications(testUserId).first()
        assertFalse("Deleted notification should not reappear", feed.any { it.id == notification.id })
    }

    // Why this matters: FCM token persistence is what lets the backend actually reach this
    // device - a broken round trip silently breaks push notifications with no visible symptom.
    @Test
    fun saveFCMToken_and_getFCMToken_round_trip_through_firestore() = runTest {
        val token = FCMToken(userId = testUserId, token = "fcm_token_value", deviceId = "device_1")
        seededTokenKeys.add("${token.userId}_${token.deviceId}")

        val saveResult = repository.saveFCMToken(token)
        assertTrue("Save should succeed", saveResult.isSuccess)

        val fetched = repository.getFCMToken(testUserId, "device_1")
        assertTrue("Get should succeed", fetched.isSuccess)
        assertEquals("token value should round-trip", "fcm_token_value", fetched.getOrNull()?.token)
    }

    // Why this matters: on logout, a stale token left behind would let the old session keep
    // receiving pushes meant for whoever signs in next on the same device.
    @Test
    fun deleteFCMToken_removes_it_so_it_is_no_longer_fetchable() = runTest {
        val token = FCMToken(userId = testUserId, token = "fcm_token_value", deviceId = "device_2")
        repository.saveFCMToken(token)

        val result = repository.deleteFCMToken(testUserId, "device_2")

        assertTrue("Delete should succeed", result.isSuccess)
        val fetched = repository.getFCMToken(testUserId, "device_2")
        assertTrue("Get should still succeed (absence is not an error)", fetched.isSuccess)
        assertNull("Token should no longer exist", fetched.getOrNull())
    }

    // Why this matters: a user who has never touched notification settings must get sane
    // defaults (all opted in, quiet hours off), not a crash or an empty/null preferences object.
    @Test
    fun getPreferences_returns_defaults_for_a_user_with_no_saved_preferences() = runTest {
        val result = repository.getPreferences(testUserId)

        assertTrue("Get should succeed even with nothing saved", result.isSuccess)
        val prefs = result.getOrNull()
        assertNotNull(prefs)
        assertEquals(testUserId, prefs?.userId)
        assertTrue("Push notifications should default to enabled", prefs?.enablePushNotifications == true)
    }

    // Why this matters: preference changes (e.g. disabling push, setting quiet hours) must
    // actually persist - otherwise the user's settings silently revert on next read.
    @Test
    fun savePreferences_persists_custom_settings_and_they_round_trip() = runTest {
        val custom = com.ssbmax.shared.domain.model.NotificationPreferences(
            userId = testUserId,
            enablePushNotifications = false,
            quietHoursEnabled = true,
            quietHoursStart = 23,
            quietHoursEnd = 6
        )

        val saveResult = repository.savePreferences(custom)
        assertTrue("Save should succeed", saveResult.isSuccess)

        val fetched = repository.getPreferences(testUserId).getOrNull()
        assertNotNull(fetched)
        assertFalse("Push notifications should now be disabled", fetched?.enablePushNotifications == true)
        assertTrue("Quiet hours should be enabled", fetched?.quietHoursEnabled == true)
        assertEquals(23, fetched?.quietHoursStart)
    }

    // ==================== HELPER METHODS ====================

    private fun testNotification(id: String): SSBMaxNotification {
        seededNotificationIds.add(id)
        return SSBMaxNotification(
            id = id,
            userId = testUserId,
            type = NotificationType.GRADING_COMPLETE,
            priority = NotificationPriority.NORMAL,
            title = "Test Results Available",
            message = "Your test has been graded",
            isRead = false
        )
    }
}
