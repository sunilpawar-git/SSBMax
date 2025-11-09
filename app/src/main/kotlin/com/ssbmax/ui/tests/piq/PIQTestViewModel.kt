package com.ssbmax.ui.tests.piq

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PIQTestViewModel"

/**
 * ViewModel for PIQ (Personal Information Questionnaire) Test
 * 
 * Features:
 * - Two-page form navigation
 * - Auto-save every 2 seconds
 * - Free navigation between pages
 * - Draft/final submission
 * - Security checks and subscription validation
 */
@HiltViewModel
class PIQTestViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val subscriptionManager: SubscriptionManager,
    private val difficultyManager: DifficultyProgressionManager,
    private val securityLogger: SecurityEventLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val testId: String = savedStateHandle["testId"] ?: "piq_standard"

    private val _uiState = MutableStateFlow(PIQUiState())
    val uiState: StateFlow<PIQUiState> = _uiState.asStateFlow()

    // Auto-save channel with debounce
    private val autoSaveChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        Log.d(TAG, "📋 PIQ: ViewModel initialized for test: $testId")
        loadDraft()
        setupAutoSave()
    }

    /**
     * Setup auto-save mechanism with 2-second debounce
     */
    private fun setupAutoSave() {
        viewModelScope.launch {
            autoSaveChannel.receiveAsFlow()
                .debounce(2000L) // 2 seconds
                .collect {
                    saveDraft()
                }
        }
    }

    /**
     * Load existing draft if available
     */
    private fun loadDraft() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📂 PIQ: Loading draft...")
                val userId = observeCurrentUser().first()?.id
                if (userId == null) {
                    Log.w(TAG, "⚠️ PIQ: No user logged in, skipping draft load")
                    return@launch
                }

                // TODO: Implement draft loading from Firestore
                // For now, start with empty form
                Log.d(TAG, "✅ PIQ: Ready for new form")
            } catch (e: Exception) {
                Log.e(TAG, "❌ PIQ: Error loading draft", e)
            }
        }
    }

    /**
     * Update a field value and trigger auto-save
     */
    fun updateField(fieldId: String, value: String) {
        _uiState.update { state ->
            val updatedAnswers = state.answers.toMutableMap()
            updatedAnswers[fieldId] = value
            state.copy(
                answers = updatedAnswers,
                lastModifiedAt = System.currentTimeMillis()
            )
        }
        
        // Trigger auto-save
        autoSaveChannel.trySend(Unit)
    }

    /**
     * Navigate to a specific page
     */
    fun navigateToPage(page: PIQPage) {
        Log.d(TAG, "🔄 PIQ: Navigating to ${page.displayName}")
        _uiState.update { it.copy(currentPage = page) }
    }

    /**
     * Move to review screen
     */
    fun goToReview() {
        Log.d(TAG, "📝 PIQ: Moving to review screen")
        _uiState.update { it.copy(showReviewScreen = true) }
    }

    /**
     * Return from review to specific page for editing
     */
    fun editPage(page: PIQPage) {
        Log.d(TAG, "✏️ PIQ: Editing ${page.displayName}")
        _uiState.update { it.copy(
            showReviewScreen = false,
            currentPage = page
        ) }
    }

    /**
     * Save draft to Firestore
     */
    private fun saveDraft() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state.isSaving || state.answers.isEmpty()) return@launch

                _uiState.update { it.copy(isSaving = true) }
                
                Log.d(TAG, "💾 PIQ: Auto-saving draft...")
                
                val userId = observeCurrentUser().first()?.id
                if (userId == null) {
                    Log.w(TAG, "⚠️ PIQ: Cannot save draft - user not logged in")
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }

                // Create submission with DRAFT status
                val submission = createSubmissionFromState(userId, SubmissionStatus.DRAFT)
                
                // Save to Firestore
                val result = submissionRepository.submitPIQ(submission)
                
                result.onSuccess {
                    Log.d(TAG, "✅ PIQ: Draft saved successfully")
                    _uiState.update { it.copy(
                        isSaving = false,
                        lastSavedAt = System.currentTimeMillis()
                    ) }
                }.onFailure { error ->
                    Log.e(TAG, "❌ PIQ: Failed to save draft", error)
                    _uiState.update { it.copy(isSaving = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ PIQ: Error in saveDraft", e)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    /**
     * Submit final PIQ form
     */
    fun submitTest() {
        viewModelScope.launch {
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "📤 PIQ: submitTest() called")
            Log.d(TAG, "════════════════════════════════════════")
            
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Step 1: Get current user
                Log.d(TAG, "📍 PIQ Step 1: Getting current user...")
                val currentUserId: String = observeCurrentUser().first()?.id ?: run {
                    Log.e(TAG, "❌ PIQ: User not authenticated")
                    securityLogger.logUnauthenticatedAccess(
                        testType = TestType.PIQ,
                        context = "PIQTestViewModel.submitTest"
                    )
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to submit PIQ"
                    ) }
                    return@launch
                }
                Log.d(TAG, "✅ PIQ: User ID: $currentUserId")
                
                // Step 2: Check subscription eligibility
                Log.d(TAG, "📍 PIQ Step 2: Checking subscription eligibility...")
                val eligibility = subscriptionManager.canTakeTest(TestType.PIQ, currentUserId)
                when (eligibility) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        Log.w(TAG, "⚠️ PIQ: Subscription limit reached")
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Monthly PIQ limit reached. Upgrade to submit more tests."
                        ) }
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        Log.d(TAG, "✅ PIQ: Eligible (${eligibility.remainingTests} tests remaining)")
                    }
                }
                
                // Step 3: Get user profile for subscription type
                Log.d(TAG, "📍 PIQ Step 3: Getting user profile...")
                val userProfileResult = userProfileRepository.getUserProfile(currentUserId).first()
                val userProfile = userProfileResult.getOrNull()
                val subscriptionType = userProfile?.subscriptionType ?: SubscriptionType.FREE
                Log.d(TAG, "✅ PIQ: Subscription type: $subscriptionType")
                
                // Step 4: Create submission
                Log.d(TAG, "📍 PIQ Step 4: Creating submission...")
                val submission = createSubmissionFromState(currentUserId, SubmissionStatus.SUBMITTED_PENDING_REVIEW)
                Log.d(TAG, "✅ PIQ: Submission created")
                
                // Step 4.5: Generate AI quality score
                Log.d(TAG, "📍 PIQ Step 4.5: Generating AI quality score...")
                val aiScore = generateMockAIScore(submission)
                val submissionWithAI = submission.copy(aiPreliminaryScore = aiScore)
                Log.d(TAG, "✅ PIQ: AI score generated - Overall: ${aiScore.overallScore}/100")
                
                // Step 5: Submit to Firestore
                Log.d(TAG, "📍 PIQ Step 5: Submitting to Firestore...")
                val result = submissionRepository.submitPIQ(submissionWithAI, batchId = null)
                
                result.onSuccess { submissionId ->
                    Log.d(TAG, "✅ PIQ: Successfully submitted with ID: $submissionId")
                    
                    // Step 6: Record usage
                    Log.d(TAG, "📍 PIQ Step 6: Recording test usage...")
                    subscriptionManager.recordTestUsage(TestType.PIQ, currentUserId, submissionId)
                    Log.d(TAG, "✅ PIQ: Usage recorded")
                    
                    // Step 7: Track performance (PIQ is reference data, so 100% completion)
                    Log.d(TAG, "📍 PIQ Step 7: Recording performance...")
                    difficultyManager.recordPerformance(
                        testType = "PIQ",
                        difficulty = "STANDARD",
                        score = 100f,
                        correctAnswers = 1,
                        totalQuestions = 1,
                        timeSeconds = 0f
                    )
                    Log.d(TAG, "✅ PIQ: Performance recorded")
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        submissionComplete = true,
                        submissionId = submissionId
                    ) }
                    
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "✅ PIQ: Submission complete!")
                    Log.d(TAG, "════════════════════════════════════════")
                }.onFailure { error ->
                    Log.e(TAG, "❌ PIQ: Submission failed", error)
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to submit PIQ: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ PIQ: Error in submitTest", e)
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "An error occurred: ${e.message}"
                ) }
            }
        }
    }

    /**
     * Generate mock AI quality score for PIQ
     */
    private fun generateMockAIScore(submission: PIQSubmission): PIQAIScore {
        // Calculate completeness
        val totalFields = 21 // All PIQ fields
        val filledFields = listOf(
            submission.fullName, submission.dateOfBirth, submission.phone,
            submission.fatherName, submission.motherName,
            submission.whyDefenseForces, submission.strengths, submission.weaknesses,
            submission.hobbies, submission.sports
        ).count { it.isNotBlank() }
        val completeness = (filledFields.toFloat() / totalFields * 100).toInt()
        
        // Mock scores based on completeness and length
        val personalInfo = (completeness * 0.25f * 0.8f) + kotlin.random.Random.nextFloat() * 5f
        val familyInfo = (completeness * 0.25f * 0.9f) + kotlin.random.Random.nextFloat() * 3f
        val motivation = if (submission.whyDefenseForces.length > 100) 
            18f + kotlin.random.Random.nextFloat() * 7f else 12f + kotlin.random.Random.nextFloat() * 6f
        val selfAssessment = if (submission.strengths.isNotBlank() && submission.weaknesses.isNotBlank())
            17f + kotlin.random.Random.nextFloat() * 8f else 10f + kotlin.random.Random.nextFloat() * 7f
        
        val overall = personalInfo + familyInfo + motivation + selfAssessment
        
        return PIQAIScore(
            overallScore = overall.coerceIn(60f, 95f),
            personalInfoScore = personalInfo.coerceIn(15f, 25f),
            familyInfoScore = familyInfo.coerceIn(16f, 25f),
            motivationScore = motivation.coerceIn(12f, 25f),
            selfAssessmentScore = selfAssessment.coerceIn(10f, 25f),
            feedback = when {
                overall >= 85 -> "Excellent PIQ! Comprehensive information with clear motivation. Well-prepared for assessor questions."
                overall >= 75 -> "Good PIQ. Adequate information provided. Some areas could be more detailed."
                else -> "PIQ needs improvement. Add more details to motivation and self-assessment sections."
            },
            strengths = buildList {
                if (completeness > 80) add("Comprehensive information")
                if (submission.whyDefenseForces.length > 150) add("Clear defense forces motivation")
                if (submission.strengths.isNotBlank()) add("Self-awareness of strengths")
            },
            areasForImprovement = buildList {
                if (completeness < 70) add("Fill all sections completely")
                if (submission.whyDefenseForces.length < 100) add("Elaborate on defense forces motivation")
                if (submission.weaknesses.isBlank()) add("Add areas for improvement (shows self-awareness)")
            },
            completenessPercentage = completeness,
            clarityScore = if (submission.whyDefenseForces.length > 150) 8.5f else 6.5f,
            consistencyScore = 8.0f + kotlin.random.Random.nextFloat() * 2f
        )
    }

    /**
     * Create PIQSubmission from current state
     */
    private fun createSubmissionFromState(userId: String, status: SubmissionStatus): PIQSubmission {
        val answers = _uiState.value.answers
        
        return PIQSubmission(
            userId = userId,
            testId = testId,
            fullName = answers["fullName"] ?: "",
            dateOfBirth = answers["dateOfBirth"] ?: "",
            age = answers["age"] ?: "",
            gender = answers["gender"] ?: "",
            phone = answers["phone"] ?: "",
            email = answers["email"] ?: "",
            permanentAddress = answers["permanentAddress"] ?: "",
            presentAddress = answers["presentAddress"] ?: "",
            fatherName = answers["fatherName"] ?: "",
            fatherOccupation = answers["fatherOccupation"] ?: "",
            fatherEducation = answers["fatherEducation"] ?: "",
            fatherIncome = answers["fatherIncome"] ?: "",
            motherName = answers["motherName"] ?: "",
            motherOccupation = answers["motherOccupation"] ?: "",
            motherEducation = answers["motherEducation"] ?: "",
            hobbies = answers["hobbies"] ?: "",
            sports = answers["sports"] ?: "",
            whyDefenseForces = answers["whyDefenseForces"] ?: "",
            strengths = answers["strengths"] ?: "",
            weaknesses = answers["weaknesses"] ?: "",
            status = status,
            submittedAt = System.currentTimeMillis(),
            lastModifiedAt = _uiState.value.lastModifiedAt
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for PIQ Test
 */
data class PIQUiState(
    val currentPage: PIQPage = PIQPage.PAGE_1,
    val answers: Map<String, String> = emptyMap(),
    val validationErrors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val lastSavedAt: Long? = null,
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val showReviewScreen: Boolean = false,
    val submissionComplete: Boolean = false,
    val submissionId: String? = null,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = answers["fullName"]?.isNotBlank() == true &&
                answers["dateOfBirth"]?.isNotBlank() == true &&
                answers["phone"]?.isNotBlank() == true
}

