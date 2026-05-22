package com.ssbmax.core.data.repository.interview

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.Instant

class InterviewResultManagerTest {

    private lateinit var resultManager: InterviewResultManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: InterviewSessionManager
    private lateinit var responseManager: InterviewResponseManager
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var subscriptionManager: SubscriptionManager

    private val testSessionId = "session-123"
    private val testUserId = "user-456"

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        // Mock Firestore calls
        firestore = mockk(relaxed = true)
        val mockCollection = mockk<CollectionReference>(relaxed = true)
        val mockDocument = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument
        every { mockDocument.set(any()) } returns Tasks.forResult(null)

        sessionManager = mockk(relaxed = true)
        responseManager = mockk(relaxed = true)
        subscriptionRepository = mockk(relaxed = true)
        subscriptionManager = mockk(relaxed = true)

        resultManager = InterviewResultManager(
            firestore = firestore,
            sessionManager = sessionManager,
            responseManager = responseManager,
            subscriptionRepository = subscriptionRepository,
            subscriptionManager = subscriptionManager
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `completeInterview saves result, updates session, and records usage atomically`() = runTest {
        // Given
        val mockSession = InterviewSession(
            id = testSessionId,
            userId = testUserId,
            mode = InterviewMode.VOICE_BASED,
            status = InterviewStatus.IN_PROGRESS,
            startedAt = Instant.now().minusSeconds(300),
            completedAt = null,
            piqSnapshotId = "piq-111",
            consentGiven = true,
            questionIds = listOf("q1", "q2"),
            currentQuestionIndex = 1,
            estimatedDuration = 15
        )

        val mockResponses = listOf(
            InterviewResponse(
                id = "resp-1",
                sessionId = testSessionId,
                questionId = "q1",
                responseText = "Sample answer 1",
                responseMode = InterviewMode.VOICE_BASED,
                respondedAt = Instant.now(),
                thinkingTimeSec = 5,
                audioUrl = null,
                olqScores = mapOf(
                    OLQ.EFFECTIVE_INTELLIGENCE to OLQScore(7, 85, "Good reasoning"),
                    OLQ.COOPERATION to OLQScore(6, 80, "Cooperative")
                ),
                confidenceScore = 80
            )
        )

        coEvery { sessionManager.getSession(testSessionId) } returns Result.success(mockSession)
        coEvery { responseManager.getResponses(testSessionId) } returns Result.success(mockResponses)
        coEvery { sessionManager.updateSession(any()) } returns Result.success(Unit)

        // When
        val result = resultManager.completeInterview(testSessionId)

        // Then
        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Interview completion should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val interviewResult = result.getOrNull()
        assertEquals(testSessionId, interviewResult?.sessionId)
        assertEquals(testUserId, interviewResult?.userId)
        assertEquals(7, interviewResult?.overallOLQScores?.get(OLQ.EFFECTIVE_INTELLIGENCE)?.score)

        // Verify session updated to COMPLETED
        coVerify(exactly = 1) {
            sessionManager.updateSession(withArg {
                assertEquals(InterviewStatus.COMPLETED, it.status)
            })
        }

        // Verify that usage is atomically recorded via SubscriptionManager
        coVerify(exactly = 1) {
            subscriptionManager.recordTestUsage(
                testType = TestType.IO,
                userId = testUserId,
                submissionId = "interview_${interviewResult?.id}"
            )
        }
    }
}
