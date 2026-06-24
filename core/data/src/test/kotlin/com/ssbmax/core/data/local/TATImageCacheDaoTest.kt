package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.TATImageCacheDao
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
import com.ssbmax.core.data.local.entity.TATBatchMetadataEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TATImageCacheDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: TATImageCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.tatImageCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndQueryByBatch_returnsImages() = runTest {
        val batchId = "batch-1"
        val images = listOf(
            entity(id = "tat_001_male", pos = 1, gender = "MALE", batchId = batchId),
            entity(id = "tat_002_female", pos = 2, gender = "FEMALE", batchId = batchId)
        )
        dao.insertImages(images)

        val loaded = dao.getImagesByBatch(batchId)
        assertEquals(2, loaded.size)
    }

    @Test
    fun getLeastUsedImageByPosition_returnsMatchingGender() = runTest {
        dao.insertImages(listOf(
            entity(id = "tat_001_male", pos = 1, gender = "MALE"),
            entity(id = "tat_001_female", pos = 1, gender = "FEMALE"),
            entity(id = "tat_001_mixed", pos = 1, gender = "MIXED")
        ))

        // MALE user gets MALE image first (usageCount = 0 for both MALE and MIXED)
        val result = dao.getLeastUsedImageByPosition(cardPosition = 1, genderTag = "MALE")
        assertNotNull(result)
        // Result is either MALE or MIXED (both match the WHERE clause)
        assert(result!!.genderTag == "MALE" || result.genderTag == "MIXED")
    }

    @Test
    fun getLeastUsedImageByPosition_mixedFallsThrough() = runTest {
        // Only MIXED image at position 3 — should be returned for FEMALE user
        dao.insertImages(listOf(
            entity(id = "tat_003_mixed", pos = 3, gender = "MIXED")
        ))

        val result = dao.getLeastUsedImageByPosition(cardPosition = 3, genderTag = "FEMALE")
        assertNotNull(result)
        assertEquals("MIXED", result!!.genderTag)
    }

    @Test
    fun getLeastUsedImageByPosition_returnsNullWhenNoneAvailable() = runTest {
        val result = dao.getLeastUsedImageByPosition(cardPosition = 5, genderTag = "MALE")
        assertNull(result)
    }

    @Test
    fun getLeastUsedImageByPosition_prefersLeastUsed() = runTest {
        // Two MIXED images at the same position — lower usageCount should come first
        val fresh = entity(id = "tat_002_mixed_a", pos = 2, gender = "MIXED", usage = 0)
        val used = entity(id = "tat_002_mixed_b", pos = 2, gender = "MIXED", usage = 5)
        dao.insertImages(listOf(used, fresh))

        val result = dao.getLeastUsedImageByPosition(cardPosition = 2, genderTag = "MALE")
        assertNotNull(result)
        assertEquals(0, result!!.usageCount)
    }

    @Test
    fun markImagesAsUsed_incrementsUsageAndLastUsed() = runTest {
        val now = 1234L
        dao.insertImages(listOf(
            entity(id = "a", pos = 1, gender = "MIXED"),
            entity(id = "b", pos = 2, gender = "MIXED")
        ))

        dao.markImagesAsUsed(listOf("a", "b"), timestamp = now)

        val all = dao.getAllImages()
        val a = all.first { it.id == "a" }
        val b = all.first { it.id == "b" }
        assertEquals(1, a.usageCount)
        assertEquals(1, b.usageCount)
        assertEquals(now, a.lastUsed)
        assertEquals(now, b.lastUsed)
    }

    @Test
    fun batchMetadata_stalenessCheckAt_persistsAndReads() = runTest {
        val meta = TATBatchMetadataEntity(
            batchId = "batch_001",
            downloadedAt = 1000L,
            imageCount = 167,
            version = "1.0.0",
            lastStalenessCheckAt = 9999L
        )
        dao.insertBatchMetadata(meta)

        val loaded = dao.getBatchMetadata("batch_001")
        assertNotNull(loaded)
        assertEquals(9999L, loaded!!.lastStalenessCheckAt)
    }

    private fun entity(
        id: String,
        pos: Int,
        gender: String,
        batchId: String = "batch_001",
        usage: Int = 0
    ): CachedTATImageEntity = CachedTATImageEntity(
        id = id,
        imageUrl = "https://example.com/$id.jpg",
        cardPosition = pos,
        genderTag = gender,
        imageContextJson = "{}",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 150,
        maxCharacters = 1500,
        category = null,
        difficulty = null,
        batchId = batchId,
        cachedAt = 0L,
        lastUsed = null,
        usageCount = usage
    )
}
