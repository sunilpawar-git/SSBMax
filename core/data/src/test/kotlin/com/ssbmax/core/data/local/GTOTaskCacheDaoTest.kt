package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.GTOTaskCacheDao
import com.ssbmax.core.data.local.entity.CachedGTOTaskEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class GTOTaskCacheDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: GTOTaskCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.gtoTaskCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getLeastUsedTasks_ordersByUsage() = runTest {
        val tasks = listOf(
            entity(id = "a", usage = 3),
            entity(id = "b", usage = 0),
            entity(id = "c", usage = 1)
        )
        dao.insertTasks(tasks)

        val least = dao.getLeastUsedTasks(2)
        assertEquals(listOf("b", "c"), least.map { it.id })
    }

    @Test
    fun markTasksAsUsed_updatesUsageAndLastUsed() = runTest {
        val tasks = listOf(
            entity(id = "a", usage = 0),
            entity(id = "b", usage = 0)
        )
        dao.insertTasks(tasks)

        dao.markTasksAsUsed(listOf("a", "b"), timestamp = 42L)

        val updated = dao.getAllTasks()
        assertTrue(updated.all { it.usageCount == 1 })
        assertTrue(updated.all { it.lastUsed == 42L })
    }

    private fun entity(
        id: String,
        usage: Int
    ): CachedGTOTaskEntity = CachedGTOTaskEntity(
        id = id,
        taskType = "GPE",
        title = "Title $id",
        description = "desc",
        instructions = "inst",
        timeAllowedMinutes = 10,
        difficultyLevel = "easy",
        category = "leadership",
        scenario = null,
        resources = null,
        objectives = null,
        evaluationCriteria = null,
        batchId = "batch",
        cachedAt = 0L,
        lastUsed = null,
        usageCount = usage
    )
}
