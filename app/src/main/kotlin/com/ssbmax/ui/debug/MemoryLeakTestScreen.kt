package com.ssbmax.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssbmax.core.data.util.LeakVerificationHelper
import com.ssbmax.core.data.util.MemoryLeakTracker

@OptIn(ExperimentalMaterial3Api::class)

/**
 * Debug screen for memory leak verification
 *
 * ONLY AVAILABLE IN DEBUG BUILDS
 *
 * Use this screen to:
 * 1. Run controlled leak test scenarios
 * 2. Monitor memory usage with Android Profiler
 * 3. Verify that ViewModels are properly cleaned up
 *
 * To access: Add to navigation graph in debug builds only
 */
@Composable
fun MemoryLeakTestScreen(
    viewModel: MemoryLeakTestViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Leak Verification") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🧪 Memory Leak Verification",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Use Android Profiler to verify memory leaks are prevented. Run test scenarios, then rotate device or navigate away to trigger cleanup.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Memory Stats
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Memory Statistics", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Active ViewModels: ${uiState.activeViewModels}")
                    Text("Active Jobs: ${uiState.activeJobs}")
                    Text("Memory Usage: ${uiState.memoryUsage}")
                    Text("Leak Status: ${if (uiState.hasLeaks) "🚨 LEAKS DETECTED" else "✅ No leaks"}")
                }
            }

            // Test Scenarios
            Text("Test Scenarios", style = MaterialTheme.typography.titleMedium)

            TestScenarioButton(
                title = "TAT Timer Leak Test",
                description = "Simulates TAT test timers (30s per image)",
                onClick = { viewModel.runLeakTest("tat-timer-leak") }
            )

            TestScenarioButton(
                title = "WAT Timer Leak Test",
                description = "Simulates WAT test word timers (15s per word)",
                onClick = { viewModel.runLeakTest("wat-timer-leak") }
            )

            TestScenarioButton(
                title = "Coroutine Leak Test",
                description = "Tests background coroutine cleanup",
                onClick = { viewModel.runLeakTest("coroutine-leak") }
            )

            TestScenarioButton(
                title = "Firebase Listener Leak Test",
                description = "Simulates Firestore listener cleanup",
                onClick = { viewModel.runLeakTest("firebase-listener-leak") }
            )

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.forceMemoryPressure() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Force GC")
                }

                OutlinedButton(
                    onClick = { viewModel.checkForLeaks() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Check Leaks")
                }
            }

            Button(
                onClick = { viewModel.generateReport() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Report")
            }

            // Instructions
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 Instructions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Start Android Profiler memory monitoring")
                    Text("2. Run a test scenario above")
                    Text("3. Rotate device (portrait ↔ landscape)")
                    Text("4. Check profiler for retained objects")
                    Text("5. Use 'Force GC' to trigger cleanup")
                    Text("6. Verify no SSBMax ViewModels are leaked")
                }
            }

            // Logs
            if (uiState.logs.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📝 Recent Logs", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.logs.takeLast(10).forEach { log ->
                            Text(log, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestScenarioButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
