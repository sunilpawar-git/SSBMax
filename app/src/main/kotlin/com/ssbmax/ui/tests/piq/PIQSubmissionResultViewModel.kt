package com.ssbmax.ui.tests.piq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.Education
import com.ssbmax.core.domain.model.ExtraCurricularActivity
import com.ssbmax.core.domain.model.NCCTraining
import com.ssbmax.core.domain.model.PIQAIScore
import com.ssbmax.core.domain.model.PIQSubmission
import com.ssbmax.core.domain.model.PreviousInterview
import com.ssbmax.core.domain.model.Sibling
import com.ssbmax.core.domain.model.SportsParticipation
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.WorkExperience
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
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
            _uiState.update { it.copy(isLoading = true) }

            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        ErrorLogger.logTestError(
                            throwable = IllegalStateException("Submission data is null"),
                            description = "PIQ submission not found: $submissionId",
                            testType = "PIQ"
                        )
                        _uiState.update { it.copy(isLoading = false, submission = null, error = "Submission not found") }
                        return@onSuccess
                    }

                    val submission = parsePIQSubmission(data)

                    if (submission == null) {
                        ErrorLogger.logTestError(
                            throwable = IllegalStateException("Failed to parse submission"),
                            description = "PIQ submission parsing failed: $submissionId",
                            testType = "PIQ"
                        )
                    }

                    _uiState.update { it.copy(isLoading = false, submission = submission,
                        error = if (submission == null) "Submission not found" else null) }
                }
                .onFailure { error ->
                    ErrorLogger.logTestError(error, "Failed to load PIQ submission result", "PIQ")
                    _uiState.update { it.copy(isLoading = false,
                        error = error.message ?: "Failed to load submission") }
                }
        }
    }

    private fun parsePIQSubmission(data: Map<String, Any>): PIQSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null

            var submission = PIQSubmission(userId = submissionData["userId"] as? String ?: "")
            submission = applyHeaderSection(submission, submissionData)
            submission = applyPersonalInfoSection(submission, submissionData)
            submission = applyResidenceSection(submission, submissionData)
            submission = applyParentDetailsSection(submission, submissionData)
            submission = applyGuardianAndSiblingsSection(submission, submissionData)
            submission = applyEducationAndActivitiesSection(submission, submissionData)
            submission = applyServiceAndMetadataSection(submission, submissionData)
            submission
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing PIQ submission data", "PIQ")
            null
        }
    }

    private fun applyHeaderSection(submission: PIQSubmission, submissionData: Map<*, *>): PIQSubmission {
        return submission.copy(
            id = submissionData["id"] as? String ?: submission.id,
            testId = submissionData["testId"] as? String ?: submission.testId,
            oirNumber = submissionData["oirNumber"] as? String ?: "",
            selectionBoard = submissionData["selectionBoard"] as? String ?: "",
            batchNumber = submissionData["batchNumber"] as? String ?: "",
            chestNumber = submissionData["chestNumber"] as? String ?: "",
            upscRollNumber = submissionData["upscRollNumber"] as? String ?: ""
        )
    }

    private fun applyPersonalInfoSection(submission: PIQSubmission, submissionData: Map<*, *>): PIQSubmission {
        return submission.copy(
            fullName = submissionData["fullName"] as? String ?: "",
            dateOfBirth = submissionData["dateOfBirth"] as? String ?: "",
            age = submissionData["age"]?.toString() ?: "",
            gender = submissionData["gender"] as? String ?: "",
            phone = submissionData["phone"] as? String ?: "",
            email = submissionData["email"] as? String ?: "",
            state = submissionData["state"] as? String ?: "",
            district = submissionData["district"] as? String ?: "",
            religion = submissionData["religion"] as? String ?: "",
            scStObcStatus = submissionData["scStObcStatus"] as? String ?: "",
            motherTongue = submissionData["motherTongue"] as? String ?: "",
            maritalStatus = submissionData["maritalStatus"] as? String ?: ""
        )
    }

    private fun applyResidenceSection(submission: PIQSubmission, submissionData: Map<*, *>): PIQSubmission {
        return submission.copy(
            permanentAddress = submissionData["permanentAddress"] as? String ?: "",
            presentAddress = submissionData["presentAddress"] as? String ?: "",
            maximumResidence = submissionData["maximumResidence"] as? String ?: "",
            maximumResidencePopulation = submissionData["maximumResidencePopulation"] as? String ?: "",
            presentResidencePopulation = submissionData["presentResidencePopulation"] as? String ?: "",
            permanentResidencePopulation = submissionData["permanentResidencePopulation"] as? String ?: "",
            isDistrictHQ = submissionData["isDistrictHQ"] as? Boolean ?: false,
            height = submissionData["height"] as? String ?: "",
            weight = submissionData["weight"] as? String ?: ""
        )
    }

    private fun applyParentDetailsSection(submission: PIQSubmission, submissionData: Map<*, *>): PIQSubmission {
        return submission.copy(
            fatherName = submissionData["fatherName"] as? String ?: "",
            fatherOccupation = submissionData["fatherOccupation"] as? String ?: "",
            fatherEducation = submissionData["fatherEducation"] as? String ?: "",
            fatherIncome = submissionData["fatherIncome"] as? String ?: "",
            motherName = submissionData["motherName"] as? String ?: "",
            motherOccupation = submissionData["motherOccupation"] as? String ?: "",
            motherEducation = submissionData["motherEducation"] as? String ?: "",
            parentsAlive = submissionData["parentsAlive"] as? String ?: "",
            ageAtFatherDeath = submissionData["ageAtFatherDeath"] as? String ?: "",
            ageAtMotherDeath = submissionData["ageAtMotherDeath"] as? String ?: ""
        )
    }

    private fun applyGuardianAndSiblingsSection(submission: PIQSubmission, submissionData: Map<*, *>): PIQSubmission {
        return submission.copy(
            guardianName = submissionData["guardianName"] as? String ?: "",
            guardianOccupation = submissionData["guardianOccupation"] as? String ?: "",
            guardianEducation = submissionData["guardianEducation"] as? String ?: "",
            guardianIncome = submissionData["guardianIncome"] as? String ?: "",
            siblings = parseSiblings(submissionData["siblings"] as? List<*>),
            presentOccupation = submissionData["presentOccupation"] as? String ?: "",
            personalMonthlyIncome = submissionData["personalMonthlyIncome"] as? String ?: ""
        )
    }

    private fun applyEducationAndActivitiesSection(submission: PIQSubmission, submissionData: Map<*, *>): PIQSubmission {
        return submission.copy(
            education10th = parseEducation10th(submissionData["education10th"] as? Map<*, *>),
            education12th = parseEducation12th(submissionData["education12th"] as? Map<*, *>),
            educationGraduation = parseEducationGraduation(submissionData["educationGraduation"] as? Map<*, *>),
            educationPostGraduation = parseEducationPostGraduation(submissionData["educationPostGraduation"] as? Map<*, *>),
            hobbies = submissionData["hobbies"] as? String ?: "",
            sports = submissionData["sports"] as? String ?: "",
            sportsParticipation = parseSportsParticipation(submissionData["sportsParticipation"] as? List<*>),
            extraCurricularActivities = parseExtraCurricularActivities(submissionData["extraCurricularActivities"] as? List<*>),
            positionsOfResponsibility = submissionData["positionsOfResponsibility"] as? String ?: "",
            workExperience = parseWorkExperience(submissionData["workExperience"] as? List<*>),
            nccTraining = parseNccTraining(submissionData["nccTraining"] as? Map<*, *>)
        )
    }

    private fun applyServiceAndMetadataSection(submission: PIQSubmission, submissionData: Map<*, *>): PIQSubmission {
        return submission.copy(
            natureOfCommission = submissionData["natureOfCommission"] as? String ?: "",
            choiceOfService = submissionData["choiceOfService"] as? String ?: "",
            chancesAvailed = submissionData["chancesAvailed"] as? String ?: "",
            previousInterviews = parsePreviousInterviews(submissionData["previousInterviews"] as? List<*>),
            whyDefenseForces = submissionData["whyDefenseForces"] as? String ?: "",
            strengths = submissionData["strengths"] as? String ?: "",
            weaknesses = submissionData["weaknesses"] as? String ?: "",
            status = try {
                SubmissionStatus.valueOf(submissionData["status"] as? String ?: "DRAFT")
            } catch (e: Exception) {
                SubmissionStatus.DRAFT
            },
            submittedAt = (submissionData["submittedAt"] as? Number)?.toLong() ?: 0L,
            lastModifiedAt = (submissionData["lastModifiedAt"] as? Number)?.toLong() ?: 0L,
            gradedByInstructorId = submissionData["gradedByInstructorId"] as? String,
            gradingTimestamp = (submissionData["gradingTimestamp"] as? Number)?.toLong(),
            aiPreliminaryScore = parseAiScore(submissionData["aiPreliminaryScore"] as? Map<*, *>)
        )
    }

    private fun parseSiblings(siblingsList: List<*>?): List<Sibling> {
        return (siblingsList ?: emptyList<Any>()).mapNotNull { siblingData ->
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
    }

    private fun parseEducation10th(eduMap: Map<*, *>?): Education {
        return eduMap?.let {
            Education(
                level = it["level"] as? String ?: "",
                institution = it["institution"] as? String ?: "",
                board = it["board"] as? String ?: "",
                stream = "",
                year = it["year"]?.toString() ?: "",
                percentage = it["percentage"]?.toString() ?: "",
                cgpa = "",
                mediumOfInstruction = it["mediumOfInstruction"] as? String ?: "",
                boarderDayScholar = it["boarderDayScholar"] as? String ?: "",
                outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
            )
        } ?: Education(level = "10th")
    }

    private fun parseEducation12th(eduMap: Map<*, *>?): Education {
        return eduMap?.let {
            Education(
                level = it["level"] as? String ?: "",
                institution = it["institution"] as? String ?: "",
                board = it["board"] as? String ?: "",
                stream = it["stream"] as? String ?: "",
                year = it["year"]?.toString() ?: "",
                percentage = it["percentage"]?.toString() ?: "",
                cgpa = "",
                mediumOfInstruction = it["mediumOfInstruction"] as? String ?: "",
                boarderDayScholar = it["boarderDayScholar"] as? String ?: "",
                outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
            )
        } ?: Education(level = "12th")
    }

    private fun parseEducationGraduation(eduMap: Map<*, *>?): Education {
        return eduMap?.let {
            Education(
                level = it["level"] as? String ?: "",
                institution = it["institution"] as? String ?: "",
                board = it["board"] as? String ?: "",
                stream = "",
                year = it["year"]?.toString() ?: "",
                percentage = "",
                cgpa = it["cgpa"]?.toString() ?: "",
                mediumOfInstruction = it["mediumOfInstruction"] as? String ?: "",
                boarderDayScholar = it["boarderDayScholar"] as? String ?: "",
                outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
            )
        } ?: Education(level = "Graduation")
    }

    private fun parseEducationPostGraduation(eduMap: Map<*, *>?): Education {
        return eduMap?.let {
            Education(
                level = it["level"] as? String ?: "",
                institution = it["institution"] as? String ?: "",
                board = it["board"] as? String ?: "",
                stream = "",
                year = it["year"]?.toString() ?: "",
                percentage = "",
                cgpa = it["cgpa"]?.toString() ?: "",
                mediumOfInstruction = it["mediumOfInstruction"] as? String ?: "",
                boarderDayScholar = it["boarderDayScholar"] as? String ?: "",
                outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
            )
        } ?: Education(level = "Post-Graduation")
    }

    private fun parseNccTraining(nccMap: Map<*, *>?): NCCTraining {
        return nccMap?.let {
            NCCTraining(
                hasTraining = it["hasTraining"] as? Boolean ?: false,
                totalTraining = it["totalTraining"] as? String ?: "",
                wing = it["wing"] as? String ?: "",
                division = it["division"] as? String ?: "",
                certificateObtained = it["certificateObtained"] as? String ?: ""
            )
        } ?: NCCTraining()
    }

    private fun parseSportsParticipation(sportsList: List<*>?): List<SportsParticipation> {
        return (sportsList ?: emptyList<Any>()).mapNotNull { sportData ->
            val sport = sportData as? Map<*, *> ?: return@mapNotNull null
            SportsParticipation(
                id = sport["id"] as? String ?: "",
                sport = sport["sport"] as? String ?: "",
                period = sport["period"] as? String ?: "",
                representedInstitution = sport["representedInstitution"] as? String ?: "",
                outstandingAchievement = sport["outstandingAchievement"] as? String ?: ""
            )
        }
    }

    private fun parseExtraCurricularActivities(activitiesList: List<*>?): List<ExtraCurricularActivity> {
        return (activitiesList ?: emptyList<Any>()).mapNotNull { activityData ->
            val activity = activityData as? Map<*, *> ?: return@mapNotNull null
            ExtraCurricularActivity(
                id = activity["id"] as? String ?: "",
                activityName = activity["activityName"] as? String ?: "",
                duration = activity["duration"] as? String ?: "",
                outstandingAchievement = activity["outstandingAchievement"] as? String ?: ""
            )
        }
    }

    private fun parsePreviousInterviews(interviewsList: List<*>?): List<PreviousInterview> {
        return (interviewsList ?: emptyList<Any>()).mapNotNull { interviewData ->
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
    }

    private fun parseWorkExperience(workExpList: List<*>?): List<WorkExperience> {
        return (workExpList ?: emptyList<Any>()).mapNotNull { workData ->
            val work = workData as? Map<*, *> ?: return@mapNotNull null
            WorkExperience(
                id = work["id"] as? String ?: "",
                company = work["company"] as? String ?: "",
                role = work["role"] as? String ?: "",
                duration = work["duration"] as? String ?: "",
                description = work["description"] as? String ?: ""
            )
        }
    }

    private fun parseAiScore(aiScoreData: Map<*, *>?): PIQAIScore? {
        return aiScoreData?.let {
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
    }
}

data class PIQSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: PIQSubmission? = null,
    val error: String? = null
)

