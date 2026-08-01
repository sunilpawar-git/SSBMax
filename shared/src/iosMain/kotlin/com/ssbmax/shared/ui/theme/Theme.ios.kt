package com.ssbmax.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** No platform "dynamic color" equivalent on iOS — brand colors always apply. */
@Composable
internal actual fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme? = null
