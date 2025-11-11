package com.ssbmax.ui.topic

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.repository.ContentSource
import com.ssbmax.core.data.repository.TopicContentData
import com.ssbmax.core.domain.config.ContentFeatureFlags
import com.ssbmax.core.domain.model.StudyMaterial
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.StudyContentRepository
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Topic Screen
 * Manages topic information, study materials, and tests for a specific SSB topic
 * 
 * NOW SUPPORTS CLOUD CONTENT!
 * - Loads from Firestore when enabled
 * - Automatically falls back to local on error
 * - Gradual per-topic rollout
 */
@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val testProgressRepository: TestProgressRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val studyContentRepository: StudyContentRepository
) : ViewModel() {
    
    private val testType: String = savedStateHandle.get<String>("topicId") ?: "OIR"
    
    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()
    
    init {
        loadTopicContent()
    }
    
    private fun loadTopicContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current user for personalized data
                val currentUser = observeCurrentUser().first()
                val userId = currentUser?.id
                
                // Check if cloud content is enabled for this topic
                val useCloud = ContentFeatureFlags.isTopicCloudEnabled(testType)
                
                // CRITICAL DEBUG: Log feature flag state
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "Feature Flag Check for: $testType")
                Log.d(TAG, "useCloudContent (master): ${ContentFeatureFlags.useCloudContent}")
                Log.d(TAG, "isTopicCloudEnabled($testType): $useCloud")
                Log.d(TAG, "Feature Flags Status:\n${ContentFeatureFlags.getStatus()}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                
                if (useCloud) {
                    Log.d(TAG, "✓ Loading $testType from CLOUD (Firestore)")
                    loadFromCloud(userId)
                } else {
                    Log.d(TAG, "✗ Loading $testType from LOCAL (hardcoded)")
                    loadFromLocal(userId)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading topic content", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load topic content"
                    )
                }
            }
        }
    }
    
    /**
     * Load content from Firestore (cloud)
     */
    private suspend fun loadFromCloud(userId: String?) {
        try {
            // Collect from Flow with lifecycle awareness
            studyContentRepository.getTopicContent(testType)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = Result.success(null)
                )
                .collect { result ->
                result.onSuccess { data ->
                    when (data) {
                        is TopicContentData -> {
                            if (data.source == ContentSource.CLOUD) {
                                // Got cloud data - convert to UI format
                                val cloudMaterials = data.materials.map { cloudMaterial ->
                                    StudyMaterialItem(
                                        id = cloudMaterial.id,
                                        title = cloudMaterial.title,
                                        duration = cloudMaterial.readTime,
                                        isPremium = cloudMaterial.isPremium
                                    )
                                }
                                
                                // For PIQ_FORM, merge with local piq_form_reference material
                                val materials = if (testType.uppercase() == "PIQ_FORM" || testType.uppercase() == "PIQ") {
                                    val localMaterials = TopicContentLoader.getTopicInfo(testType).studyMaterials
                                    val piqFormReference = localMaterials.firstOrNull { it.id == "piq_form_reference" }
                                    
                                    if (piqFormReference != null) {
                                        // Prepend piq_form_reference to cloud materials
                                        listOf(piqFormReference) + cloudMaterials
                                    } else {
                                        cloudMaterials
                                    }
                                } else {
                                    cloudMaterials
                                }
                                
                                val testProgress = loadTestProgress(userId)
                                
                                _uiState.update {
                                    it.copy(
                                        testType = testType,
                                        topicTitle = data.title,
                                        introduction = data.introduction,
                                        studyMaterials = materials,
                                        availableTests = getTestsForTopic(testType),
                                        testCompletionStatus = testProgress?.status,
                                        testLatestScore = testProgress?.latestScore,
                                        isLoading = false,
                                        error = null,
                                        contentSource = "Cloud (Firestore)"
                                    )
                                }
                                
                                Log.d(TAG, "✓ Loaded $testType from cloud: ${materials.size} materials (${cloudMaterials.size} cloud + ${materials.size - cloudMaterials.size} local)")
                            } else {
                                // Fallback triggered - use local
                                loadFromLocal(userId)
                            }
                        }
                        else -> loadFromLocal(userId)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Cloud loading failed: ${error.message}")
                    if (ContentFeatureFlags.fallbackToLocalOnError) {
                        loadFromLocal(userId)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load content: ${error.message}"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during cloud load", e)
            loadFromLocal(userId)
        }
    }
    
    /**
     * Load content from local hardcoded data (fallback)
     */
    private suspend fun loadFromLocal(userId: String?) {
        try {
            val topicInfo = TopicContentLoader.getTopicInfo(testType)
            val testProgress = loadTestProgress(userId)
            
            _uiState.update {
                it.copy(
                    testType = testType,
                    topicTitle = topicInfo.title,
                    introduction = topicInfo.introduction,
                    studyMaterials = topicInfo.studyMaterials,
                    availableTests = topicInfo.tests,
                    testCompletionStatus = testProgress?.status,
                    testLatestScore = testProgress?.latestScore,
                    isLoading = false,
                    error = null,
                    contentSource = "Local"
                )
            }
            
            Log.d(TAG, "✓ Loaded $testType from local: ${topicInfo.studyMaterials.size} materials")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local content", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to load local content: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Load test progress for user
     */
    private suspend fun loadTestProgress(userId: String?): com.ssbmax.core.domain.model.TestProgress? {
        return if (userId != null) {
            try {
                when (testType) {
                    "OIR" -> {
                        val phase1 = testProgressRepository.getPhase1Progress(userId).first()
                        phase1.oirProgress
                    }
                    "PPDT" -> {
                        val phase1 = testProgressRepository.getPhase1Progress(userId).first()
                        phase1.ppdtProgress
                    }
                    else -> {
                        val phase2 = testProgressRepository.getPhase2Progress(userId).first()
                        phase2.psychologyProgress
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load test progress", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Get available tests for a topic
     */
    private fun getTestsForTopic(testType: String): List<TestType> {
        return when (testType.uppercase()) {
            "OIR" -> listOf(TestType.OIR)
            "PPDT" -> listOf(TestType.PPDT)
            "PIQ", "PIQ_FORM" -> listOf(TestType.PIQ)
            "PSYCHOLOGY" -> listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD)
            "GTO" -> listOf(
                TestType.GTO_GD,
                TestType.GTO_GPE,
                TestType.GTO_PGT,
                TestType.GTO_GOR,
                TestType.GTO_HGT,
                TestType.GTO_LECTURETTE,
                TestType.GTO_IO,
                TestType.GTO_CT
            )
            "INTERVIEW" -> listOf(TestType.IO)
            else -> emptyList()
        }
    }
    
    companion object {
        private const val TAG = "TopicViewModel"
    }
    
    fun refresh() {
        loadTopicContent()
    }
}

/**
 * UI State for Topic Screen
 */
data class TopicUiState(
    val testType: String = "",
    val topicTitle: String = "",
    val introduction: String = "",
    val studyMaterials: List<StudyMaterialItem> = emptyList(),
    val availableTests: List<TestType> = emptyList(),
    val testCompletionStatus: com.ssbmax.core.domain.model.TestStatus? = null,
    val testLatestScore: Float? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val contentSource: String = "Local" // "Cloud (Firestore)" or "Local"
)

/**
 * Study material item for list display
 */
data class StudyMaterialItem(
    val id: String,
    val title: String,
    val duration: String,
    val isPremium: Boolean
)

