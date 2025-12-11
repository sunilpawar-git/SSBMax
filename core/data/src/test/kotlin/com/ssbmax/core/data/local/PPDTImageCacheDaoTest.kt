package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
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
class PPDTImageCacheDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: PPDTImageCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.ppdtImageCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getRandomImages_prioritizesLeastUsed() = runTest {
        val images = listOf(
            entity(id = "a", usage = 2),
            entity(id = "b", usage = 0),
            entity(id = "c", usage = 1)
        )
        dao.insertImages(images)

        val random = dao.getRandomImages(2)
        assertEquals(setOf("b", "c"), random.map { it.id }.toSet())
    }

    @Test
    fun markImageAsDownloaded_setsPathAndFlag() = runTest {
        val image = entity(id = "a", usage = 0)
        dao.insertImage(image)

        dao.markImageAsDownloaded(imageId = "a", localPath = "/tmp/ppdt")

        val loaded = dao.getAllImages().first { it.id == "a" }
        assertTrue(loaded.imageDownloaded)
        assertEquals("/tmp/ppdt", loaded.localFilePath)
    }

    private fun entity(
        id: String,
        usage: Int
    ): CachedPPDTImageEntity = CachedPPDTImageEntity(
        id = id,
        imageUrl = "https://example.com/$id",
        localFilePath = null,
        imageDescription = "desc",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 200,
        maxCharacters = 1000,
        category = null,
        difficulty = null,
        batchId = "batch",
        cachedAt = 0L,
        lastUsed = null,
        usageCount = usage,
        imageDownloaded = false
    )
}
