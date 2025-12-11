package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.WATWordCacheDao
import com.ssbmax.core.data.local.entity.CachedWATWordEntity
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
class WATWordCacheDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: WATWordCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.watWordCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndRandomWords_prioritizesLeastUsed() = runTest {
        val words = listOf(
            entity(id = "a", seq = 1, usageCount = 2),
            entity(id = "b", seq = 2, usageCount = 0),
            entity(id = "c", seq = 3, usageCount = 1)
        )
        dao.insertWords(words)

        val random = dao.getRandomWords(2)
        assertEquals(setOf("b", "c"), random.map { it.id }.toSet())
    }

    @Test
    fun markWordsAsUsed_incrementsUsage() = runTest {
        val words = listOf(
            entity(id = "a", seq = 1, usageCount = 0),
            entity(id = "b", seq = 2, usageCount = 0)
        )
        dao.insertWords(words)

        dao.markWordsAsUsed(listOf("a", "b"), timestamp = 10L)

        val all = dao.getAllWords()
        assertTrue(all.all { it.usageCount == 1 })
        assertTrue(all.all { it.lastUsed == 10L })
    }

    private fun entity(
        id: String,
        seq: Int,
        usageCount: Int
    ): CachedWATWordEntity = CachedWATWordEntity(
        id = id,
        word = id,
        sequenceNumber = seq,
        category = null,
        difficulty = null,
        batchId = "batch",
        cachedAt = 0L,
        lastUsed = null,
        usageCount = usageCount
    )
}
