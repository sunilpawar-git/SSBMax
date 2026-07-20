package com.ssbmax.shared.data.repository

/**
 * "Leadership Exposure", "SSB Journey", "Self-Assessment" section builders plus the
 * "Personalization Notes" heuristics for [PIQContextBuilder], split out purely to stay under this
 * repo's 300-line-per-file limit — see that file's class doc.
 */

internal fun buildLeadershipExposure(
    nccTraining: Map<String, Any>,
    positionsOfResponsibility: String
): String {
    val hasNCC = nccTraining["hasTraining"] as? Boolean ?: false
    val nccDetails = if (hasNCC) {
        val wing = nccTraining["wing"] as? String ?: ""
        val division = nccTraining["division"] as? String ?: ""
        val certificate = nccTraining["certificateObtained"] as? String ?: ""
        val totalTraining = nccTraining["totalTraining"] as? String ?: ""
        buildString {
            append("Yes")
            if (wing.isNotBlank()) append(" - $wing Wing")
            if (division.isNotBlank()) append(", $division Division")
            if (certificate.isNotBlank()) append("\n  Certificate: $certificate")
            if (totalTraining.isNotBlank()) append("\n  Training Duration: $totalTraining")
        }
    } else {
        "No NCC training"
    }

    return """
LEADERSHIP EXPOSURE:
- NCC Training: $nccDetails
- Positions of Responsibility: ${positionsOfResponsibility.ifBlank { "None mentioned" }}
    """.trimIndent()
}

internal fun buildSSBJourney(
    previousInterviews: List<Map<String, Any>>,
    choiceOfService: String,
    natureOfCommission: String,
    chancesAvailed: String
): String {
    val interviewSummary = if (previousInterviews.isNotEmpty()) {
        previousInterviews.mapIndexed { index, interview ->
            val entryType = interview["typeOfEntry"] as? String ?: ""
            val ssbPlace = interview["ssbPlace"] as? String ?: ""
            val date = interview["date"] as? String ?: ""
            buildString {
                append("${index + 1}. ")
                if (entryType.isNotBlank()) append("$entryType entry")
                if (ssbPlace.isNotBlank()) append(" at $ssbPlace")
                if (date.isNotBlank()) append(" ($date)")
            }
        }.joinToString("\n  ").ifBlank { "Details not provided" }
    } else {
        "First attempt (Freshie)"
    }

    val attemptContext = when {
        previousInterviews.isEmpty() -> "Fresh candidate - no prior SSB experience"
        previousInterviews.size == 1 -> "Repeater (1 previous attempt) - has SSB exposure"
        else -> "Multiple attempts (${previousInterviews.size}) - highly determined"
    }

    return """
SSB JOURNEY:
- Choice of Service: ${choiceOfService.ifBlank { "Not specified" }}
- Nature of Commission: ${natureOfCommission.ifBlank { "Not specified" }}
- Chances Availed: ${chancesAvailed.ifBlank { "Not specified" }}
- Previous SSB Attempts:
  $interviewSummary
- Candidate Type: $attemptContext
    """.trimIndent()
}

internal fun buildSelfAssessment(
    whyDefenseForces: String,
    strengths: String,
    weaknesses: String
): String {
    return """
SELF-ASSESSMENT:
- Why Defense Forces:
  ${whyDefenseForces.ifBlank { "Not provided" }}
- Stated Strengths:
  ${strengths.ifBlank { "Not provided" }}
- Acknowledged Weaknesses:
  ${weaknesses.ifBlank { "Not provided" }}
    """.trimIndent()
}

@Suppress("UNCHECKED_CAST")
internal fun buildPersonalizationNotes(data: Map<String, Any>): String {
    val notes = mutableListOf<String>()

    val nccTraining = data["nccTraining"] as? Map<String, Any>
    if (nccTraining?.get("hasTraining") == true) {
        val wing = nccTraining["wing"] as? String ?: ""
        val cert = nccTraining["certificateObtained"] as? String ?: ""
        notes.add("-> Has NCC background ($wing Wing, $cert) - explore leadership experiences")
    }

    val prevInterviews = (data["previousInterviews"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
    if (prevInterviews?.isNotEmpty() == true) {
        notes.add("-> Repeater candidate - ask about learning from previous attempt(s)")
    }

    val fatherOcc = data["fatherOccupation"] as? String ?: ""
    if (listOf("army", "navy", "air force", "forces").any { fatherOcc.contains(it, ignoreCase = true) }) {
        notes.add("-> Defense family background - explore influence and expectations")
    }

    val sports = (data["sportsParticipation"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
    val hasAchievements = sports?.any {
        (it["outstandingAchievement"] as? String)?.isNotBlank() == true ||
            (it["representedInstitution"] as? String)?.isNotBlank() == true
    } == true
    if (hasAchievements) {
        notes.add("-> Sports achievements present - ask about teamwork and competition")
    }

    val workExp = (data["workExperience"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
    if (workExp?.isNotEmpty() == true) {
        notes.add("-> Has work experience - explore professional challenges and growth")
    }

    val positions = data["positionsOfResponsibility"] as? String ?: ""
    if (positions.isNotBlank()) {
        notes.add("-> Has held leadership positions - probe leadership style and challenges")
    }

    val population = data["maximumResidencePopulation"] as? String ?: ""
    if (population.toIntOrNull()?.let { it < 50000 } == true ||
        population.contains("village", ignoreCase = true)
    ) {
        notes.add("-> Rural/small town background - ask about adaptability and exposure")
    }

    return if (notes.isNotEmpty()) {
        notes.joinToString("\n")
    } else {
        "-> Standard profile - use general SSB questioning approach"
    }
}
