package com.ssbmax.ui.tests.piq

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PIQSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PIQSubmissionResultUiState())
    val uiState: StateFlow<PIQSubmissionResultUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "PIQResultViewModel"
    }

    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            Log.d(TAG, "📥 PIQ Result: Loading submission from Firestore...")
            Log.d(TAG, "   Submission ID: $submissionId")
            _uiState.update { it.copy(isLoading = true) }

            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        Log.e(TAG, "❌ PIQ Result: Submission data is null")
                        _uiState.update { it.copy(isLoading = false, submission = null, error = "Submission not found") }
                        return@onSuccess
                    }
                    
                    Log.d(TAG, "✅ PIQ Result: Submission data received from Firestore")
                    Log.d(TAG, "   Parsing submission data...")
                    val submission = parsePIQSubmission(data)
                    
                    if (submission != null) {
                        Log.d(TAG, "✅ PIQ Result: Successfully parsed submission")
                        Log.d(TAG, "   Full Name: ${submission.fullName}")
                        Log.d(TAG, "   AI Score: ${submission.aiPreliminaryScore?.overallScore}")
                    } else {
                        Log.e(TAG, "❌ PIQ Result: Failed to parse submission data")
                    }
                    
                    _uiState.update { it.copy(isLoading = false, submission = submission,
                        error = if (submission == null) "Submission not found" else null) }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ PIQ Result: Failed to load submission - ${error.message}", error)
                    _uiState.update { it.copy(isLoading = false,
                        error = error.message ?: "Failed to load submission") }
                }
        }
    }

    private fun parsePIQSubmission(data: Map<String, Any>): PIQSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null
            
            // Parse siblings
            val siblingsList = submissionData["siblings"] as? List<*> ?: emptyList<Any>()
            val siblings = siblingsList.mapNotNull { siblingData ->
                val sibling = siblingData as? Map<*, *> ?: return@mapNotNull null
                Sibling(
                    id = sibling["id"] as? String ?: "",
                    name = sibling["name"] as? String ?: "",
                    age = sibling["age"]?.toString() ?: "",
                    occupation = sibling["occupation"] as? String ?: "",
                    education = sibling["education"] as? String ?: "",
                    income = sibling["income"] as? String ?: ""
                )
            }
            
            // Parse education
            val education10thMap = submissionData["education10th"] as? Map<*, *>
            val education10th = education10thMap?.let { eduMap ->
                Education(
                    level = eduMap["level"] as? String ?: "",
                    institution = eduMap["institution"] as? String ?: "",
                    board = eduMap["board"] as? String ?: "",
                    stream = "",
                    year = eduMap["year"]?.toString() ?: "",
                    percentage = eduMap["percentage"]?.toString() ?: "",
                    cgpa = "",
                    mediumOfInstruction = eduMap["mediumOfInstruction"] as? String ?: "",
                    boarderDayScholar = eduMap["boarderDayScholar"] as? String ?: "",
                    outstandingAchievement = eduMap["outstandingAchievement"] as? String ?: ""
                )
            } ?: Education(level = "10th")
            
            val education12thMap = submissionData["education12th"] as? Map<*, *>
            val education12th = education12thMap?.let { eduMap ->
                Education(
                    level = eduMap["level"] as? String ?: "",
                    institution = eduMap["institution"] as? String ?: "",
                    board = eduMap["board"] as? String ?: "",
                    stream = eduMap["stream"] as? String ?: "",
                    year = eduMap["year"]?.toString() ?: "",
                    percentage = eduMap["percentage"]?.toString() ?: "",
                    cgpa = "",
                    mediumOfInstruction = eduMap["mediumOfInstruction"] as? String ?: "",
                    boarderDayScholar = eduMap["boarderDayScholar"] as? String ?: "",
                    outstandingAchievement = eduMap["outstandingAchievement"] as? String ?: ""
                )
            } ?: Education(level = "12th")
            
            val educationGraduationMap = submissionData["educationGraduation"] as? Map<*, *>
            val educationGraduation = educationGraduationMap?.let { eduMap ->
                Education(
                    level = eduMap["level"] as? String ?: "",
                    institution = eduMap["institution"] as? String ?: "",
                    board = eduMap["board"] as? String ?: "",
                    stream = "",
                    year = eduMap["year"]?.toString() ?: "",
                    percentage = "",
                    cgpa = eduMap["cgpa"]?.toString() ?: "",
                    mediumOfInstruction = eduMap["mediumOfInstruction"] as? String ?: "",
                    boarderDayScholar = eduMap["boarderDayScholar"] as? String ?: "",
                    outstandingAchievement = eduMap["outstandingAchievement"] as? String ?: ""
                )
            } ?: Education(level = "Graduation")
            
            val educationPostGraduationMap = submissionData["educationPostGraduation"] as? Map<*, *>
            val educationPostGraduation = educationPostGraduationMap?.let { eduMap ->
                Education(
                    level = eduMap["level"] as? String ?: "",
                    institution = eduMap["institution"] as? String ?: "",
                    board = eduMap["board"] as? String ?: "",
                    stream = "",
                    year = eduMap["year"]?.toString() ?: "",
                    percentage = "",
                    cgpa = eduMap["cgpa"]?.toString() ?: "",
                    mediumOfInstruction = eduMap["mediumOfInstruction"] as? String ?: "",
                    boarderDayScholar = eduMap["boarderDayScholar"] as? String ?: "",
                    outstandingAchievement = eduMap["outstandingAchievement"] as? String ?: ""
                )
            } ?: Education(level = "Post-Graduation")
            
            // Parse NCC Training
            val nccTrainingMap = submissionData["nccTraining"] as? Map<*, *>
            val nccTraining = nccTrainingMap?.let { nccMap ->
                NCCTraining(
                    hasTraining = nccMap["hasTraining"] as? Boolean ?: false,
                    totalTraining = nccMap["totalTraining"] as? String ?: "",
                    wing = nccMap["wing"] as? String ?: "",
                    division = nccMap["division"] as? String ?: "",
                    certificateObtained = nccMap["certificateObtained"] as? String ?: ""
                )
            } ?: NCCTraining()
            
            // Parse sports participation
            val sportsParticipationList = submissionData["sportsParticipation"] as? List<*> ?: emptyList<Any>()
            val sportsParticipation = sportsParticipationList.mapNotNull { sportData ->
                val sport = sportData as? Map<*, *> ?: return@mapNotNull null
                SportsParticipation(
                    id = sport["id"] as? String ?: "",
                    sport = sport["sport"] as? String ?: "",
                    period = sport["period"] as? String ?: "",
                    representedInstitution = sport["representedInstitution"] as? String ?: "",
                    outstandingAchievement = sport["outstandingAchievement"] as? String ?: ""
                )
            }
            
            // Parse extra-curricular activities
            val extraCurricularList = submissionData["extraCurricularActivities"] as? List<*> ?: emptyList<Any>()
            val extraCurricularActivities = extraCurricularList.mapNotNull { activityData ->
                val activity = activityData as? Map<*, *> ?: return@mapNotNull null
                ExtraCurricularActivity(
                    id = activity["id"] as? String ?: "",
                    activityName = activity["activityName"] as? String ?: "",
                    duration = activity["duration"] as? String ?: "",
                    outstandingAchievement = activity["outstandingAchievement"] as? String ?: ""
                )
            }
            
            // Parse previous interviews
            val previousInterviewsList = submissionData["previousInterviews"] as? List<*> ?: emptyList<Any>()
            val previousInterviews = previousInterviewsList.mapNotNull { interviewData ->
                val interview = interviewData as? Map<*, *> ?: return@mapNotNull null
                PreviousInterview(
                    id = interview["id"] as? String ?: "",
                    typeOfEntry = interview["typeOfEntry"] as? String ?: "",
                    ssbNumber = interview["ssbNumber"] as? String ?: "",
                    ssbPlace = interview["ssbPlace"] as? String ?: "",
                    date = interview["date"] as? String ?: "",
                    chestNumber = interview["chestNumber"] as? String ?: "",
                    batchNumber = interview["batchNumber"] as? String ?: ""
                )
            }
            
            // Parse work experience
            val workExpList = submissionData["workExperience"] as? List<*> ?: emptyList<Any>()
            val workExperience = workExpList.mapNotNull { workData ->
                val work = workData as? Map<*, *> ?: return@mapNotNull null
                WorkExperience(
                    id = work["id"] as? String ?: "",
                    company = work["company"] as? String ?: "",
                    role = work["role"] as? String ?: "",
                    duration = work["duration"] as? String ?: "",
                    description = work["description"] as? String ?: ""
                )
            }
            
            // Parse AI Score
            val aiScoreData = submissionData["aiPreliminaryScore"] as? Map<*, *>
            val aiScore = aiScoreData?.let {
                PIQAIScore(
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    personalInfoScore = (it["personalInfoScore"] as? Number)?.toFloat() ?: 0f,
                    familyInfoScore = (it["familyInfoScore"] as? Number)?.toFloat() ?: 0f,
                    motivationScore = (it["motivationScore"] as? Number)?.toFloat() ?: 0f,
                    selfAssessmentScore = (it["selfAssessmentScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String ?: "",
                    strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                    areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { a -> a as? String } ?: emptyList(),
                    completenessPercentage = (it["completenessPercentage"] as? Number)?.toInt() ?: 0,
                    clarityScore = (it["clarityScore"] as? Number)?.toFloat() ?: 0f,
                    consistencyScore = (it["consistencyScore"] as? Number)?.toFloat() ?: 0f,
                    analysisTimestamp = (it["analysisTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }

            PIQSubmission(
                id = submissionData["id"] as? String ?: "",
                userId = submissionData["userId"] as? String ?: "",
                testId = submissionData["testId"] as? String ?: "",
                
                // Header Section
                oirNumber = submissionData["oirNumber"] as? String ?: "",
                selectionBoard = submissionData["selectionBoard"] as? String ?: "",
                batchNumber = submissionData["batchNumber"] as? String ?: "",
                chestNumber = submissionData["chestNumber"] as? String ?: "",
                upscRollNumber = submissionData["upscRollNumber"] as? String ?: "",
                
                // Personal Information
                fullName = submissionData["fullName"] as? String ?: "",
                dateOfBirth = submissionData["dateOfBirth"] as? String ?: "",
                age = submissionData["age"]?.toString() ?: "",
                gender = submissionData["gender"] as? String ?: "",
                phone = submissionData["phone"] as? String ?: "",
                email = submissionData["email"] as? String ?: "",
                
                // Personal Details Table
                state = submissionData["state"] as? String ?: "",
                district = submissionData["district"] as? String ?: "",
                religion = submissionData["religion"] as? String ?: "",
                scStObcStatus = submissionData["scStObcStatus"] as? String ?: "",
                motherTongue = submissionData["motherTongue"] as? String ?: "",
                maritalStatus = submissionData["maritalStatus"] as? String ?: "",
                
                // Residence Information
                permanentAddress = submissionData["permanentAddress"] as? String ?: "",
                presentAddress = submissionData["presentAddress"] as? String ?: "",
                maximumResidence = submissionData["maximumResidence"] as? String ?: "",
                maximumResidencePopulation = submissionData["maximumResidencePopulation"] as? String ?: "",
                presentResidencePopulation = submissionData["presentResidencePopulation"] as? String ?: "",
                permanentResidencePopulation = submissionData["permanentResidencePopulation"] as? String ?: "",
                isDistrictHQ = submissionData["isDistrictHQ"] as? Boolean ?: false,
                
                // Physical Details
                height = submissionData["height"] as? String ?: "",
                weight = submissionData["weight"] as? String ?: "",
                
                // Father details
                fatherName = submissionData["fatherName"] as? String ?: "",
                fatherOccupation = submissionData["fatherOccupation"] as? String ?: "",
                fatherEducation = submissionData["fatherEducation"] as? String ?: "",
                fatherIncome = submissionData["fatherIncome"] as? String ?: "",
                
                // Mother details
                motherName = submissionData["motherName"] as? String ?: "",
                motherOccupation = submissionData["motherOccupation"] as? String ?: "",
                motherEducation = submissionData["motherEducation"] as? String ?: "",
                
                // Family Enhancement
                parentsAlive = submissionData["parentsAlive"] as? String ?: "",
                ageAtFatherDeath = submissionData["ageAtFatherDeath"] as? String ?: "",
                ageAtMotherDeath = submissionData["ageAtMotherDeath"] as? String ?: "",
                guardianName = submissionData["guardianName"] as? String ?: "",
                guardianOccupation = submissionData["guardianOccupation"] as? String ?: "",
                guardianEducation = submissionData["guardianEducation"] as? String ?: "",
                guardianIncome = submissionData["guardianIncome"] as? String ?: "",
                
                // Siblings
                siblings = siblings,
                
                // Occupation
                presentOccupation = submissionData["presentOccupation"] as? String ?: "",
                personalMonthlyIncome = submissionData["personalMonthlyIncome"] as? String ?: "",
                
                // Education
                education10th = education10th,
                education12th = education12th,
                educationGraduation = educationGraduation,
                educationPostGraduation = educationPostGraduation,
                
                // Activities
                hobbies = submissionData["hobbies"] as? String ?: "",
                sports = submissionData["sports"] as? String ?: "",
                sportsParticipation = sportsParticipation,
                extraCurricularActivities = extraCurricularActivities,
                positionsOfResponsibility = submissionData["positionsOfResponsibility"] as? String ?: "",
                
                // Work Experience
                workExperience = workExperience,
                
                // NCC Training
                nccTraining = nccTraining,
                
                // Service Selection
                natureOfCommission = submissionData["natureOfCommission"] as? String ?: "",
                choiceOfService = submissionData["choiceOfService"] as? String ?: "",
                chancesAvailed = submissionData["chancesAvailed"] as? String ?: "",
                
                // Previous Interviews
                previousInterviews = previousInterviews,
                
                // Motivation & Self Assessment
                whyDefenseForces = submissionData["whyDefenseForces"] as? String ?: "",
                strengths = submissionData["strengths"] as? String ?: "",
                weaknesses = submissionData["weaknesses"] as? String ?: "",
                
                // Metadata
                status = try {
                    SubmissionStatus.valueOf(submissionData["status"] as? String ?: "DRAFT")
                } catch (e: Exception) {
                    SubmissionStatus.DRAFT
                },
                submittedAt = (submissionData["submittedAt"] as? Number)?.toLong() ?: 0L,
                lastModifiedAt = (submissionData["lastModifiedAt"] as? Number)?.toLong() ?: 0L,
                gradedByInstructorId = submissionData["gradedByInstructorId"] as? String,
                gradingTimestamp = (submissionData["gradingTimestamp"] as? Number)?.toLong(),
                aiPreliminaryScore = aiScore
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ PIQ Result: Exception parsing submission", e)
            null
        }
    }
}

data class PIQSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: PIQSubmission? = null,
    val error: String? = null
)

