package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.SRTSituationCacheDao
import com.ssbmax.core.data.local.entity.CachedSRTSituationEntity
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
class SRTSituationCacheDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: SRTSituationCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.srtSituationCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetLeastUsed_ordersByUsageThenLastUsed() = runTest {
        val now = 10L
        val situations = listOf(
            entity(id = "a", usage = 2, lastUsed = 5L),
            entity(id = "b", usage = 0, lastUsed = 1L),
            entity(id = "c", usage = 1, lastUsed = 2L)
        )
        dao.insertSituations(situations)

        val least = dao.getLeastUsedSituations(2)
        assertEquals(listOf("b", "c"), least.map { it.id })
    }

    @Test
    fun markSituationsAsUsed_updatesUsageAndLastUsed() = runTest {
        val situations = listOf(
            entity(id = "a", usage = 0, lastUsed = null),
            entity(id = "b", usage = 0, lastUsed = null)
        )
        dao.insertSituations(situations)

        dao.markSituationsAsUsed(listOf("a", "b"), timestamp = 50L)

        val updated = dao.getAllSituations()
        assertTrue(updated.all { it.usageCount == 1 })
        assertTrue(updated.all { it.lastUsed == 50L })
    }

    private fun entity(
        id: String,
        usage: Int,
        lastUsed: Long?
    ): CachedSRTSituationEntity = CachedSRTSituationEntity(
        id = id,
        situation = "Situation $id",
        sequenceNumber = usage + 1,
        category = "leadership",
        timeAllowedSeconds = 30,
        difficulty = null,
        batchId = "batch",
        cachedAt = 0L,
        lastUsed = lastUsed,
        usageCount = usage
    )
}
