package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.ssbmax.shared.domain.model.PPDTImageContext
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.service.OLQScoreWithReasoning
import com.ssbmax.shared.domain.service.ResponseAnalysis
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GeminiAIServiceTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `GeminiAIService temperature constant is zero for deterministic scoring`() {
        // Temperature 0 ensures identical scores for identical stories
        assertEquals(0.0f, GeminiAIService.TEMPERATURE)
    }

    @Test
    fun generateAdaptiveQuestions_returnsFailure_whenModelThrows() = runTest {
        val mockModel = mockk<GenerativeModel>()
        coEvery { mockModel.generateContent(any<String>()) } throws IllegalStateException("boom")

        val service = GeminiAIService(apiKey = "key")
        injectMockModel(service, mockModel)

        val result = service.generateAdaptiveQuestions(
            previousQuestions = emptyList(),
            previousResponses = emptyList(),
            weakOLQs = listOf(OLQ.EFFECTIVE_INTELLIGENCE),
            count = 1
        )

        assertTrue(result.isFailure)
    }

    // --- Phase 7: analyzePPDTMultimodal tests ---

    @Test
    fun `analyzePPDTMultimodal returns success when model returns valid 15-OLQ JSON`() = runTest {
        // WHY: Verifies GeminiAIService delegates to ppdtAnalyzer and the success path flows through
        val fakeAnalysis = buildFakeResponseAnalysis()
        val mockAnalyzer = mockk<GeminiPPDTAnalyzer>()
        coEvery {
            mockAnalyzer.analyzePPDTMultimodal(any(), any(), any(), any())
        } returns Result.success(fakeAnalysis)

        val service = GeminiAIService(apiKey = "test-key")
        injectMockPPDTAnalyzer(service, mockAnalyzer)

        val result = service.analyzePPDTMultimodal(
            imageBytes = ByteArray(100),
            story = "A brave officer helped villagers cross the flooded river.",
            imageContext = PPDTImageContext(sceneDescription = "River rescue scene"),
            candidateGender = "Male"
        )

        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!
        assertEquals(15, analysis.olqScores.size)
    }

    @Test
    fun `analyzePPDTMultimodal returns failure when model throws`() = runTest {
        // WHY: Multimodal failures must propagate cleanly to worker retry logic
        val mockModel = mockk<GenerativeModel>()
        coEvery { mockModel.generateContent(any<Content>()) } throws IOException("connection timeout")

        val service = GeminiAIService(apiKey = "test-key")
        injectMockModel(service, mockModel)

        val result = service.analyzePPDTMultimodal(
            imageBytes = ByteArray(0),
            story = "",
            imageContext = PPDTImageContext(),
            candidateGender = ""
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `analyzePPDTMultimodal uses Content overload not plain String overload`() = runTest {
        // WHY: Verifies the multimodal code path is called (Content, not String)
        val capturedContent = slot<Content>()
        val mockModel = mockk<GenerativeModel>()
        coEvery { mockModel.generateContent(capture(capturedContent)) } throws IOException("stop early")

        val service = GeminiAIService(apiKey = "test-key")
        injectMockModel(service, mockModel)

        service.analyzePPDTMultimodal(
            imageBytes = ByteArray(10),
            story = "Officer leads team.",
            imageContext = PPDTImageContext(),
            candidateGender = "Male"
        )

        // Verify a Content object was captured (multimodal overload was called)
        assertNotNull(capturedContent.captured)
        assertTrue(capturedContent.isCaptured)
    }

    @Test
    fun `analyzePPDTMultimodal falls back gracefully when image bytes are empty`() = runTest {
        // WHY: Storage download could return 0 bytes — must not throw NPE, must degrade cleanly
        val mockModel = mockk<GenerativeModel>()
        coEvery { mockModel.generateContent(any<Content>()) } throws IOException("network error")

        val service = GeminiAIService(apiKey = "test-key")
        injectMockModel(service, mockModel)

        val result = service.analyzePPDTMultimodal(
            imageBytes = ByteArray(0),
            story = "A story about courage.",
            imageContext = PPDTImageContext(),
            candidateGender = "Female"
        )
        // Clean failure — no NPE or uncaught exception
        assertTrue(result.isFailure)
    }

    // --- Helpers ---

    private fun injectMockModel(service: GeminiAIService, mockModel: GenerativeModel) {
        GeminiAIService::class.java.getDeclaredField("model\$delegate").apply {
            isAccessible = true
            set(service, lazy { mockModel })
        }
    }

    private fun injectMockPPDTAnalyzer(service: GeminiAIService, analyzer: GeminiPPDTAnalyzer) {
        GeminiAIService::class.java.getDeclaredField("ppdtAnalyzer\$delegate").apply {
            isAccessible = true
            set(service, lazy { analyzer })
        }
    }

    private fun buildFakeResponseAnalysis(): ResponseAnalysis {
        val olqScores = OLQ.entries.associate { olq ->
            olq to OLQScoreWithReasoning(olq = olq, score = 6.0f, reasoning = "Test", evidence = emptyList())
        }
        return ResponseAnalysis(olqScores = olqScores, overallConfidence = 75, keyInsights = emptyList())
    }

}
