package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.entity.SyncStatus
import com.ssbmax.core.data.local.entity.TestResultEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TestResultDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: TestResultDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.testResultDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndQueryOrdersByCompletedAtDescending() = runTest {
        val r1 = entity(id = "1", completedAt = 10L)
        val r2 = entity(id = "2", completedAt = 20L)
        dao.insertAll(listOf(r1, r2))

        val results = dao.getResults("u1").first()
        assertEquals(listOf(r2, r1), results)
    }

    @Test
    fun pendingSyncAndUpdateStatus() = runTest {
        val pending = entity(id = "p", syncStatus = SyncStatus.PENDING)
        val synced = entity(id = "s", syncStatus = SyncStatus.SYNCED)
        dao.insertAll(listOf(pending, synced))

        val pendingList = dao.getPendingSync()
        assertEquals(listOf(pending), pendingList)

        dao.updateSyncStatus(id = "p", status = SyncStatus.SYNCED)
        val pendingAfter = dao.getPendingSync()
        assertEquals(emptyList<TestResultEntity>(), pendingAfter)
    }

    private fun entity(
        id: String,
        completedAt: Long = 0L,
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): TestResultEntity = TestResultEntity(
        id = id,
        testId = "t1",
        userId = "u1",
        score = 80f,
        maxScore = 100f,
        completedAt = completedAt,
        timeSpentMinutes = 5,
        syncStatus = syncStatus
    )
}









