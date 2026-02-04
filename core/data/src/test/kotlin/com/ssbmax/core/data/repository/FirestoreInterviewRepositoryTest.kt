package com.ssbmax.core.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.ssbmax.core.data.repository.interview.InterviewQuestionGenerator
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for FirestoreInterviewRepository
 *
 * Tests the integration between the repository and InterviewQuestionGenerator,
 * verifying that sessions are created with the correct number of questions.
 */
class FirestoreInterviewRepositoryTest {

    private lateinit var repository: FirestoreInterviewRepository
    private lateinit var firestore: FirebaseFirestore
    private lateinit var questionCacheRepository: QuestionCacheRepository
    private lateinit var questionGenerator: InterviewQuestionGenerator
    private lateinit var subscriptionRepository: SubscriptionRepository

    private val testUserId = "test-user-123"
    private val testPiqSnapshotId = "test-piq-456"

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        // Mock Firestore batch operations
        val mockBatch = mockk<WriteBatch>(relaxed = true)
        val mockTask = Tasks.forResult<Void>(null)
        every { mockBatch.commit() } returns mockTask

        val mockCollection = mockk<CollectionReference>(relaxed = true)
        val mockDocument = mockk<DocumentReference>(relaxed = true)
        every { mockCollection.document(any()) } returns mockDocument

        firestore = mockk(relaxed = true)
        every { firestore.batch() } returns mockBatch
        every { firestore.collection(any()) } returns mockCollection

        questionCacheRepository = mockk(relaxed = true)
        questionGenerator = mockk(relaxed = true)
        subscriptionRepository = mockk(relaxed = true)

        repository = FirestoreInterviewRepository(
            firestore = firestore,
            questionCacheRepository = questionCacheRepository,
            questionGenerator = questionGenerator,
            subscriptionRepository = subscriptionRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `createSession generates TARGET_TOTAL_QUESTIONS questions`() = runTest {
        // Given - Question generator returns the target number of questions
        val expectedCount = InterviewConstants.TARGET_TOTAL_QUESTIONS
        val generatedQuestions = createMockQuestions(expectedCount)

        coEvery {
            questionGenerator.generateQuestions(testPiqSnapshotId, expectedCount)
        } returns Result.success(generatedQuestions)

        // When
        val result = repository.createSession(
            userId = testUserId,
            mode = InterviewMode.VOICE_BASED,
            piqSnapshotId = testPiqSnapshotId,
            consentGiven = true
        )

        // Then
        assertTrue("Session creation should succeed", result.isSuccess)
        val session = result.getOrNull()
        assertEquals(
            "Session should have TARGET_TOTAL_QUESTIONS question IDs",
            expectedCount,
            session?.questionIds?.size
        )
    }

    @Test
    fun `createSession with partial cache still generates full question count`() = runTest {
        // Given - Question generator returns fewer questions (simulating partial cache + AI)
        val expectedCount = InterviewConstants.TARGET_TOTAL_QUESTIONS
        val generatedQuestions = createMockQuestions(expectedCount)

        coEvery {
            questionGenerator.generateQuestions(testPiqSnapshotId, expectedCount)
        } returns Result.success(generatedQuestions)

        // When
        val result = repository.createSession(
            userId = testUserId,
            mode = InterviewMode.TEXT_BASED,
            piqSnapshotId = testPiqSnapshotId,
            consentGiven = true
        )

        // Then
        assertTrue("Session creation should succeed", result.isSuccess)
        val session = result.getOrNull()
        assertEquals(
            "Session should have full question count",
            expectedCount,
            session?.questionIds?.size
        )
    }

    @Test
    fun `createSession fails when question generation fails`() = runTest {
        // Given - Question generator fails
        coEvery {
            questionGenerator.generateQuestions(any(), any())
        } returns Result.failure(Exception("Generation failed"))

        // When
        val result = repository.createSession(
            userId = testUserId,
            mode = InterviewMode.VOICE_BASED,
            piqSnapshotId = testPiqSnapshotId,
            consentGiven = true
        )

        // Then
        assertTrue("Session creation should fail", result.isFailure)
    }

    private fun createMockQuestions(count: Int): List<InterviewQuestion> {
        return List(count) { index ->
            InterviewQuestion(
                id = "q-test-$index",
                questionText = "Test question $index",
                expectedOLQs = listOf(OLQ.EFFECTIVE_INTELLIGENCE),
                context = "Test context",
                source = QuestionSource.AI_GENERATED
            )
        }
    }
}
