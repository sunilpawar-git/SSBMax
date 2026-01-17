package com.ssbmax.core.designsystem.theme

import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests to verify Spacing object provides all required spacing values
 * Part of Phase 5: Create Spacing Object
 */
class SpacingTest {

    @Test
    fun `Spacing object provides base spacing values`() {
        assertEquals("extraSmall should be 4dp", 4.dp, Spacing.extraSmall)
        assertEquals("small should be 8dp", 8.dp, Spacing.small)
        assertEquals("medium should be 12dp", 12.dp, Spacing.medium)
        assertEquals("large should be 16dp", 16.dp, Spacing.large)
        assertEquals("extraLarge should be 20dp", 20.dp, Spacing.extraLarge)
    }

    @Test
    fun `Spacing provides component-specific values`() {
        assertEquals("cardPadding should be 16dp", 16.dp, Spacing.cardPadding)
        assertEquals("cardCornerRadius should be 12dp", 12.dp, Spacing.cardCornerRadius)
        assertEquals("cardCornerRadiusLarge should be 16dp", 16.dp, Spacing.cardCornerRadiusLarge)
    }

    @Test
    fun `Spacing provides icon sizes`() {
        assertEquals("iconSizeExtraSmall should be 16dp", 16.dp, Spacing.iconSizeExtraSmall)
        assertEquals("iconSizeSmall should be 18dp", 18.dp, Spacing.iconSizeSmall)
        assertEquals("iconSize should be 24dp", 24.dp, Spacing.iconSize)
        assertEquals("iconSizeLarge should be 28dp", 28.dp, Spacing.iconSizeLarge)
    }

    @Test
    fun `Spacing provides card heights`() {
        assertEquals("statsCardHeight should be 84dp", 84.dp, Spacing.statsCardHeight)
        assertEquals("phaseCardHeight should be 280dp", 280.dp, Spacing.phaseCardHeight)
    }

    @Test
    fun `Spacing provides section spacing`() {
        assertEquals("sectionSpacing should be 20dp", 20.dp, Spacing.sectionSpacing)
    }

    @Test
    fun `Spacing values follow 4dp grid system`() {
        val spacingValues = listOf(
            Spacing.extraSmall,
            Spacing.small,
            Spacing.medium,
            Spacing.large,
            Spacing.extraLarge,
            Spacing.cardPadding
        )

        spacingValues.forEach { spacing ->
            val value = spacing.value.toInt()
            assertEquals(
                "All spacing should be 4dp multiples, but got ${spacing}",
                0,
                value % 4
            )
        }
    }

    @Test
    fun `Spacing provides icon button size`() {
        assertEquals("iconButtonSize should be 40dp", 40.dp, Spacing.iconButtonSize)
    }
}
