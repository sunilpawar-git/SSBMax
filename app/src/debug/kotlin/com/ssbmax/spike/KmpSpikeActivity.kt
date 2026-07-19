package com.ssbmax.spike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ssbmax.shared.ui.SpikeApp

/**
 * Phase 0 KMP migration spike validation harness (debug builds only).
 * Launches the shared module's ported Compose Multiplatform screen
 * (Koin -> ViewModel -> StateFlow -> CMP) to confirm it actually renders
 * on Android, not just compiles. Not part of the app's real navigation
 * graph -- launch directly via:
 *   adb shell am start -n com.ssbmax.debug/com.ssbmax.spike.KmpSpikeActivity
 * Remove once Phase 5 (real UI port) supersedes this validation need.
 */
class KmpSpikeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpikeApp()
        }
    }
}
