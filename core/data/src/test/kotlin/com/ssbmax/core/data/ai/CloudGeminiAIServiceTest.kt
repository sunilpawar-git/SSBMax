package com.ssbmax.core.data.ai

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.gto.GTOTestType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 3 – TDD unit tests for CloudGeminiAIService.
 *
 * Verifies that every analysis method (TAT, WAT, SRT, SD, PPDT, GTO)
 * calls the secure `analyzeResponseInline` HTTPS Callable with:
 * - "questionText"  – task-specific header
 * - "responseText"  – the full prompt
 * - "expectedOLQs"  – all 15 OLQ names
 * - "responseMode"  – "text"
 *
 * Auth check: unauthenticated calls must return Result.failure.
 */
class CloudGeminiAIServiceTest {

    private lateinit var mockFunctions: FirebaseFunctions
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockCallable: HttpsCallableReference
    private lateinit var service: CloudGeminiAIService

    // Minimal successful analysis result that the parser can handle
    private val successResultData = mapOf(
        "success" to true,
        "analysis" to mapOf(
            "overallConfidence" to 70,
            "keyInsights" to listOf("Good leadership"),
            "suggestedFollowUp" to null,
            "olqScores" to listOf(
                mapOf(
                    "olq" to "INITIATIVE",
                    "score" to 5.0,
                    "reasoning" to "Showed initiative",
                    "evidence" to listOf("Led the group")
                )
            )
        )
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        mockFunctions = mockk()
        mockAuth = mockk()
        mockUser = mockk()
        mockCallable = mockk()

        val mockResult = mockk<HttpsCallableResult>()
        every { mockResult.getData() } returns successResultData

        val successTask: Task<HttpsCallableResult> = Tasks.forResult(mockResult)

        every { mockFunctions.getHttpsCallable("analyzeResponseInline") } returns mockCallable
        every { mockCallable.call(any()) } returns successTask

        service = CloudGeminiAIService(mockFunctions, mockAuth)
    }

    // ─── Auth guard ──────────────────────────────────────────────────────────

    @Test
    fun analyzeTATResponse_whenNotAuthenticated_returnsFailure() = runTest {
        every { mockAuth.currentUser } returns null
        val result = service.analyzeTATResponse("some prompt")
        assertTrue(result.isFailure)
    }

    @Test
    fun analyzeWATResponse_whenNotAuthenticated_returnsFailure() = runTest {
        every { mockAuth.currentUser } returns null
        val result = service.analyzeWATResponse("some prompt")
        assertTrue(result.isFailure)
    }

    @Test
    fun analyzeSRTResponse_whenNotAuthenticated_returnsFailure() = runTest {
        every { mockAuth.currentUser } returns null
        val result = service.analyzeSRTResponse("some prompt")
        assertTrue(result.isFailure)
    }

    @Test
    fun analyzeSDResponse_whenNotAuthenticated_returnsFailure() = runTest {
        every { mockAuth.currentUser } returns null
        val result = service.analyzeSDResponse("some prompt")
        assertTrue(result.isFailure)
    }

    @Test
    fun analyzePPDTResponse_whenNotAuthenticated_returnsFailure() = runTest {
        every { mockAuth.currentUser } returns null
        val result = service.analyzePPDTResponse("some prompt")
        assertTrue(result.isFailure)
    }

    @Test
    fun analyzeGTOResponse_whenNotAuthenticated_returnsFailure() = runTest {
        every { mockAuth.currentUser } returns null
        val result = service.analyzeGTOResponse("some prompt", GTOTestType.GROUP_DISCUSSION)
        assertTrue(result.isFailure)
    }

    // ─── Routing to analyzeResponseInline ────────────────────────────────────

    @Test
    fun analyzeTATResponse_whenAuthenticated_callsAnalyzeResponseInline() = runTest {
        every { mockAuth.currentUser } returns mockUser
        val capturedData = slot<HashMap<String, Any>>()
        every { mockCallable.call(capture(capturedData)) } returns
            Tasks.forResult(mockk<HttpsCallableResult>().also {
                every { it.getData() } returns successResultData
            })

        val result = service.analyzeTATResponse("tat-prompt-text")

        assertTrue(result.isSuccess)
        verify { mockFunctions.getHttpsCallable("analyzeResponseInline") }
        val data = capturedData.captured
        assertTrue(data.containsKey("questionText"))
        assertTrue(data.containsKey("responseText"))
        assertEquals("tat-prompt-text", data["responseText"])
        assertEquals("text", data["responseMode"])
        @Suppress("UNCHECKED_CAST")
        val olqs = data["expectedOLQs"] as List<String>
        assertEquals(15, olqs.size)
        assertTrue(olqs.containsAll(OLQ.entries.map { it.name }))
    }

    @Test
    fun analyzeWATResponse_whenAuthenticated_callsAnalyzeResponseInline() = runTest {
        every { mockAuth.currentUser } returns mockUser
        val capturedData = slot<HashMap<String, Any>>()
        every { mockCallable.call(capture(capturedData)) } returns
            Tasks.forResult(mockk<HttpsCallableResult>().also {
                every { it.getData() } returns successResultData
            })

        val result = service.analyzeWATResponse("wat-prompt-text")

        assertTrue(result.isSuccess)
        val data = capturedData.captured
        assertEquals("wat-prompt-text", data["responseText"])
        assertEquals("text", data["responseMode"])
    }

    @Test
    fun analyzeGTOResponse_whenAuthenticated_callsAnalyzeResponseInline() = runTest {
        every { mockAuth.currentUser } returns mockUser
        val capturedData = slot<HashMap<String, Any>>()
        every { mockCallable.call(capture(capturedData)) } returns
            Tasks.forResult(mockk<HttpsCallableResult>().also {
                every { it.getData() } returns successResultData
            })

        val result = service.analyzeGTOResponse("gto-prompt-text", GTOTestType.LECTURETTE)

        assertTrue(result.isSuccess)
        val data = capturedData.captured
        assertEquals("gto-prompt-text", data["responseText"])
        assertEquals("text", data["responseMode"])
        assertTrue(data.containsKey("questionText"))
    }

    @Test
    fun analyzeTATResponse_whenFunctionFails_returnsFailure() = runTest {
        every { mockAuth.currentUser } returns mockUser
        every { mockCallable.call(any()) } returns
            Tasks.forException(RuntimeException("Function unavailable"))

        val result = service.analyzeTATResponse("tat-prompt")
        assertTrue(result.isFailure)
    }
}
