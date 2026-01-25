package com.ssbmax.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TimerProgressBar - A visual countdown indicator for timed tests.
 * 
 * Displays a linear progress bar that depletes as time runs out.
 * Changes color to indicate urgency when time is running low.
 * 
 * This component solves the keyboard visibility problem - when the soft keyboard
 * is open, the timer in the top bar becomes hidden. Placing this progress bar
 * below the text input area ensures users always see their remaining time.
 * 
 * Design follows TAT's existing implementation pattern.
 * 
 * @param timeRemainingSeconds Current remaining time in seconds
 * @param totalTimeSeconds Total time for this phase in seconds
 * @param lowTimeThresholdSeconds Threshold below which color turns to error (default: 60s)
 * @param height Height of the progress bar (default: 6.dp for visibility)
 * @param modifier Optional modifier for customization
 */
@Composable
fun TimerProgressBar(
    timeRemainingSeconds: Int,
    totalTimeSeconds: Int,
    lowTimeThresholdSeconds: Int = 60,
    height: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    // Calculate progress (0.0 to 1.0, where 1.0 = full time remaining)
    val progress = calculateProgress(timeRemainingSeconds, totalTimeSeconds)
    
    // Determine if we're in "low time" state
    val isLowTime = isLowTime(timeRemainingSeconds, lowTimeThresholdSeconds)
    
    // Animate color transition for smooth UX
    val progressColor by animateColorAsState(
        targetValue = if (isLowTime) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300),
        label = "TimerProgressBarColor"
    )
    
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        color = progressColor,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round
    )
}

/**
 * Calculate progress value from 0.0 to 1.0
 * Returns 0.0 if totalTimeSeconds is 0 to avoid division by zero
 */
internal fun calculateProgress(timeRemainingSeconds: Int, totalTimeSeconds: Int): Float {
    if (totalTimeSeconds <= 0) return 0f
    val progress = timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat()
    return progress.coerceIn(0f, 1f)
}

/**
 * Determine if current time is below the low time threshold
 */
internal fun isLowTime(timeRemainingSeconds: Int, lowTimeThresholdSeconds: Int): Boolean {
    return timeRemainingSeconds <= lowTimeThresholdSeconds && timeRemainingSeconds > 0
}

/**
 * Default time thresholds for different test types (in seconds)
 * Used as reference for lowTimeThresholdSeconds parameter
 */
object TimerThresholds {
    /** For short timed tests like WAT (15s per word) - warn at 5s */
    const val SHORT_TEST = 5
    
    /** For medium timed tests like SRT (30s per situation) - warn at 10s */
    const val MEDIUM_TEST = 10
    
    /** For standard tests like PPDT writing (4min) - warn at 30s */
    const val STANDARD_TEST = 30
    
    /** For long tests like GD (20min), GPE (25min) - warn at 60s */
    const val LONG_TEST = 60
}
