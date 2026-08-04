package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.NotificationPriority
import com.ssbmax.shared.domain.model.NotificationType
import com.ssbmax.shared.domain.model.SSBMaxNotification
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises GitLiveNotificationCacheManager against a real in-memory SQLite
 * DB. Why these assertions matter: [SSBMaxNotification.actionData] is a
 * Map<String, String> that the Android original serialized by hand-rolled
 * string concatenation with no escaping -- this port switched to
 * kotlinx.serialization JSON specifically to survive values containing
 * commas/quotes/colons that would have corrupted the old round-trip, so a
 * round-trip test with exactly that kind of value is the whole point, not
 * a formality.
 */
class GitLiveNotificationCacheManagerTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLiveNotificationCacheManager

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLiveNotificationCacheManager(database)
    }

    private fun notification(
        id: String,
        userId: String = "user-1",
        isRead: Boolean = false,
        createdAt: Long = 0L,
        expiresAt: Long? = null,
        actionData: Map<String, String>? = null
    ) = SSBMaxNotification(
        id = id,
        userId = userId,
        type = NotificationType.GRADING_COMPLETE,
        priority = NotificationPriority.NORMAL,
        title = "Title $id",
        message = "Message $id",
        isRead = isRead,
        createdAt = createdAt,
        expiresAt = expiresAt,
        actionData = actionData
    )

    @Test
    fun `getNotifications emits newest first and survives a comma-and-quote-bearing actionData round trip`() = runTest {
        // Why this matters: the Android hand-rolled JSON split on "," and stripped bare
        // quotes -- a value like the one below would have silently corrupted on that path.
        val trickyActionData = mapOf("note" to "has, a comma \"and quotes\"", "url" to "https://x/y?a=1&b=2")
        manager.insert(notification(id = "old", createdAt = 100L))
        manager.insert(notification(id = "new", createdAt = 200L, actionData = trickyActionData))

        manager.getNotifications("user-1").test {
            val list = awaitItem()
            assertEquals(listOf("new", "old"), list.map { it.id })
            assertEquals(trickyActionData, list.first { it.id == "new" }.actionData)
        }
    }

    @Test
    fun `getUnreadCount reflects only this user's unread rows`() = runTest {
        manager.insert(notification(id = "n1", userId = "user-1", isRead = false))
        manager.insert(notification(id = "n2", userId = "user-1", isRead = false))
        manager.insert(notification(id = "n3", userId = "user-2", isRead = false))

        manager.getUnreadCount("user-1").test {
            assertEquals(2, awaitItem())
        }
    }

    @Test
    fun `markAsRead flips only the targeted notification`() = runTest {
        manager.insert(notification(id = "n1", userId = "user-1", isRead = false))
        manager.insert(notification(id = "n2", userId = "user-1", isRead = false))

        manager.markAsRead("n1")

        manager.getUnreadCount("user-1").test {
            assertEquals(1, awaitItem())
        }
    }

    @Test
    fun `markAllAsRead zeroes the unread count for that user without touching other users`() = runTest {
        manager.insert(notification(id = "n1", userId = "user-1", isRead = false))
        manager.insert(notification(id = "n2", userId = "user-1", isRead = false))
        manager.insert(notification(id = "n3", userId = "user-2", isRead = false))

        manager.markAllAsRead("user-1")

        manager.getUnreadCount("user-1").test { assertEquals(0, awaitItem()) }
        manager.getUnreadCount("user-2").test { assertEquals(1, awaitItem()) }
    }

    @Test
    fun `delete removes the row so it no longer appears in getNotifications`() = runTest {
        manager.insert(notification(id = "n1"))
        manager.insert(notification(id = "n2"))

        manager.delete("n1")

        manager.getNotifications("user-1").test {
            val list = awaitItem()
            assertTrue(list.none { it.id == "n1" })
            assertEquals(1, list.size)
        }
    }
}
