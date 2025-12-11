package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.TATImageCacheDao
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
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
    fun insertAndQueryByBatch_returnsOrderedImages() = runTest {
        val batchId = "batch-1"
        val images = listOf(
            entity(id = "a", seq = 2, batchId = batchId),
            entity(id = "b", seq = 1, batchId = batchId)
        )

        dao.insertImages(images)

        val loaded = dao.getImagesByBatch(batchId)
        assertEquals(listOf(images[1], images[0]), loaded)
    }

    @Test
    fun markImagesAsUsed_incrementsUsageAndLastUsed() = runTest {
        val now = 1234L
        val images = listOf(
            entity(id = "a", seq = 1, batchId = "b"),
            entity(id = "b", seq = 2, batchId = "b")
        )
        dao.insertImages(images)

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
    fun markImageAsDownloaded_setsFlagAndPath() = runTest {
        val image = entity(id = "a", seq = 1, batchId = "b")
        dao.insertImage(image)

        dao.markImageAsDownloaded(imageId = "a", localPath = "/tmp/file")

        val updated = dao.getAllImages().first { it.id == "a" }
        assertTrue(updated.imageDownloaded)
        assertEquals("/tmp/file", updated.localFilePath)
    }

    private fun entity(
        id: String,
        seq: Int,
        batchId: String
    ): CachedTATImageEntity = CachedTATImageEntity(
        id = id,
        imageUrl = "https://example.com/$id",
        localFilePath = null,
        sequenceNumber = seq,
        prompt = "Prompt",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 150,
        maxCharacters = 800,
        category = null,
        difficulty = null,
        batchId = batchId,
        cachedAt = 0L,
        lastUsed = null,
        usageCount = 0,
        imageDownloaded = false
    )
}
