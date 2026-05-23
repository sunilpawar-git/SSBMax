package com.ssbmax.core.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.ssbmax.core.domain.model.Batch
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FirestoreBatchRepositoryTest {

    private lateinit var repository: FirestoreBatchRepository
    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var documentRef: DocumentReference

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        firestore = mockk(relaxed = true)
        collectionRef = mockk(relaxed = true)
        documentRef = mockk(relaxed = true)

        every { firestore.collection("batches") } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef
        
        repository = FirestoreBatchRepository(firestore)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun createBatch_setsDocumentOnFirestore() = runTest {
        val batch = Batch(
            id = "batch_123",
            name = "NDA Super Batch",
            description = "Intensive preparation",
            instructorId = "inst_999",
            inviteCode = "NDA999"
        )
        
        val mockTask = Tasks.forResult<Void>(null)
        every { documentRef.set(any()) } returns mockTask

        val result = repository.createBatch(batch)

        assertTrue(result.isSuccess)
        verify { documentRef.set(match<Map<String, Any?>> { map ->
            map["id"] == "batch_123" && map["instructorId"] == "inst_999"
        }) }
    }

    @Test
    fun getBatchesForInstructor_attachesSnapshotListener() = runTest {
        val mockQuery = mockk<Query>(relaxed = true)
        every { collectionRef.whereEqualTo("instructorId", "inst_999") } returns mockQuery
        
        val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)
        val snapshotSlot = slot<EventListener<QuerySnapshot>>()
        
        every { mockQuery.addSnapshotListener(capture(snapshotSlot)) } returns mockListenerRegistration

        val flow = repository.getBatchesForInstructor("inst_999")
        assertNotNull(flow)

        val job = launch {
            val result = flow.first()
            assertTrue(result.isSuccess)
            val batches = result.getOrThrow()
            assertEquals(1, batches.size)
            assertEquals("batch_123", batches[0].id)
        }

        testScheduler.advanceUntilIdle()

        val mockSnapshot = mockk<QuerySnapshot>(relaxed = true)
        val mockDoc = mockk<QueryDocumentSnapshot>(relaxed = true)
        every { mockDoc.getString("id") } returns "batch_123"
        every { mockDoc.getString("name") } returns "NDA Super Batch"
        every { mockDoc.getString("instructorId") } returns "inst_999"
        every { mockDoc.getString("inviteCode") } returns "NDA999"
        every { mockSnapshot.documents } returns listOf(mockDoc)

        snapshotSlot.captured.onEvent(mockSnapshot, null)

        testScheduler.advanceUntilIdle()
        job.join()
    }

    @Test
    fun getBatch_attachesSnapshotListenerAndEmitsBatch() = runTest {
        val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)
        val snapshotSlot = slot<EventListener<DocumentSnapshot>>()

        every { documentRef.addSnapshotListener(capture(snapshotSlot)) } returns mockListenerRegistration

        val flow = repository.getBatch("batch_123")
        assertNotNull(flow)

        val job = launch {
            val result = flow.first()
            assertTrue(result.isSuccess)
            val batch = result.getOrThrow()
            assertEquals("batch_123", batch.id)
            assertEquals("NDA Super Batch", batch.name)
        }

        testScheduler.advanceUntilIdle()

        val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.getString("id") } returns "batch_123"
        every { mockSnapshot.getString("name") } returns "NDA Super Batch"
        every { mockSnapshot.getString("instructorId") } returns "inst_999"
        every { mockSnapshot.getString("inviteCode") } returns "NDA999"

        snapshotSlot.captured.onEvent(mockSnapshot, null)

        testScheduler.advanceUntilIdle()
        job.join()
    }
}
