package com.ssbmax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.ui.SSBMaxApp
import com.ssbmax.ui.theme.LocalThemeState
import com.ssbmax.ui.theme.SSBMaxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeState = mainViewModel.themeState
            
            CompositionLocalProvider(LocalThemeState provides themeState) {
                SSBMaxTheme(appTheme = themeState.currentTheme) {
                    SSBMaxApp()
                }
            }
        }
    }
}

