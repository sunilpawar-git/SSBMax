package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.OIRSetProgressDao
import com.ssbmax.core.data.local.entity.OIRSetProgressEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class OIRSetProgressDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: OIRSetProgressDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.oirSetProgressDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ==================== getProgressByUserId ====================

    @Test
    fun getProgressByUserId_returnsEmptyListWhenNoRows() = runTest {
        val result = dao.getProgressByUserId("user-abc")
        assertTrue(result.isEmpty())
    }

    @Test
    fun getProgressByUserId_returnsOnlyRowsForThatUser() = runTest {
        dao.upsertProgress(entity("user-A", 1, completed = true))
        dao.upsertProgress(entity("user-A", 2, completed = false))
        dao.upsertProgress(entity("user-B", 1, completed = true))

        val resultA = dao.getProgressByUserId("user-A")
        val resultB = dao.getProgressByUserId("user-B")

        assertEquals(2, resultA.size)
        assertEquals(1, resultB.size)
        assertEquals("user-B", resultB[0].userId)
    }

    @Test
    fun getProgressByUserId_orderedBySetNumberAscending() = runTest {
        dao.upsertProgress(entity("user-A", 3))
        dao.upsertProgress(entity("user-A", 1))
        dao.upsertProgress(entity("user-A", 2))

        val result = dao.getProgressByUserId("user-A")
        assertEquals(listOf(1, 2, 3), result.map { it.setNumber })
    }

    // ==================== upsert (insert + update) ====================

    @Test
    fun upsertProgress_insertsNewRow() = runTest {
        dao.upsertProgress(entity("user-A", 1, completed = false))

        val result = dao.getProgressByUserId("user-A")
        assertEquals(1, result.size)
        assertEquals(1, result[0].setNumber)
        assertEquals(false, result[0].isCompleted)
    }

    @Test
    fun upsertProgress_updatesExistingRowOnSameId() = runTest {
        dao.upsertProgress(entity("user-A", 1, completed = false, score = null))
        dao.upsertProgress(entity("user-A", 1, completed = true, score = 45))

        val result = dao.getProgressByUserId("user-A")
        assertEquals(1, result.size)
        assertEquals(true, result[0].isCompleted)
        assertEquals(45, result[0].score)
    }

    @Test
    fun upsertProgress_nullableScoreStoredCorrectly() = runTest {
        dao.upsertProgress(entity("user-A", 1, completed = false, score = null))
        val rows = dao.getProgressByUserId("user-A")
        assertNull(rows[0].score)
    }

    // ==================== getProgressById ====================

    @Test
    fun getProgressById_returnsNullForMissingId() = runTest {
        val result = dao.getProgressById("user-A_99")
        assertNull(result)
    }

    @Test
    fun getProgressById_returnsCorrectRow() = runTest {
        dao.upsertProgress(entity("user-A", 5, completed = true, score = 40))
        val result = dao.getProgressById("user-A_5")
        assertEquals(5, result?.setNumber)
        assertEquals(true, result?.isCompleted)
        assertEquals(40, result?.score)
    }

    // ==================== deleteProgressByUserId ====================

    @Test
    fun deleteProgressByUserId_removesOnlyThatUsersRows() = runTest {
        dao.upsertProgress(entity("user-A", 1))
        dao.upsertProgress(entity("user-A", 2))
        dao.upsertProgress(entity("user-B", 1))

        dao.deleteProgressByUserId("user-A")

        assertTrue(dao.getProgressByUserId("user-A").isEmpty())
        assertEquals(1, dao.getProgressByUserId("user-B").size)
    }

    // ==================== Helper ====================

    private fun entity(
        userId: String,
        setNumber: Int,
        completed: Boolean = false,
        score: Int? = null
    ) = OIRSetProgressEntity(
        id = "${userId}_${setNumber}",
        userId = userId,
        setNumber = setNumber,
        isCompleted = completed,
        score = score,
        totalQuestions = 50,
        completedAt = if (completed) System.currentTimeMillis() else null
    )
}
