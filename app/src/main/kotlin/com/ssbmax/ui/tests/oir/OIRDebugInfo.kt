package com.ssbmax.ui.tests.oir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Debug screen to diagnose OIR question cache issues
 * 
 * Shows:
 * - User authentication status
 * - Cache status (question count, batches)
 * - Firestore connectivity
 * - Last error from cache manager
 */

class OIRDebugViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val testContentRepository: TestContentRepository
) : ViewModel() {

    private val _debugInfo = MutableStateFlow<DebugInfo>(DebugInfo())
    val debugInfo: StateFlow<DebugInfo> = _debugInfo.asStateFlow()

    init {
        loadDebugInfo()
    }

    fun loadDebugInfo() {
        viewModelScope.launch {
            try {
                // Collect current user state
                observeCurrentUser().collect { user ->
                    val cacheStatus = testContentRepository.getOIRCacheStatus()

                    _debugInfo.value = DebugInfo(
                        isAuthenticated = user != null,
                        userId = user?.id ?: "Not logged in",
                        userEmail = user?.email ?: "N/A",
                        cachedQuestions = cacheStatus.cachedQuestions,
                        batchesDownloaded = cacheStatus.batchesDownloaded,
                        verbalCount = cacheStatus.verbalCount,
                        nonVerbalCount = cacheStatus.nonVerbalCount,
                        numericalCount = cacheStatus.numericalCount,
                        spatialCount = cacheStatus.spatialCount,
                        lastSyncTime = cacheStatus.lastSyncTime,
                        error = null
                    )

                    // Try to get questions
                    val result = testContentRepository.getOIRTestQuestions(50)
                    if (result.isFailure) {
                        _debugInfo.update { it.copy(
                            error = result.exceptionOrNull()?.message ?: "Unknown error"
                        ) }
                    } else {
                        val questions = result.getOrNull() ?: emptyList()
                        _debugInfo.update { it.copy(
                            questionsRetrieved = questions.size,
                            usingMockData = questions.any { it.id.contains("mock") }
                        ) }
                    }
                }
            } catch (e: Exception) {
                _debugInfo.update { it.copy(
                    error = e.message ?: "Unknown error",
                    isAuthenticated = false
                ) }
            }
        }
    }
}

data class DebugInfo(
    val isAuthenticated: Boolean = false,
    val userId: String = "",
    val userEmail: String = "",
    val cachedQuestions: Int = 0,
    val batchesDownloaded: Int = 0,
    val verbalCount: Int = 0,
    val nonVerbalCount: Int = 0,
    val numericalCount: Int = 0,
    val spatialCount: Int = 0,
    val lastSyncTime: Long? = null,
    val questionsRetrieved: Int = 0,
    val usingMockData: Boolean = false,
    val error: String? = null
)

@Composable
fun OIRDebugInfoScreen(
    viewModel: OIRDebugViewModel = koinViewModel(),
    onClose: () -> Unit
) {
    val debugInfo by viewModel.debugInfo.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("OIR Cache Debug Info", style = MaterialTheme.typography.headlineMedium)
        
        HorizontalDivider()
        
        // Authentication
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Authentication", style = MaterialTheme.typography.titleMedium)
                Text("Status: ${if (debugInfo.isAuthenticated) "✅ Logged In" else "❌ Not Logged In"}")
                Text("User ID: ${debugInfo.userId}")
                Text("Email: ${debugInfo.userEmail}")
            }
        }
        
        // Cache Status
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cache Status", style = MaterialTheme.typography.titleMedium)
                Text("Total Questions: ${debugInfo.cachedQuestions}")
                Text("Batches Downloaded: ${debugInfo.batchesDownloaded}")
                Text("Verbal: ${debugInfo.verbalCount}")
                Text("Non-Verbal: ${debugInfo.nonVerbalCount}")
                Text("Numerical: ${debugInfo.numericalCount}")
                Text("Spatial: ${debugInfo.spatialCount}")
                val syncTime = debugInfo.lastSyncTime
                Text("Last Sync: ${if (syncTime != null) 
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(syncTime))
                    else "Never"}")
            }
        }
        
        // Question Retrieval
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Question Retrieval", style = MaterialTheme.typography.titleMedium)
                Text("Questions Retrieved: ${debugInfo.questionsRetrieved}")
                Text("Using Mock Data: ${if (debugInfo.usingMockData) "⚠️ YES (PROBLEM!)" else "✅ No"}")
            }
        }
        
        // Error
        val errorMsg = debugInfo.error
        if (errorMsg != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Error", style = MaterialTheme.typography.titleMedium, 
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.loadDebugInfo() }) {
                Text("Refresh")
            }
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}

