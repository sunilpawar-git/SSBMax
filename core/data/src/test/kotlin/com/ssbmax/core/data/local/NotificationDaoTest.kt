package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.NotificationDao
import com.ssbmax.core.data.local.entity.NotificationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NotificationDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.notificationDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndQueryUnreadCount_updatesWithMarkAllAsRead() = runTest {
        val n1 = entity(id = "n1", userId = "u1", isRead = false)
        val n2 = entity(id = "n2", userId = "u1", isRead = false)
        dao.insert(n1)
        dao.insert(n2)

        val unreadBefore = dao.getUnreadCount("u1").first()
        assertEquals(2, unreadBefore)

        dao.markAllAsRead("u1")

        val unreadAfter = dao.getUnreadCount("u1").first()
        assertEquals(0, unreadAfter)
    }

    @Test
    fun deleteExpired_removesOldItems() = runTest {
        val now = 100L
        val expired = entity(id = "old", userId = "u1", expiresAt = 50L)
        val fresh = entity(id = "new", userId = "u1", expiresAt = 200L)
        dao.insert(expired)
        dao.insert(fresh)

        dao.deleteExpired(now)

        val remaining = dao.getNotifications("u1").first()
        assertEquals(listOf(fresh), remaining)
    }

    private fun entity(
        id: String,
        userId: String,
        isRead: Boolean = false,
        expiresAt: Long? = null
    ): NotificationEntity = NotificationEntity(
        id = id,
        userId = userId,
        type = "GENERAL",
        priority = "LOW",
        title = "title",
        message = "message",
        imageUrl = null,
        actionUrl = null,
        actionDataJson = null,
        isRead = isRead,
        createdAt = 0L,
        expiresAt = expiresAt
    )
}
