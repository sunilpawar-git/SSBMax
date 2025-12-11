package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.ssbmax.core.domain.model.interview.OLQ
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.every
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiAIServiceTest {

    @Test
    fun generateAdaptiveQuestions_returnsFailure_whenModelThrows() = runTest {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val mockModel = mockk<GenerativeModel>()
        coEvery { mockModel.generateContent(any<String>()) } throws IllegalStateException("boom")

        val service = GeminiAIService(apiKey = "key")
        GeminiAIService::class.java.getDeclaredField("model\$delegate").apply {
            isAccessible = true
            set(service, lazy { mockModel })
        }

        val result = service.generateAdaptiveQuestions(
            previousQuestions = emptyList(),
            previousResponses = emptyList(),
            weakOLQs = listOf(OLQ.EFFECTIVE_INTELLIGENCE),
            count = 1
        )

        assertTrue(result.isFailure)
    }
}
