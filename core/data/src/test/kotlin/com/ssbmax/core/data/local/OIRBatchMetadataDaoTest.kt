package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
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
class OIRBatchMetadataDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: OIRQuestionCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.oirQuestionCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndQueryBatchMetadata() = runTest {
        val meta = OIRBatchMetadataEntity(
            batchId = "batch1",
            downloadedAt = 100L,
            questionCount = 50,
            version = "v1"
        )
        dao.insertBatchMetadata(meta)

        val exists = dao.isBatchDownloaded("batch1")
        val all = dao.getAllBatchMetadata()
        val count = dao.getBatchCount()

        assertTrue(exists)
        assertEquals(listOf(meta), all)
        assertEquals(1, count)
    }
}


