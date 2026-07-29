package com.ssbmax.core.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

/**
 * Phase 4-A RED tests: getLatestOIRSubmission must use server-first fetch.
 *
 * Uses mockkStatic("kotlinx.coroutines.tasks.TasksKt") so Task.await() can be
 * stubbed without an Android main Looper. Tasks.forResult relies on the main
 * Looper to dispatch completion callbacks, causing UncompletedCoroutinesError.
 */
class OIRPersonalSubmissionDataSourceTest {

    @get:Rule
    val timeout: Timeout = Timeout(60, TimeUnit.SECONDS)


    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
            every { Log.w(any(), any<String>()) } returns 0
            every { Log.i(any(), any()) } returns 0
            // Make the coroutines-play-services await() extension mockable
            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            // Must unmock static to avoid leaking into other test classes
            unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
            unmockkStatic(Log::class)
        }
    }

    private lateinit var dataSource: OIRPersonalSubmissionDataSource
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockQuery: Query
    private lateinit var serverTask: Task<QuerySnapshot>
    private lateinit var cacheTask: Task<QuerySnapshot>
    private lateinit var defaultTask: Task<QuerySnapshot>
    private lateinit var emptySnapshot: QuerySnapshot

    @Before
    fun setUp() {
        mockFirestore = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockQuery = mockk(relaxed = true)
        serverTask = mockk()
        cacheTask = mockk()
        defaultTask = mockk()
        emptySnapshot = mockk { every { isEmpty } returns true }

        every { mockFirestore.collection("submissions") } returns mockCollection
        every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
        every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
        every { mockQuery.orderBy(any<String>(), any()) } returns mockQuery
        every { mockQuery.limit(any()) } returns mockQuery

        every { mockQuery.get(Source.SERVER) } returns serverTask
        every { mockQuery.get(Source.CACHE) } returns cacheTask
        every { mockQuery.get() } returns defaultTask

        // All tasks complete successfully by default
        coEvery { serverTask.await() } returns emptySnapshot
        coEvery { cacheTask.await() } returns emptySnapshot
        coEvery { defaultTask.await() } returns emptySnapshot

        dataSource = OIRPersonalSubmissionDataSource(firestore = mockFirestore)
    }

    @After
    fun tearDown() {
        clearMocks(mockFirestore, mockCollection, mockQuery,
                   serverTask, cacheTask, defaultTask, emptySnapshot)
    }

    // ==================== Phase 4-A RED: server-first fetch ====================

    @Test
    fun `getLatestOIRSubmission usesServerSourceFirst`() = runTest {
        // Act
        dataSource.getLatestOIRSubmission(userId = "user1")

        // Assert — Source.SERVER must be used (RED: current code calls get() with no source)
        verify(exactly = 1) { mockQuery.get(Source.SERVER) }
    }

    @Test
    fun `getLatestOIRSubmission fallsBackToCache whenServerFails`() = runTest {
        // Arrange — server throws network error
        coEvery { serverTask.await() } throws java.io.IOException("No network")

        // Act
        val result = dataSource.getLatestOIRSubmission(userId = "user1")

        // Assert — must fall back to cache and still return success
        verify(exactly = 1) { mockQuery.get(Source.CACHE) }
        assertTrue("Should succeed after cache fallback", result.isSuccess)
    }
}
