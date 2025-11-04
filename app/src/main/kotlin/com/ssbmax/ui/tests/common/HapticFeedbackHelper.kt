package com.ssbmax.ui.tests.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

/**
 * Haptic Feedback Helper for SSB Tests
 * 
 * Provides consistent tactile feedback patterns across all test types
 * to enhance user experience and provide immediate non-visual feedback.
 */
object HapticFeedbackHelper {
    
    /**
     * Triggers haptic feedback for a correct answer
     * Pattern: Single long press (success vibration)
     */
    fun performCorrectAnswerFeedback(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    /**
     * Triggers haptic feedback for an incorrect answer
     * Pattern: Double tap (error vibration)
     */
    suspend fun performIncorrectAnswerFeedback(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(100) // 100ms gap between vibrations
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    /**
     * Triggers haptic feedback for a button click
     * Pattern: Light tap
     */
    fun performClickFeedback(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    /**
     * Triggers haptic feedback for test submission
     * Pattern: Success pattern (same as correct answer)
     */
    fun performSubmissionFeedback(hapticFeedback: HapticFeedback) {
        performCorrectAnswerFeedback(hapticFeedback)
    }
    
    /**
     * Triggers haptic feedback for time warning (e.g., 5 minutes remaining)
     * Pattern: Triple tap
     */
    suspend fun performTimeWarningFeedback(hapticFeedback: HapticFeedback) {
        repeat(3) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(150)
        }
    }
}

/**
 * Composable wrapper for answer feedback
 * Automatically triggers appropriate haptic feedback when answer validation changes
 * 
 * @param showFeedback Whether feedback is currently being shown
 * @param isCorrect Whether the answer is correct
 * @param hapticFeedback The HapticFeedback instance from LocalHapticFeedback.current
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


