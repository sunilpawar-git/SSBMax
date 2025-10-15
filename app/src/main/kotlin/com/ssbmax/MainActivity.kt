package com.ssbmax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ssbmax.core.designsystem.theme.SSBMaxTheme
import com.ssbmax.ui.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SSBMaxTheme {
                DashboardScreen(
                    onNavigateToTest = { testId ->
                        // TODO: Navigate to test screen
                    },
                    onNavigateToStudy = {
                        // TODO: Navigate to study materials
                    },
                    onNavigateToProgress = {
                        // TODO: Navigate to progress tracker
                    }
                )
            }
        }
    }
}

