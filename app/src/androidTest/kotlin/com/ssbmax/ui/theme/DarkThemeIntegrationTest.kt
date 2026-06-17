package com.ssbmax.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ssbmax.core.designsystem.theme.SSBMaxTheme
import com.ssbmax.core.designsystem.theme.SSBColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Dark Theme Integration Test
 *
 * Tests that the dark theme color scheme is applied correctly to Material3 components
 * and that text remains readable in dark mode.
 *
 * WHY: Ensures dark theme is actually being used and colors are correctly mapped to
 * Material3 color slots (primaryContainer, onPrimaryContainer, etc.)
 */
@RunWith(AndroidJUnit4::class)
class DarkThemeIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dark_theme_applies_to_material_colors() {
        // WHY: Verify the dark color scheme is actually applied to Material3
        composeTestRule.setContent {
            var isDarkTheme by mutableStateOf(true)
            
            SSBMaxTheme(darkTheme = isDarkTheme) {
                // Verify primaryContainer color is the lighter gold (not the dark one)
                val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
                
                // In dark theme, primaryContainer should be MilitaryGoldLight
                assert(primaryContainer == SSBColors.MilitaryGoldLight) {
                    "Dark theme primaryContainer should be MilitaryGoldLight, got $primaryContainer"
                }
                
                // onPrimaryContainer should be dark for text contrast
                assert(onPrimaryContainer == SSBColors.NavyBlueDark) {
                    "Dark theme onPrimaryContainer should be NavyBlueDark, got $onPrimaryContainer"
                }
            }
        }
        
        // If this test passes without crashing, colors are correctly applied
        composeTestRule.mainClock.advanceTimeBy(100)
    }
    
    @Test
    fun secondary_colors_have_proper_contrast_in_dark_theme() {
        // WHY: Secondary container colors must also be readable
        composeTestRule.setContent {
            SSBMaxTheme(darkTheme = true) {
                val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
                val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
                
                // Verify secondary colors are correctly mapped
                assert(secondaryContainer == SSBColors.OliveGreen) {
                    "Dark theme secondaryContainer should be OliveGreen"
                }
                
                // Secondary text should contrast well
                assert(onSecondaryContainer == SSBColors.SurfaceLight) {
                    "Dark theme onSecondaryContainer should be bright for readability"
                }
            }
        }
        
        composeTestRule.mainClock.advanceTimeBy(100)
    }
    
    @Test
    fun light_theme_still_works_correctly() {
        // WHY: Ensure light theme wasn't broken by dark theme changes
        composeTestRule.setContent {
            SSBMaxTheme(darkTheme = false) {
                val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
                
                // Light theme should use light colors for backgrounds
                assert(primaryContainer == SSBColors.NavyBlueLight) {
                    "Light theme primaryContainer should be NavyBlueLight"
                }
                
                // Light theme text should be dark
                assert(onPrimaryContainer == SSBColors.NavyBlueDark) {
                    "Light theme onPrimaryContainer should be dark for contrast"
                }
            }
        }
        
        composeTestRule.mainClock.advanceTimeBy(100)
    }
}
