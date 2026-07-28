package com.ssbmax.ui.tests.piq

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.Education
import com.ssbmax.core.domain.model.ExtraCurricularActivity
import com.ssbmax.core.domain.model.NCCTraining
import com.ssbmax.core.domain.model.PIQAIScore
import com.ssbmax.core.domain.model.PIQPage
import com.ssbmax.core.domain.model.PIQSubmission
import com.ssbmax.core.domain.model.PreviousInterview
import com.ssbmax.core.domain.model.Sibling
import com.ssbmax.core.domain.model.SportsParticipation
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.WorkExperience
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.AppConstants
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.InterviewQuestionGenerationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val TAG = "PIQTestViewModel"

/**
 * Selection Board options for PIQ dropdown
 */
val SELECTION_BOARD_OPTIONS = listOf(
    "Army (Prayagraj)",
    "Army (Bhopal)",
    "Army (Bangalore)",
    "Army (Jalandhar)",
    "Navy (Coimbatore)",
    "Navy (Visakhapatnam)",
    "Navy (Kolkata)",
    "Navy (Bhopal)",
    "Navy (Bengaluru)",
    "Air Force (Dehradun)",
    "Air Force (Mysore)",
    "Air Force (Gandhinagar)",
    "Air Force (Varanasi)",
    "Air Force (Guwahati)",
    "Coast Guard (Noida)"
)

