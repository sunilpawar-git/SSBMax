package com.ssbmax.shared.domain.service

/**
 * Phase 2 scoped slice of the Android app's 15-method AIService: only the
 * single-response interview analysis path, ported end-to-end through Ktor to
 * validate the "Gemini -> Ktor" migration item. Deliberately narrower than
 * AIService (same pattern Phase 0 used for OirResultRepository vs. the app's
 * full result repository) — the other 14 AIService methods (question
 * generation, GTO/TAT/WAT/SRT/SD/PPDT multimodal analysis, feedback
 * synthesis) are real, substantial ports each and are out of this slice's
 * scope; merging this into the real AIService is later Phase 2 work.
 */
interface InterviewResponseAnalysisService {
    suspend fun analyzeResponse(
        questionText: String,
        expectedOLQs: List<com.ssbmax.shared.domain.model.interview.OLQ>,
        responseText: String,
        responseMode: String
    ): Result<ResponseAnalysis>

    suspend fun isAvailable(): Boolean
}
