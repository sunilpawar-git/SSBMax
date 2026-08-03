package com.ssbmax.shared.ai

import com.ssbmax.shared.domain.model.PPDTImageContext
import com.ssbmax.shared.domain.model.TATImageContext
import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.QuestionSource
import com.ssbmax.shared.domain.util.NoOpLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates [KtorAIService] end-to-end against a MockEngine: request shape
 * (token tiers, multimodal inline_data) and response parsing, for every
 * distinct request/parser combination the interface exercises. Doesn't
 * re-test the request-shape/parsing basics already covered by
 * [KtorGeminiClientTest] and [KtorGeminiResponseParserTest] for every one of
 * the 12 methods -- only what's new here: which tier each method uses, and
 * that the multimodal image part is present/absent correctly.
 *
 * The MockEngine is wired to run on the same [UnconfinedTestDispatcher] that
 * `runTest` uses (via `HttpClient(MockEngine) { engine { dispatcher = ... } }`),
 * not the default `HttpClient(mockEngineInstance)` shape -- otherwise the
 * `withTimeout(...)` calls inside [KtorAIService] race
 * kotlinx-coroutines-test's auto-advance-when-idle behavior and fire
 * spuriously before the mocked response resolves.
 */
class KtorAIServiceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun serviceWith(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): KtorAIService {
        val httpClient = HttpClient(MockEngine) {
            engine {
                dispatcher = testDispatcher
                addHandler(handler)
            }
            install(ContentNegotiation) { json(json) }
        }
        val client = KtorGeminiClient(httpClient = httpClient, apiKey = "test-key")
        val logger = NoOpLogger()
        return KtorAIService(
            client = client,
            logger = logger,
            ppdtAnalyzer = KtorPPDTAnalyzer(client, logger),
            tatStoryAnalyzer = KtorTATStoryAnalyzer(client, logger)
        )
    }

    private fun MockRequestHandleScope.okResponse(text: String) = respond(
        content = json.encodeToString(
            GeminiGenerateContentResponse.serializer(),
            GeminiGenerateContentResponse(
                candidates = listOf(GeminiCandidate(content = GeminiContent(parts = listOf(GeminiPart(text = text)))))
            )
        ),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )

    private fun bodyTextOf(request: HttpRequestData): String = when (val body = request.body) {
        is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
        else -> body.toString()
    }

    private val question = InterviewQuestion(
        id = "q1",
        questionText = "Tell me about a time you led a team.",
        expectedOLQs = listOf(OLQ.INFLUENCE_GROUP),
        source = QuestionSource.GENERIC_POOL
    )

    // ---- Token-tier request bodies ----

    @Test
    fun `analyzeResponse uses tier-1 maxOutputTokens of 8192`() = runTest(testDispatcher) {
        var capturedBody = ""
        val service = serviceWith { request ->
            capturedBody = bodyTextOf(request)
            okResponse(
                """{"olqScores": [{"olq": "INFLUENCE_GROUP", "score": 5.0, "reasoning": "ok"}], "overallConfidence": 70, "keyInsights": []}"""
            )
        }
        service.analyzeResponse(question, "I organized the team.", "text").getOrThrow()
        assertTrue(capturedBody.contains("\"maxOutputTokens\":8192"), capturedBody)
    }

    @Test
    fun `generatePIQBasedQuestions uses tier-2 maxOutputTokens of 12288`() = runTest(testDispatcher) {
        var capturedBody = ""
        val service = serviceWith { request ->
            capturedBody = bodyTextOf(request)
            okResponse("""[{"id":"q1","questionText":"Why defense?","targetOLQs":["COURAGE"]}]""")
        }
        service.generatePIQBasedQuestions(piqData = "PIQ context", count = 1).getOrThrow()
        assertTrue(capturedBody.contains("\"maxOutputTokens\":12288"), capturedBody)
    }

    @Test
    fun `generateFeedback uses tier-3 maxOutputTokens of 16384`() = runTest(testDispatcher) {
        var capturedBody = ""
        val service = serviceWith { request ->
            capturedBody = bodyTextOf(request)
            okResponse("Great performance overall.")
        }
        val result = service.generateFeedback(
            questions = listOf(question),
            responses = listOf("I led the team."),
            olqScores = mapOf(OLQ.INFLUENCE_GROUP to 5f)
        )
        assertEquals("Great performance overall.", result.getOrThrow())
        assertTrue(capturedBody.contains("\"maxOutputTokens\":16384"), capturedBody)
    }

    // ---- Response parsing per parser type ----

    @Test
    fun `generatePIQBasedQuestions parses the question array response`() = runTest(testDispatcher) {
        val service = serviceWith { _ ->
            okResponse("""[{"id":"q1","questionText":"Why defense?","targetOLQs":["COURAGE"]}]""")
        }
        val result = service.generatePIQBasedQuestions(piqData = "PIQ context", count = 1).getOrThrow()
        assertEquals(1, result.size)
        assertEquals("Why defense?", result.first().questionText)
        assertEquals(listOf(OLQ.COURAGE), result.first().expectedOLQs)
    }

    @Test
    fun `analyzeWATResponse parses the GTO analysis object response`() = runTest(testDispatcher) {
        val service = serviceWith { _ ->
            okResponse("""{"olqScores": {"COURAGE": {"score": 6.0, "confidence": 80, "reasoning": "steady"}}}""")
        }
        val result = service.analyzeWATResponse("prompt").getOrThrow()
        assertEquals(1, result.olqScores.size)
        assertEquals(80, result.overallConfidence)
    }

    // ---- Multimodal request shape ----

    @Test
    fun `analyzePPDTMultimodal includes an inline_data part when imageBytes is non-empty`() = runTest(testDispatcher) {
        var capturedBody = ""
        val service = serviceWith { request ->
            capturedBody = bodyTextOf(request)
            okResponse("""{"olqScores": {"COURAGE": {"score": 6.0, "confidence": 80, "reasoning": "ok"}}}""")
        }
        service.analyzePPDTMultimodal(
            imageBytes = byteArrayOf(1, 2, 3, 4),
            story = "A story",
            imageContext = PPDTImageContext(),
            candidateGender = "male"
        ).getOrThrow()

        assertTrue(capturedBody.contains("\"inline_data\""), capturedBody)
        assertTrue(capturedBody.contains("\"mime_type\":\"image/jpeg\""), capturedBody)
    }

    @Test
    fun `analyzePPDTMultimodal omits inline_data and is text-only when imageBytes is empty`() = runTest(testDispatcher) {
        var capturedBody = ""
        val service = serviceWith { request ->
            capturedBody = bodyTextOf(request)
            okResponse("""{"olqScores": {"COURAGE": {"score": 6.0, "confidence": 80, "reasoning": "ok"}}}""")
        }
        service.analyzePPDTMultimodal(
            imageBytes = ByteArray(0),
            story = "A story",
            imageContext = PPDTImageContext(),
            candidateGender = "male"
        ).getOrThrow()

        assertTrue(!capturedBody.contains("\"inline_data\""), capturedBody)
    }

    @Test
    fun `analyzeTATStoryMultimodal includes an inline_data part when imageBytes is non-empty`() = runTest(testDispatcher) {
        var capturedBody = ""
        val service = serviceWith { request ->
            capturedBody = bodyTextOf(request)
            okResponse("""{"olqScores": {"COURAGE": {"score": 6.0, "confidence": 80, "reasoning": "ok"}}}""")
        }
        service.analyzeTATStoryMultimodal(
            imageBytes = byteArrayOf(5, 6, 7),
            story = "A story",
            imageContext = TATImageContext(),
            candidateGender = "female",
            storyIndex = 0,
            totalStories = 11
        ).getOrThrow()

        assertTrue(capturedBody.contains("\"inline_data\""), capturedBody)
    }

    @Test
    fun `isAvailable returns false not an exception when the call fails`() = runTest(testDispatcher) {
        val service = serviceWith { respond("boom", HttpStatusCode.InternalServerError) }
        assertEquals(false, service.isAvailable())
    }

    // ---- Ported from core:data's GeminiAIServiceTest (Phase 9.0, when KtorAIService
    // became the only AIService). Not ported: `analyzePPDTMultimodal falls back
    // gracefully when image bytes are empty`, which guarded an Android
    // GenerativeModel/Bitmap NPE on zero-length arrays -- this implementation
    // base64-encodes the raw ByteArray with no Bitmap step, and the empty-bytes
    // request shape is already asserted above.

    @Test
    fun `every request is temperature zero so identical submissions score identically`() =
        runTest(testDispatcher) {
            // WHY: SSB grading must be reproducible -- a candidate re-submitting the same
            // story must not get a different OLQ profile. Temperature drift is invisible
            // in output but breaks that guarantee.
            var capturedBody = ""
            val service = serviceWith { request ->
                capturedBody = bodyTextOf(request)
                okResponse("""{"olqScores": {"COURAGE": {"score": 6.0, "confidence": 80, "reasoning": "ok"}}}""")
            }
            service.analyzeWATResponse("prompt").getOrThrow()
            assertTrue(capturedBody.contains("\"temperature\":0.0"), capturedBody)
        }

    @Test
    fun `generateAdaptiveQuestions returns a failure Result when the call fails`() = runTest(testDispatcher) {
        // WHY: this runs mid-interview; an escaping exception would kill the session
        // rather than let the caller fall back to the generic question pool.
        val service = serviceWith { respond("boom", HttpStatusCode.InternalServerError) }

        val result = service.generateAdaptiveQuestions(
            previousQuestions = listOf(question),
            previousResponses = listOf("I led the team."),
            weakOLQs = listOf(OLQ.COURAGE),
            count = 2
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `analyzePPDTMultimodal parses all 15 OLQ scores from a full response`() = runTest(testDispatcher) {
        // WHY: PPDT grading is only valid on the complete OLQ set -- a parser that
        // silently returns a subset produces a plausible-looking but wrong assessment.
        val fullOlqJson = OLQ.entries.joinToString(",") { olq ->
            """"${olq.name}": {"score": 6.0, "confidence": 80, "reasoning": "n/a"}"""
        }
        val service = serviceWith { _ -> okResponse("""{"olqScores": {$fullOlqJson}}""") }

        val result = service.analyzePPDTMultimodal(
            imageBytes = byteArrayOf(1, 2, 3),
            story = "A story",
            imageContext = PPDTImageContext(),
            candidateGender = "male"
        ).getOrThrow()

        assertEquals(OLQ.entries.size, result.olqScores.size)
    }

    @Test
    fun `analyzePPDTMultimodal returns a failure Result when the call fails`() = runTest(testDispatcher) {
        // WHY: PPDT analysis runs in a background worker whose retry/error handling
        // depends on a failed Result; an escaping exception would crash the worker.
        val service = serviceWith { respond("boom", HttpStatusCode.InternalServerError) }

        val result = service.analyzePPDTMultimodal(
            imageBytes = byteArrayOf(1, 2, 3),
            story = "A story",
            imageContext = PPDTImageContext(),
            candidateGender = "male"
        )

        assertTrue(result.isFailure)
    }
}
