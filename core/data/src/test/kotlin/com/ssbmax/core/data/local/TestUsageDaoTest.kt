package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.entity.TestUsageEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TestUsageDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: TestUsageDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.testUsageDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertOrReplace_thenGetUsage_returnsInserted() = runTest {
        val entity = TestUsageEntity(
            id = "u1_2025-01",
            userId = "u1",
            month = "2025-01",
            oirTestsUsed = 1,
            ppdtTestsUsed = 0,
            piqTestsUsed = 1,
            sdTestsUsed = 0,
            watTestsUsed = 0,
            srtTestsUsed = 0,
            tatTestsUsed = 0,
            gtoTestsUsed = 0,
            interviewTestsUsed = 0
        )

        dao.insertOrReplace(entity)

        val loaded = dao.getUsage("u1", "2025-01")
        assertEquals(entity, loaded)
    }

    @Test
    fun deleteOldRecords_removesOlderThanThreshold() = runTest {
        val keep = TestUsageEntity(
            id = "u1_2025-01",
            userId = "u1",
            month = "2025-01",
            oirTestsUsed = 1,
            ppdtTestsUsed = 1,
            piqTestsUsed = 1,
            sdTestsUsed = 1,
            watTestsUsed = 1,
            srtTestsUsed = 1,
            tatTestsUsed = 1,
            gtoTestsUsed = 1,
            interviewTestsUsed = 1
        )
        val old = keep.copy(id = "u1_2024-06", month = "2024-06")

        dao.insertOrReplace(keep)
        dao.insertOrReplace(old)

        dao.deleteOldRecords("2024-12")

        assertNull(dao.getUsage("u1", "2024-06"))
        assertEquals(keep, dao.getUsage("u1", "2025-01"))
    }
}
