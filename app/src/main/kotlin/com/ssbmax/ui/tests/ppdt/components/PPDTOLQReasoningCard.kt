package com.ssbmax.ui.tests.ppdt.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.ui.components.result.OLQResultContent

@Composable
fun PPDTOLQReasoningCard(
    olqResult: OLQAnalysisResult,
    modifier: Modifier = Modifier
) {
    OLQResultContent(olqResult = olqResult, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun PPDTOLQReasoningCardPreview() {
    val sampleResult = OLQAnalysisResult(
        submissionId = "preview",
        testType = TestType.PPDT,
        olqScores = mapOf(
            OLQ.COURAGE to OLQScore(score = 7, confidence = 80, reasoning = "Hero showed initiative but lacked follow-through"),
            OLQ.DETERMINATION to OLQScore(score = 5, confidence = 90, reasoning = "Strong resolve demonstrated throughout the story"),
            OLQ.INITIATIVE to OLQScore(score = 6, confidence = 75, reasoning = "")
        ),
        overallScore = 6.0f,
        overallRating = "Good",
        strengths = emptyList(),
        weaknesses = emptyList(),
        recommendations = emptyList(),
        analyzedAt = 0L,
        aiConfidence = 82
    )
    PPDTOLQReasoningCard(olqResult = sampleResult)
}
