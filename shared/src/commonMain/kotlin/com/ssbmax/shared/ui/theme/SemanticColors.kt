package com.ssbmax.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * UI status colors with stable meaning across features and platforms.
 *
 * Each role has a paired Material color rather than relying on hue alone:
 * consumers must also provide text, icon, or state semantics as appropriate.
 */
data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val error: Color,
    val onError: Color,
    val warning: Color,
    val onWarning: Color,
    val informational: Color,
    val onInformational: Color,
    val selected: Color,
    val onSelected: Color,
    val disabled: Color,
    val onDisabled: Color,
    val skipped: Color,
    val onSkipped: Color,
    val testProgress: Color,
    val onTestProgress: Color
)

/** Maps semantic roles to the active Material theme, keeping business meaning centralized. */
fun ColorScheme.toSemanticColors(): SemanticColors = SemanticColors(
    success = primary,
    onSuccess = onPrimary,
    error = error,
    onError = onError,
    warning = tertiary,
    onWarning = onTertiary,
    informational = secondary,
    onInformational = onSecondary,
    selected = primaryContainer,
    onSelected = onPrimaryContainer,
    disabled = surfaceVariant,
    onDisabled = onSurfaceVariant,
    skipped = surfaceVariant,
    onSkipped = onSurfaceVariant,
    testProgress = secondary,
    onTestProgress = onSecondary
)

val LocalSemanticColors = staticCompositionLocalOf<SemanticColors> {
    error("No SemanticColors provided")
}

val MaterialTheme.semanticColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current
