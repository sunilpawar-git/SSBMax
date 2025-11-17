package com.ssbmax.ui.tests.oir

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug screen to diagnose OIR question cache issues
 * 
 * Shows:
 * - User authentication status
 * - Cache status (question count, batches)
 * - Firestore connectivity
 * - Last error from cache manager
 */

@HiltViewModel
class OIRDebugViewModel @Inject constructor(
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
                        _debugInfo.value = _debugInfo.value.copy(
                            error = result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    } else {
                        val questions = result.getOrNull() ?: emptyList()
                        _debugInfo.value = _debugInfo.value.copy(
                            questionsRetrieved = questions.size,
                            usingMockData = questions.any { it.id.contains("mock") }
                        )
                    }
                }
            } catch (e: Exception) {
                _debugInfo.value = _debugInfo.value.copy(
                    error = e.message ?: "Unknown error",
                    isAuthenticated = false
                )
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
    viewModel: OIRDebugViewModel = hiltViewModel(),
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
        
        Divider()
        
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

