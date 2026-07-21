package com.ssbmax.shared.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SSBMax Color Palette — Phase 5 KMP port of
 * `core/designsystem/.../theme/Color.kt`'s `SSBColors` object.
 *
 * Ported verbatim rather than depending on `:core:designsystem` from
 * `:shared` — that module is Android-only (not a KMP target) and porting it
 * wholesale is out of this session's scope (home-screen vertical only). Only
 * the two objects the home screens actually reference (`SSBColors`,
 * `Spacing`) are duplicated here; both are pure `androidx.compose.ui`/
 * `Color`/`Dp` values with zero Android dependency, so the duplication is a
 * straight copy, not a re-implementation. If/when `:core:designsystem` is
 * ported wholesale in a future phase, the Android `app` module's own copy
 * should be re-pointed at this one (single source of truth) rather than
 * keeping both — tracked as a future consolidation, not attempted here per
 * "surgical changes" (CLAUDE.md Rule 3).
 */
object SSBColors {
    // Primary Colors - Navy Blue (Military Professionalism)
    val NavyBlue = Color(0xFF1B3A4B)
    val NavyBlueDark = Color(0xFF0D1F2D)
    val NavyBlueLight = Color(0xFF2C5F77)

    // Secondary Colors - Olive Green (Army Heritage)
    val OliveGreen = Color(0xFF6B8E23)
    val OliveGreenDark = Color(0xFF556B1C)
    val OliveGreenLight = Color(0xFF8DB040)

    // Accent - Military Gold (Excellence & Achievement)
    val MilitaryGold = Color(0xFFD4AF37)
    val MilitaryGoldDark = Color(0xFFAA8C2C)
    val MilitaryGoldLight = Color(0xFFE5C758)

    // Neutral - Military Gray
    val MilitaryGray = Color(0xFF708090)
    val MilitaryGrayLight = Color(0xFFA0B0C0)
    val MilitaryGrayDark = Color(0xFF505F70)

    // Surfaces
    val SurfaceLight = Color(0xFFFAFBFC)
    val SurfaceDark = Color(0xFF121212)

    // Functional Colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
}