/**
 * ViewModel for PIQ (Personal Information Questionnaire) Test
 * 
 * Features:
 * - Two-page form navigation
 * - Auto-save every 2 seconds
 * - Free navigation between pages
 * - Draft/final submission
 * - Security checks and subscription validation
 * - OIR number auto-fill from OIR test result
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class PIQTestViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val subscriptionManager: SubscriptionManager,
    private val getOLQDashboard: com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase,
    private val difficultyManager: DifficultyProgressionManager,
    private val securityLogger: SecurityEventLogger,
    private val workManager: WorkManager,
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
                .debounce(AppConstants.Time.AUTOSAVE_DEBOUNCE_MS)
                .collect {
                    saveDraft()
                }
        }
    }

    /**
     * Load existing draft if available and auto-fill OIR number
     */
    private fun loadDraft() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📂 PIQ: Loading draft...")
                val userId = withTimeout(3000L) { // 3 second timeout for auth state
                    observeCurrentUser().first()?.id
                }
                if (userId == null) {
                    Log.w(TAG, "⚠️ PIQ: No user logged in, skipping draft load")
                    return@launch
                }

                // Load OIR number from latest OIR submission
                loadOIRNumber(userId)
                
                // TODO: Implement draft loading from Firestore
                // For now, start with empty form
                Log.d(TAG, "✅ PIQ: Ready for new form")
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load PIQ draft for test: $testId")
            }
        }
    }
    
    /**
     * Auto-fill OIR number from latest OIR test result
     */
    private suspend fun loadOIRNumber(userId: String) {
        try {
            Log.d(TAG, "🔍 PIQ: Loading OIR number from latest OIR submission...")
            
            // Get user's latest OIR submission
            val submissionsResult = submissionRepository.getUserSubmissionsByTestType(
                userId = userId,
                testType = TestType.OIR,
                limit = 1
            )
            
            val submissions = submissionsResult.getOrNull() ?: emptyList()
            val latestOIR = submissions.firstOrNull()
            
            if (latestOIR != null) {
                // Extract OIR number from submission (assuming it's stored in test result)
                // For now, use submission ID as OIR number placeholder
                // TODO: Extract actual OIR number from OIR test result when available
                val submissionId = latestOIR["id"] as? String ?: ""
                val oirNumber = submissionId.takeIf { it.isNotBlank() } ?: ""
                
                if (oirNumber.isNotBlank()) {
                    Log.d(TAG, "✅ PIQ: Auto-filled OIR number: $oirNumber")
                    _uiState.update { state ->
                        val updatedAnswers = state.answers.toMutableMap()
                        updatedAnswers["oirNumber"] = oirNumber
                        state.copy(answers = updatedAnswers)
                    }
                } else {
                    Log.d(TAG, "ℹ️ PIQ: No OIR number found in latest submission")
                }
            } else {
                Log.d(TAG, "ℹ️ PIQ: No OIR submissions found")
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to load OIR number for PIQ form")
            // Don't fail the whole form if OIR loading fails
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
                
                val userId = withTimeout(3000L) { // 3 second timeout for auth state
                    observeCurrentUser().first()?.id
                }
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
                    ErrorLogger.log(error, "Failed to save PIQ draft for test: $testId")
                    _uiState.update { it.copy(isSaving = false) }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception in PIQ draft save operation for test: $testId")
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

            var currentUserId: String? = null
            try {
                currentUserId = resolveEligibleUserId() ?: return@launch

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
                    handleSubmissionSuccess(currentUserId, submissionId)
                }.onFailure { error ->
                    ErrorLogger.log(error, "Failed to submit PIQ test for user: $currentUserId")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to submit PIQ: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception during PIQ submission for user: ${currentUserId ?: "unknown"}")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "An error occurred: ${e.message}"
                ) }
            }
        }
    }

    /**
     * Resolve the current authenticated user and verify subscription eligibility.
     * Updates UI state and returns null if the user should not proceed with submission.
     */
    private suspend fun resolveEligibleUserId(): String? {
        // Step 1: Get current user
        Log.d(TAG, "📍 PIQ Step 1: Getting current user...")
        val currentUserId = withTimeout(3000L) { // 3 second timeout for auth state
            observeCurrentUser().first()?.id
        }
        if (currentUserId == null) {
            ErrorLogger.log(Exception("User not authenticated during PIQ submission"), "PIQ submission failed: user not authenticated")
            securityLogger.logUnauthenticatedAccess(
                testType = TestType.PIQ,
                context = "PIQTestViewModel.submitTest"
            )
            _uiState.update { it.copy(
                isLoading = false,
                error = "Please login to submit PIQ"
            ) }
            return null
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
                return null
            }
            is com.ssbmax.core.data.repository.TestEligibility.NetworkError -> {
                _uiState.update { it.copy(isLoading = false, error = "No connection. Please check your network and try again.") }
                return null
            }
            is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                Log.d(TAG, "✅ PIQ: Eligible (${eligibility.remainingTests} tests remaining)")
            }
        }

        // Step 3: Get user profile for subscription type
        Log.d(TAG, "📍 PIQ Step 3: Getting user profile...")
        val userProfileResult = withTimeout(5000L) { // 5 second timeout for user profile fetch
            userProfileRepository.getUserProfile(currentUserId).first()
        }
        val userProfile = userProfileResult.getOrNull()
        val subscriptionType = userProfile?.subscriptionType ?: SubscriptionType.FREE
        Log.d(TAG, "✅ PIQ: Subscription type: $subscriptionType")

        return currentUserId
    }

    private suspend fun handleSubmissionSuccess(userId: String, submissionId: String) {
        Log.d(TAG, "✅ PIQ: Successfully submitted with ID: $submissionId")

        // Step 6: Record usage
        Log.d(TAG, "📍 PIQ Step 6: Recording test usage...")
        subscriptionManager.recordTestUsage(TestType.PIQ, userId, submissionId)
        Log.d(TAG, "✅ PIQ: Usage recorded")

        // Invalidate OLQ dashboard cache (user just completed a test)
        Log.d(TAG, "📍 PIQ: Invalidating OLQ dashboard cache...")
        getOLQDashboard.invalidateCache(userId)
        Log.d(TAG, "✅ PIQ: Dashboard cache invalidated!")

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

        // Step 8: Trigger background question generation
        Log.d(TAG, "📍 PIQ Step 8: Triggering background question generation...")
        triggerBackgroundQuestionGeneration(submissionId)

        _uiState.update { it.copy(
            isLoading = false,
            submissionComplete = true,
            submissionId = submissionId
        ) }

        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "✅ PIQ: Submission complete!")
        Log.d(TAG, "════════════════════════════════════════")
    }

    /**
     * Generate mock AI quality score for PIQ
     */
    private fun generateMockAIScore(submission: PIQSubmission): PIQAIScore {
        // Calculate completeness (excluding strengths/weaknesses as they're not in actual SSB PIQ)
        val totalFields = 18 // All PIQ fields (excluding gender/phone/email which are not in actual SSB PIQ)
        val filledFields = listOf(
            submission.fullName, submission.dateOfBirth,
            submission.fatherName, submission.motherName,
            submission.hobbies, submission.sports
        ).count { it.isNotBlank() }
        val completeness = (filledFields.toFloat() / totalFields * 100).toInt()
        
        // Mock scores based on completeness
        val personalInfo = (completeness * 0.33f * 0.8f) + kotlin.random.Random.nextFloat() * 5f
        val familyInfo = (completeness * 0.33f * 0.9f) + kotlin.random.Random.nextFloat() * 3f
        val educationCareer = (completeness * 0.34f * 0.85f) + kotlin.random.Random.nextFloat() * 4f
        // Self-assessment score based on overall completeness
        val selfAssessment = if (completeness > 80)
            17f + kotlin.random.Random.nextFloat() * 8f else 10f + kotlin.random.Random.nextFloat() * 7f
        
        val overall = personalInfo + familyInfo + educationCareer + selfAssessment
        
        return PIQAIScore(
            overallScore = overall.coerceIn(60f, 95f),
            personalInfoScore = personalInfo.coerceIn(15f, 25f),
            familyInfoScore = familyInfo.coerceIn(16f, 25f),
            motivationScore = educationCareer.coerceIn(12f, 25f), // Using educationCareer for motivationScore field
            selfAssessmentScore = selfAssessment.coerceIn(10f, 25f),
            feedback = when {
                overall >= 85 -> "Excellent PIQ! Comprehensive information provided. Well-prepared for assessor questions."
                overall >= 75 -> "Good PIQ. Adequate information provided. Some areas could be more detailed."
                else -> "PIQ needs improvement. Add more details to all sections."
            },
            strengths = buildList {
                if (completeness > 80) add("Comprehensive information")
                if (submission.hobbies.isNotBlank()) add("Well-documented interests")
            },
            areasForImprovement = buildList {
                if (completeness < 70) add("Fill all sections completely")
                if (submission.hobbies.isBlank()) add("Add hobbies and interests")
            },
            completenessPercentage = completeness,
            clarityScore = 7.5f,
            consistencyScore = 8.0f + kotlin.random.Random.nextFloat() * 2f
        )
    }

    /**
     * Create PIQSubmission from current state
     */
    private fun createSubmissionFromState(userId: String, status: SubmissionStatus): PIQSubmission {
        val state = _uiState.value
        val answers = state.answers

        var submission = PIQSubmission(userId = userId, testId = testId)
        submission = applyHeaderAnswers(submission, answers)
        submission = applyPersonalInfoAnswers(submission, answers)
        submission = applyResidenceAnswers(submission, answers)
        submission = applyParentDetailsAnswers(submission, answers)
        submission = applyGuardianAndSiblingsAnswers(submission, answers)
        submission = applyEducationAndActivitiesAnswers(submission, answers, state)
        submission = applyServiceAnswers(submission, answers, state, status)
        return submission
    }

    private fun applyHeaderAnswers(submission: PIQSubmission, answers: Map<String, String>): PIQSubmission {
        return submission.copy(
            oirNumber = answers["oirNumber"] ?: "",
            selectionBoard = answers["selectionBoard"] ?: "",
            batchNumber = answers["batchNumber"] ?: "",
            chestNumber = answers["chestNumber"] ?: "",
            upscRollNumber = answers["upscRollNumber"] ?: ""
        )
    }

    private fun applyPersonalInfoAnswers(submission: PIQSubmission, answers: Map<String, String>): PIQSubmission {
        return submission.copy(
            fullName = answers["fullName"] ?: "",
            dateOfBirth = answers["dateOfBirth"] ?: "",
            age = answers["age"] ?: "",
            gender = "", // Not in actual SSB PIQ
            phone = "", // Not in actual SSB PIQ
            email = "", // Not in actual SSB PIQ
            state = answers["state"] ?: "",
            district = answers["district"] ?: "",
            religion = answers["religion"] ?: "",
            scStObcStatus = answers["scStObcStatus"] ?: "",
            motherTongue = answers["motherTongue"] ?: "",
            maritalStatus = answers["maritalStatus"] ?: ""
        )
    }

    private fun applyResidenceAnswers(submission: PIQSubmission, answers: Map<String, String>): PIQSubmission {
        return submission.copy(
            permanentAddress = answers["permanentAddress"] ?: "",
            presentAddress = answers["presentAddress"] ?: "",
            maximumResidence = answers["maximumResidence"] ?: "",
            maximumResidencePopulation = answers["maximumResidencePopulation"] ?: "",
            presentResidencePopulation = answers["presentResidencePopulation"] ?: "",
            permanentResidencePopulation = answers["permanentResidencePopulation"] ?: "",
            isDistrictHQ = answers["isDistrictHQ"]?.toBoolean() ?: false,
            height = answers["height"] ?: "",
            weight = answers["weight"] ?: ""
        )
    }

    private fun applyParentDetailsAnswers(submission: PIQSubmission, answers: Map<String, String>): PIQSubmission {
        return submission.copy(
            fatherName = answers["fatherName"] ?: "",
            fatherOccupation = answers["fatherOccupation"] ?: "",
            fatherEducation = answers["fatherEducation"] ?: "",
            fatherIncome = answers["fatherIncome"] ?: "",
            motherName = answers["motherName"] ?: "",
            motherOccupation = answers["motherOccupation"] ?: "",
            motherEducation = answers["motherEducation"] ?: "",
            parentsAlive = answers["parentsAlive"] ?: "",
            ageAtFatherDeath = answers["ageAtFatherDeath"] ?: "",
            ageAtMotherDeath = answers["ageAtMotherDeath"] ?: ""
        )
    }

    private fun applyGuardianAndSiblingsAnswers(submission: PIQSubmission, answers: Map<String, String>): PIQSubmission {
        return submission.copy(
            guardianName = answers["guardianName"] ?: "",
            guardianOccupation = answers["guardianOccupation"] ?: "",
            guardianEducation = answers["guardianEducation"] ?: "",
            guardianIncome = answers["guardianIncome"] ?: "",
            siblings = parseSiblingsFromAnswers(answers),
            presentOccupation = answers["presentOccupation"] ?: "",
            personalMonthlyIncome = answers["personalMonthlyIncome"] ?: ""
        )
    }

    private fun parseSiblingsFromAnswers(answers: Map<String, String>): List<Sibling> {
        val siblings = mutableListOf<Sibling>()
        for (prefixGroup in listOf("elderSibling", "youngerSibling")) {
            repeat(2) { index ->
                val prefix = "$prefixGroup${index + 1}_"
                val name = answers["${prefix}name"] ?: ""
                if (name.isNotBlank()) {
                    siblings.add(Sibling(
                        name = name,
                        age = answers["${prefix}age"] ?: "",
                        education = answers["${prefix}education"] ?: "",
                        occupation = answers["${prefix}occupation"] ?: "",
                        income = answers["${prefix}income"] ?: ""
                    ))
                }
            }
        }
        return siblings
    }

    private fun applyEducationAndActivitiesAnswers(
        submission: PIQSubmission,
        answers: Map<String, String>,
        state: PIQUiState
    ): PIQSubmission {
        return submission.copy(
            education10th = parseEducation10thAnswers(answers),
            education12th = parseEducation12thAnswers(answers),
            educationGraduation = parseEducationGraduationAnswers(answers),
            educationPostGraduation = parseEducationPostGraduationAnswers(answers),
            hobbies = answers["hobbies"] ?: "",
            sports = answers["sports"] ?: "", // Legacy field
            sportsParticipation = state.sportsParticipation,
            extraCurricularActivities = state.extraCurricularActivities,
            positionsOfResponsibility = answers["positionsOfResponsibility"] ?: "",
            workExperience = state.workExperience,
            nccTraining = parseNccTrainingAnswers(answers)
        )
    }

    private fun parseEducation10thAnswers(answers: Map<String, String>): Education {
        return Education(
            level = "10th",
            institution = answers["education10th_institution"] ?: "",
            board = answers["education10th_board"] ?: "",
            year = answers["education10th_year"] ?: "",
            percentage = answers["education10th_percentage"] ?: "",
            mediumOfInstruction = answers["education10th_medium"] ?: "",
            boarderDayScholar = answers["education10th_boarder"] ?: "",
            outstandingAchievement = answers["education10th_achievement"] ?: ""
        )
    }

    private fun parseEducation12thAnswers(answers: Map<String, String>): Education {
        return Education(
            level = "12th",
            institution = answers["education12th_institution"] ?: "",
            board = answers["education12th_board"] ?: "",
            stream = answers["education12th_stream"] ?: "",
            year = answers["education12th_year"] ?: "",
            percentage = answers["education12th_percentage"] ?: "",
            mediumOfInstruction = answers["education12th_medium"] ?: "",
            boarderDayScholar = answers["education12th_boarder"] ?: "",
            outstandingAchievement = answers["education12th_achievement"] ?: ""
        )
    }

    private fun parseEducationGraduationAnswers(answers: Map<String, String>): Education {
        return Education(
            level = "Graduation",
            institution = answers["educationGrad_institution"] ?: "",
            board = answers["educationGrad_university"] ?: "",
            year = answers["educationGrad_year"] ?: "",
            cgpa = answers["educationGrad_cgpa"] ?: "",
            mediumOfInstruction = answers["educationGrad_medium"] ?: "",
            boarderDayScholar = answers["educationGrad_boarder"] ?: "",
            outstandingAchievement = answers["educationGrad_achievement"] ?: ""
        )
    }

    private fun parseEducationPostGraduationAnswers(answers: Map<String, String>): Education {
        return Education(
            level = "Post-Graduation",
            institution = answers["educationPG_institution"] ?: "",
            board = answers["educationPG_university"] ?: "",
            year = answers["educationPG_year"] ?: "",
            cgpa = answers["educationPG_cgpa"] ?: "",
            mediumOfInstruction = answers["educationPG_medium"] ?: "",
            boarderDayScholar = answers["educationPG_boarder"] ?: "",
            outstandingAchievement = answers["educationPG_achievement"] ?: ""
        )
    }

    private fun parseNccTrainingAnswers(answers: Map<String, String>): NCCTraining {
        return NCCTraining(
            hasTraining = answers["ncc_hasTraining"]?.toBoolean() ?: false,
            totalTraining = answers["ncc_totalTraining"] ?: "",
            wing = answers["ncc_wing"] ?: "",
            division = answers["ncc_division"] ?: "",
            certificateObtained = answers["ncc_certificate"] ?: ""
        )
    }

    private fun applyServiceAnswers(
        submission: PIQSubmission,
        answers: Map<String, String>,
        state: PIQUiState,
        status: SubmissionStatus
    ): PIQSubmission {
        return submission.copy(
            natureOfCommission = answers["natureOfCommission"] ?: "",
            choiceOfService = answers["choiceOfService"] ?: "",
            chancesAvailed = answers["chancesAvailed"] ?: "",
            previousInterviews = state.previousInterviews,
            whyDefenseForces = answers["whyDefenseForces"] ?: "",
            strengths = "", // Not in actual SSB PIQ form - kept for backward compatibility
            weaknesses = "", // Not in actual SSB PIQ form - kept for backward compatibility
            status = status,
            submittedAt = System.currentTimeMillis(),
            lastModifiedAt = state.lastModifiedAt
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Trigger background question generation after PIQ submission
     *
     * Schedules a background WorkManager job to:
     * 1. Fetch PIQ data from Firestore
     * 2. Generate 18 personalized questions using Gemini AI
     * 3. Cache questions for 30 days
     *
     * Constraints:
     * - Requires network connection (for Gemini API)
     * - Requires battery not low (to avoid draining)
     */
    private fun triggerBackgroundQuestionGeneration(submissionId: String) {
        val workRequest = OneTimeWorkRequestBuilder<InterviewQuestionGenerationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInputData(workDataOf(
                InterviewQuestionGenerationWorker.KEY_PIQ_SUBMISSION_ID to submissionId,
                InterviewQuestionGenerationWorker.KEY_NOTIFY_ON_COMPLETE to false
            ))
            .addTag(AppConstants.WorkManager.PIQ_GENERATION_TAG)
            .build()

        workManager.enqueueUniqueWork(
            "gen_questions_$submissionId",
            ExistingWorkPolicy.KEEP, // Don't duplicate if already scheduled
            workRequest
        )

        Log.d(TAG, "📋 Enqueued background question generation for PIQ: $submissionId")
        Log.d(TAG, "   Questions will be ready for instant interview start in 30s-1min")
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveChannel.close()  // Close channel to prevent memory leaks
        Log.d(TAG, "🧹 PIQ: ViewModel cleared, autoSaveChannel closed")
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
    val error: String? = null,
    // Complex structures
    val siblings: List<Sibling> = emptyList(),
    val sportsParticipation: List<SportsParticipation> = emptyList(),
    val extraCurricularActivities: List<ExtraCurricularActivity> = emptyList(),
    val previousInterviews: List<PreviousInterview> = emptyList(),
    val workExperience: List<WorkExperience> = emptyList()
) {
    val canSubmit: Boolean
        get() = true // All fields are optional - lenient form
}

