package com.ssbmax.shared.data.repository

/**
 * "Education Journey", "Career & Work" and "Activities & Interests" section builders for
 * [PIQContextBuilder], split out purely to stay under this repo's 300-line-per-file limit — see
 * that file's class doc.
 */

internal fun formatEducationLevel(edu: Map<String, Any>, level: String): String {
    val institution = edu["institution"] as? String ?: ""
    val board = edu["board"] as? String ?: ""
    val year = edu["year"] as? String ?: ""
    val percentage = edu["percentage"] as? String ?: ""
    val cgpa = edu["cgpa"] as? String ?: ""
    val stream = edu["stream"] as? String ?: ""
    val medium = edu["mediumOfInstruction"] as? String ?: ""
    val boarderDay = edu["boarderDayScholar"] as? String ?: ""
    val achievement = edu["outstandingAchievement"] as? String ?: ""

    val scoreStr = when {
        percentage.isNotBlank() -> "$percentage%"
        cgpa.isNotBlank() -> "CGPA: $cgpa"
        else -> "Score not provided"
    }

    val details = listOfNotNull(
        if (institution.isNotBlank()) "Institution: $institution" else null,
        if (board.isNotBlank()) "Board: $board" else null,
        if (stream.isNotBlank()) "Stream: $stream" else null,
        if (year.isNotBlank()) "Year: $year" else null,
        "Performance: $scoreStr",
        if (medium.isNotBlank()) "Medium: $medium" else null,
        if (boarderDay.isNotBlank()) "Type: $boarderDay" else null,
        if (achievement.isNotBlank()) "Achievement: $achievement" else null
    )

    return if (details.isNotEmpty()) {
        "- $level:\n  " + details.joinToString("\n  - ", prefix = "- ")
    } else {
        "- $level: Not provided"
    }
}

internal fun buildEducationJourney(
    education10th: Map<String, Any>,
    education12th: Map<String, Any>,
    educationGraduation: Map<String, Any>,
    educationPostGraduation: Map<String, Any>
): String {
    val hasPostGrad = (educationPostGraduation["institution"] as? String)?.isNotBlank() == true

    return """
EDUCATION JOURNEY:
${formatEducationLevel(education10th, "10th Standard")}
${formatEducationLevel(education12th, "12th Standard")}
${formatEducationLevel(educationGraduation, "Graduation")}${
        if (hasPostGrad) "\n${formatEducationLevel(educationPostGraduation, "Post-Graduation")}" else ""
    }
    """.trimIndent()
}

internal fun buildCareerAndWork(
    presentOccupation: String,
    personalMonthlyIncome: String,
    workExperience: List<Map<String, Any>>
): String {
    val workSummary = if (workExperience.isNotEmpty()) {
        workExperience.mapNotNull { exp ->
            val company = exp["company"] as? String ?: ""
            val role = exp["role"] as? String ?: ""
            val duration = exp["duration"] as? String ?: ""
            val description = exp["description"] as? String ?: ""
            if (company.isNotBlank() || role.isNotBlank()) {
                buildString {
                    append("- $role")
                    if (company.isNotBlank()) append(" at $company")
                    if (duration.isNotBlank()) append(" ($duration)")
                    if (description.isNotBlank()) append("\n    Details: $description")
                }
            } else null
        }.joinToString("\n  ").ifBlank { "No details provided" }
    } else {
        "No prior work experience"
    }

    return """
CAREER & WORK:
- Current Occupation: ${presentOccupation.ifBlank { "Not specified (likely student/fresher)" }}
- Monthly Income: ${personalMonthlyIncome.ifBlank { "Not applicable / Not provided" }}
- Work Experience:
  $workSummary
    """.trimIndent()
}

internal fun buildActivitiesAndInterests(
    hobbies: String,
    sports: String,
    sportsParticipation: List<Map<String, Any>>,
    extraCurricularActivities: List<Map<String, Any>>
): String {
    val sportsSummary = if (sportsParticipation.isNotEmpty()) {
        sportsParticipation.mapNotNull { sp ->
            val sport = sp["sport"] as? String ?: ""
            val period = sp["period"] as? String ?: ""
            val represented = sp["representedInstitution"] as? String ?: ""
            val achievement = sp["outstandingAchievement"] as? String ?: ""
            if (sport.isNotBlank()) {
                buildString {
                    append("- $sport")
                    if (period.isNotBlank()) append(" (played: $period)")
                    if (represented.isNotBlank()) append("\n    Represented: $represented")
                    if (achievement.isNotBlank()) append("\n    Achievement: $achievement")
                }
            } else null
        }.joinToString("\n  ").ifBlank { sports.ifBlank { "Not specified" } }
    } else {
        sports.ifBlank { "Not specified" }
    }

    val ecaSummary = if (extraCurricularActivities.isNotEmpty()) {
        extraCurricularActivities.mapNotNull { eca ->
            val activity = eca["activityName"] as? String ?: ""
            val duration = eca["duration"] as? String ?: ""
            val achievement = eca["outstandingAchievement"] as? String ?: ""
            if (activity.isNotBlank()) {
                buildString {
                    append("- $activity")
                    if (duration.isNotBlank()) append(" ($duration)")
                    if (achievement.isNotBlank()) append(" - $achievement")
                }
            } else null
        }.joinToString("\n  ").ifBlank { "None listed" }
    } else {
        "None listed"
    }

    return """
ACTIVITIES & INTERESTS:
- Hobbies: ${hobbies.ifBlank { "Not specified" }}
- Sports:
  $sportsSummary
- Extra-Curricular Activities:
  $ecaSummary
    """.trimIndent()
}
