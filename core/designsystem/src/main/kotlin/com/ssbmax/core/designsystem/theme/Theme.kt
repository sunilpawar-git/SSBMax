package com.ssbmax.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * SSBMax Dark Color Scheme
 * Professional dark theme for focused study sessions
 */
private val DarkColorScheme = darkColorScheme(
    primary = SSBColors.MilitaryGold,
    onPrimary = SSBColors.NavyBlueDark,
    primaryContainer = SSBColors.MilitaryGoldDark,
    onPrimaryContainer = SSBColors.NavyBlue,
    
    secondary = SSBColors.OliveGreen,
    onSecondary = SSBColors.SurfaceLight,
    secondaryContainer = SSBColors.OliveGreenDark,
    onSecondaryContainer = SSBColors.OliveGreenLight,
    
    tertiary = SSBColors.NavyBlueLight,
    onTertiary = SSBColors.SurfaceLight,
    
    background = SSBColors.SurfaceDark,
    onBackground = SSBColors.SurfaceLight,
    
    surface = SSBColors.NavyBlueDark,
    onSurface = SSBColors.SurfaceLight,
    surfaceVariant = SSBColors.MilitaryGrayDark,
    onSurfaceVariant = SSBColors.MilitaryGrayLight,
    
    error = SSBColors.Error
)

/**
 * SSBMax Light Color Scheme
 * Clean, professional light theme for daytime use
 */
private val LightColorScheme = lightColorScheme(
    primary = SSBColors.NavyBlue,
    onPrimary = SSBColors.SurfaceLight,
    primaryContainer = SSBColors.NavyBlueLight,
    onPrimaryContainer = SSBColors.NavyBlueDark,
    
    secondary = SSBColors.OliveGreen,
    onSecondary = SSBColors.SurfaceLight,
    secondaryContainer = SSBColors.OliveGreenLight,
    onSecondaryContainer = SSBColors.OliveGreenDark,
    
    tertiary = SSBColors.MilitaryGold,
    onTertiary = SSBColors.NavyBlueDark,
    
    background = SSBColors.SurfaceLight,
    onBackground = SSBColors.NavyBlueDark,
    
    surface = SSBColors.SurfaceLight,
    onSurface = SSBColors.NavyBlueDark,
    surfaceVariant = SSBColors.MilitaryGrayLight,
    onSurfaceVariant = SSBColors.MilitaryGrayDark,
    
    error = SSBColors.Error
)

/**
 * SSBMax Theme
 * Main theme composable for the entire app
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic colors (Android 12+)
 * @param content The content to theme
 */
@Composable
fun SSBMaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to maintain brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SSBTypography,
        content = content
    )
}

