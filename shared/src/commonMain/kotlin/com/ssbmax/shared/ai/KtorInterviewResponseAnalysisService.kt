package com.ssbmax.shared.ai

import com.ssbmax.shared.ai.prompts.SSBInterviewPrompts
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.service.InterviewResponseAnalysisService
import com.ssbmax.shared.domain.service.ResponseAnalysis
import com.ssbmax.shared.domain.util.DomainLogger

class KtorInterviewResponseAnalysisService(
    private val client: KtorGeminiClient,
    private val logger: DomainLogger
) : InterviewResponseAnalysisService {

    override suspend fun analyzeResponse(
        questionText: String,
        expectedOLQs: List<OLQ>,
        responseText: String,
        responseMode: String
    ): Result<ResponseAnalysis> {
        val prompt = SSBInterviewPrompts.buildResponseAnalysisPrompt(
            questionText = questionText,
            responseText = responseText,
            expectedOLQs = expectedOLQs,
            responseMode = responseMode
        )

        val generated = client.generateContent(prompt).getOrElse { error ->
            logger.e(TAG, "Gemini call failed for interview response analysis", error)
            return Result.failure(error)
        }

        return KtorGeminiResponseParser.parseAnalysisResponse(generated)
    }

    override suspend fun isAvailable(): Boolean =
        client.generateContent("Reply with OK").isSuccess

    private companion object {
        const val TAG = "KtorInterviewAnalysis"
    }
}
