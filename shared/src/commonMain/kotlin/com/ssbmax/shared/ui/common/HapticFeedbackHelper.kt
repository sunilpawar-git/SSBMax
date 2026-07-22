package com.ssbmax.shared.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

/**
 * KMP port of `app/.../ui/tests/common/HapticFeedbackHelper.kt` — verbatim,
 * no platform-specific gotchas (`androidx.compose.ui.hapticfeedback` is
 * already Compose Multiplatform-portable). Shared by every test-type screen
 * that shows immediate answer feedback (first consumer: OIR's
 * [com.ssbmax.shared.ui.oir.components.OIRQuestionView] — TAT/WAT/SRT/etc.
 * will reuse this same object once their own verticals are ported).
 */
object HapticFeedbackHelper {

    fun performCorrectAnswerFeedback(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    suspend fun performIncorrectAnswerFeedback(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(100)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performClickFeedback(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performSubmissionFeedback(hapticFeedback: HapticFeedback) {
        performCorrectAnswerFeedback(hapticFeedback)
    }

    suspend fun performTimeWarningFeedback(hapticFeedback: HapticFeedback) {
        repeat(3) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(150)
        }
    }
}

/**
 * Automatically triggers the appropriate haptic feedback when answer
 * validation changes.
 */
@Composable
fun AnswerFeedbackEffect(
    showFeedback: Boolean,
    isCorrect: Boolean,
    hapticFeedback: HapticFeedback
) {
    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            if (isCorrect) {
                HapticFeedbackHelper.performCorrectAnswerFeedback(hapticFeedback)
            } else {
                HapticFeedbackHelper.performIncorrectAnswerFeedback(hapticFeedback)
            }
        }
    }
}
