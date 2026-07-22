package com.ssbmax.shared.ui.ppdt.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.ui.components.result.OLQResultContent

/**
 * KMP port of `app/.../ui/tests/ppdt/components/PPDTOLQReasoningCard.kt`.
 * Thin wrapper, unchanged behavior.
 */
@Composable
fun PPDTOLQReasoningCard(
    olqResult: OLQAnalysisResult,
    modifier: Modifier = Modifier
) {
    OLQResultContent(olqResult = olqResult, modifier = modifier)
}
