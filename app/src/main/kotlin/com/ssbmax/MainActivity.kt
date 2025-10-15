package com.ssbmax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.ssbmax.core.designsystem.theme.SSBMaxTheme
import com.ssbmax.navigation.SSBMaxNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SSBMaxTheme {
                val navController = rememberNavController()
                SSBMaxNavGraph(navController = navController)
            }
        }
    }
}

