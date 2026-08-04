package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.Education
import com.ssbmax.shared.domain.model.ExtraCurricularActivity
import com.ssbmax.shared.domain.model.NCCTraining
import com.ssbmax.shared.domain.model.PIQAIScore
import com.ssbmax.shared.domain.model.PreviousInterview
import com.ssbmax.shared.domain.model.Sibling
import com.ssbmax.shared.domain.model.SportsParticipation
import com.ssbmax.shared.domain.model.WorkExperience
import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for [com.ssbmax.shared.domain.model.PIQSubmission]'s nested sections, split out
 * of `PIQSubmissionMappers.kt` to keep both files under the 300-line limit. Mirrors the Android
 * `PIQSubmission.toMap()`/`parsePIQSubmission` nested-map shapes field-for-field.
 */
@Serializable
internal data class SiblingDto(
    val id: String = "",
    val name: String = "",
    val age: String = "",
    val occupation: String = "",
    val education: String = "",
    val income: String = ""
)

internal fun Sibling.toDto() = SiblingDto(id, name, age, occupation, education, income)
internal fun SiblingDto.toDomain() = Sibling(id, name, age, occupation, education, income)

@Serializable
internal data class EducationDto(
    val level: String = "",
    val institution: String = "",
    val board: String = "",
    val stream: String = "",
    val year: String = "",
    val percentage: String = "",
    val cgpa: String = "",
    val mediumOfInstruction: String = "",
    val boarderDayScholar: String = "",
    val outstandingAchievement: String = ""
)

internal fun Education.toDto() = EducationDto(
    level, institution, board, stream, year, percentage, cgpa, mediumOfInstruction, boarderDayScholar, outstandingAchievement
)

internal fun EducationDto.toDomain() = Education(
    level, institution, board, stream, year, percentage, cgpa, mediumOfInstruction, boarderDayScholar, outstandingAchievement
)

@Serializable
internal data class WorkExperienceDto(
    val id: String = "",
    val company: String = "",
    val role: String = "",
    val duration: String = "",
    val description: String = ""
)

internal fun WorkExperience.toDto() = WorkExperienceDto(id, company, role, duration, description)
internal fun WorkExperienceDto.toDomain() = WorkExperience(id, company, role, duration, description)

@Serializable
internal data class NCCTrainingDto(
    val hasTraining: Boolean = false,
    val totalTraining: String = "",
    val wing: String = "",
    val division: String = "",
    val certificateObtained: String = ""
)

internal fun NCCTraining.toDto() = NCCTrainingDto(hasTraining, totalTraining, wing, division, certificateObtained)
internal fun NCCTrainingDto.toDomain() = NCCTraining(hasTraining, totalTraining, wing, division, certificateObtained)

@Serializable
internal data class SportsParticipationDto(
    val id: String = "",
    val sport: String = "",
    val period: String = "",
    val representedInstitution: String = "",
    val outstandingAchievement: String = ""
)

internal fun SportsParticipation.toDto() = SportsParticipationDto(id, sport, period, representedInstitution, outstandingAchievement)
internal fun SportsParticipationDto.toDomain() = SportsParticipation(id, sport, period, representedInstitution, outstandingAchievement)

@Serializable
internal data class ExtraCurricularActivityDto(
    val id: String = "",
    val activityName: String = "",
    val duration: String = "",
    val outstandingAchievement: String = ""
)

internal fun ExtraCurricularActivity.toDto() = ExtraCurricularActivityDto(id, activityName, duration, outstandingAchievement)
internal fun ExtraCurricularActivityDto.toDomain() = ExtraCurricularActivity(id, activityName, duration, outstandingAchievement)

@Serializable
internal data class PreviousInterviewDto(
    val id: String = "",
    val typeOfEntry: String = "",
    val ssbNumber: String = "",
    val ssbPlace: String = "",
    val date: String = "",
    val chestNumber: String = "",
    val batchNumber: String = ""
)

internal fun PreviousInterview.toDto() = PreviousInterviewDto(id, typeOfEntry, ssbNumber, ssbPlace, date, chestNumber, batchNumber)
internal fun PreviousInterviewDto.toDomain() = PreviousInterview(id, typeOfEntry, ssbNumber, ssbPlace, date, chestNumber, batchNumber)

@Serializable
internal data class PIQAIScoreDto(
    val overallScore: Float = 0f,
    val personalInfoScore: Float = 0f,
    val familyInfoScore: Float = 0f,
    val motivationScore: Float = 0f,
    val selfAssessmentScore: Float = 0f,
    val feedback: String = "",
    val strengths: List<String> = emptyList(),
    val areasForImprovement: List<String> = emptyList(),
    val completenessPercentage: Int = 0,
    val clarityScore: Float = 0f,
    val consistencyScore: Float = 0f,
    val analysisTimestamp: Long = 0L
)

internal fun PIQAIScore.toDto() = PIQAIScoreDto(
    overallScore, personalInfoScore, familyInfoScore, motivationScore, selfAssessmentScore, feedback,
    strengths, areasForImprovement, completenessPercentage, clarityScore, consistencyScore, analysisTimestamp
)

internal fun PIQAIScoreDto.toDomain() = PIQAIScore(
    overallScore, personalInfoScore, familyInfoScore, motivationScore, selfAssessmentScore, feedback,
    strengths, areasForImprovement, completenessPercentage, clarityScore, consistencyScore, analysisTimestamp
)
