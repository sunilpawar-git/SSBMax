package com.ssbmax.core.designsystem

import androidx.compose.ui.graphics.Color
import com.ssbmax.core.designsystem.theme.SSBColors
import org.junit.Test
import org.junit.Assert.assertTrue
import kotlin.math.pow

/**
 * Dark Theme Contrast Ratio Test
 *
 * Ensures all Material3 color slots in dark theme meet WCAG AA accessibility standards.
 * Minimum contrast ratio: 4.5:1 for normal text, 3:1 for large text.
 *
 * WHY: Dark theme text was subdued/unreadable because container colors and their text colors
 * lacked sufficient contrast. This test prevents that regression.
 *
 * Reference: https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html
 */
class DarkThemeContrastTest {
    
    /**
     * Calculate relative luminance of a color (per WCAG spec)
     * https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html#dfn-relative-luminance
     */
    private fun getRelativeLuminance(color: Color): Double {
        val r = color.red.let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        val g = color.green.let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        val b = color.blue.let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    
    /**
     * Calculate contrast ratio between two colors
     * https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html#dfn-contrast-ratio
     */
    private fun getContrastRatio(foreground: Color, background: Color): Double {
        val l1 = getRelativeLuminance(foreground)
        val l2 = getRelativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Verify contrast ratio meets WCAG AA standard
     */
    private fun assertContrastRatioOK(
        foreground: Color,
        background: Color,
        minRatio: Double = 4.5,
        label: String = ""
    ) {
        val ratio = getContrastRatio(foreground, background)
        assertTrue(
            "Contrast ratio $ratio for '$label' is below WCAG AA minimum $minRatio:1. " +
            "Foreground: $foreground, Background: $background",
            ratio >= minRatio
        )
    }
    
    @Test
    fun `dark theme primaryContainer has sufficient contrast with onPrimaryContainer`() {
        // WHY: TAT Results card and other primary cards use these colors
        // Regression: MilitaryGoldDark (#AA8C2C) + NavyBlue (#1B3A4B) failed this
        assertContrastRatioOK(
            foreground = SSBColors.NavyBlueDark,  // onPrimaryContainer (text)
            background = SSBColors.MilitaryGoldLight,  // primaryContainer (card background)
            minRatio = 4.5,
            label = "primaryContainer / onPrimaryContainer"
        )
    }
    
    @Test
    fun `dark theme secondaryContainer has sufficient contrast with onSecondaryContainer`() {
        // WHY: TAT Writing Phase and other secondary cards use these colors
        assertContrastRatioOK(
            foreground = SSBColors.NavyBlueDark,  // onSecondaryContainer (text)
            background = SSBColors.OliveGreenLight,  // secondaryContainer (card background)
            minRatio = 4.5,
            label = "secondaryContainer / onSecondaryContainer"
        )
    }
    
    @Test
    fun `dark theme secondary has sufficient contrast with onSecondary`() {
        // WHY: Secondary colored backgrounds must have readable text
        assertContrastRatioOK(
            foreground = SSBColors.NavyBlueDark,  // onSecondary (text)
            background = SSBColors.OliveGreenLight,  // secondary (background)
            minRatio = 4.5,
            label = "secondary / onSecondary"
        )
    }
    
    @Test
    fun `dark theme surface has sufficient contrast with onSurface`() {
        // WHY: Default text on surfaces must be readable
        assertContrastRatioOK(
            foreground = SSBColors.SurfaceLight,  // onSurface (text)
            background = SSBColors.NavyBlueDark,  // surface (background)
            minRatio = 4.5,
            label = "surface / onSurface"
        )
    }
    
    @Test
    fun `dark theme background has sufficient contrast with onBackground`() {
        // WHY: Page backgrounds must have readable text
        assertContrastRatioOK(
            foreground = SSBColors.SurfaceLight,  // onBackground (text)
            background = SSBColors.SurfaceDark,  // background (page background)
            minRatio = 4.5,
            label = "background / onBackground"
        )
    }
    
    @Test
    fun `dark theme primary has sufficient contrast with onPrimary`() {
        // WHY: Primary action buttons and highlights
        assertContrastRatioOK(
            foreground = SSBColors.NavyBlueDark,  // onPrimary (text)
            background = SSBColors.MilitaryGold,  // primary (background)
            minRatio = 4.5,
            label = "primary / onPrimary"
        )
    }
    
    @Test
    fun `gold colors are distinct and visible in dark theme`() {
        // WHY: Military gold should not be too dark in dark theme
        val goldLuminance = getRelativeLuminance(SSBColors.MilitaryGold)
        val goldLightLuminance = getRelativeLuminance(SSBColors.MilitaryGoldLight)
        val darkSurfaceLuminance = getRelativeLuminance(SSBColors.SurfaceDark)
        
        // Gold should be noticeably lighter than dark surface
        assertTrue(
            "MilitaryGold should be lighter than dark surface background",
            goldLuminance > darkSurfaceLuminance * 1.5
        )
        
        // Gold light should be even brighter
        assertTrue(
            "MilitaryGoldLight should be brighter than MilitaryGold",
            goldLightLuminance > goldLuminance
        )
    }
    
    @Test
    fun `olive green is properly contrasted in dark theme`() {
        // WHY: Secondary olive color should not be too dark
        // Olive light should contrast well with dark surface
        assertTrue(
            "OliveGreenLight should have sufficient contrast with dark surface",
            getContrastRatio(SSBColors.OliveGreenLight, SSBColors.SurfaceDark) >= 3.0
        )
    }
    
    @Test
    fun `text colors in dark theme are sufficiently light`() {
        // WHY: Text must be bright enough for readability
        val surfaceLightLuminance = getRelativeLuminance(SSBColors.SurfaceLight)
        val darkSurfaceLuminance = getRelativeLuminance(SSBColors.SurfaceDark)
        val navyBlueDarkLuminance = getRelativeLuminance(SSBColors.NavyBlueDark)
        
        // SurfaceLight is the primary text color in dark theme
        assertTrue(
            "SurfaceLight should be very bright for text",
            surfaceLightLuminance > 0.8
        )
        
        // SurfaceDark should be very dark for backgrounds
        assertTrue(
            "SurfaceDark should be very dark for background",
            darkSurfaceLuminance < 0.1
        )
        
        // NavyBlueDark should be dark but not black (for secondary text)
        assertTrue(
            "NavyBlueDark should be darker than medium gray",
            navyBlueDarkLuminance < 0.3
        )
    }
    
    @Test
    fun `text on backgrounds in dark theme matches colorscheme`() {
        // WHY: Verify the actual colors used in DarkColorScheme are readable
        // This test only validates pairs that are actually used together in the colorscheme
        
        // primaryContainer + onPrimaryContainer (used together)
        val primaryRatio = getContrastRatio(SSBColors.NavyBlueDark, SSBColors.MilitaryGoldLight)
        assertTrue("primaryContainer + onPrimaryContainer should have ratio >= 4.5", primaryRatio >= 4.5)
        
        // secondaryContainer + onSecondaryContainer (used together)
        val secondaryRatio = getContrastRatio(SSBColors.NavyBlueDark, SSBColors.OliveGreenLight)
        assertTrue("secondaryContainer + onSecondaryContainer should have ratio >= 4.5", secondaryRatio >= 4.5)
        
        // surface + onSurface (used together)
        val surfaceRatio = getContrastRatio(SSBColors.SurfaceLight, SSBColors.NavyBlueDark)
        assertTrue("surface + onSurface should have ratio >= 4.5", surfaceRatio >= 4.5)
    }
}
