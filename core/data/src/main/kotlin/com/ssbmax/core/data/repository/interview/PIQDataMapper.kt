package com.ssbmax.core.data.repository.interview

import android.util.Log

/**
 * Mapper for converting PIQ data to AI-friendly formats
 *
 * Handles conversion of PIQ submission maps to structured text
 * optimized for Gemini AI question generation.
 *
 * Key Features:
 * - Extracts ALL 60+ PIQ fields (not just 10)
 * - Organizes data into meaningful categories for SSB context
 * - Derives insights from data (e.g., urban/rural, family context)
 * - Identifies rich areas for personalized questioning
 */
class PIQDataMapper {

    companion object {
        private const val TAG = "PIQDataMapper"
    }

    private fun getString(data: Map<String, Any>, key: String): String = (data[key] as? String)?.trim() ?: ""
    private fun getBoolean(data: Map<String, Any>, key: String): Boolean = data[key] as? Boolean ?: false

    @Suppress("UNCHECKED_CAST")
    private fun getMap(data: Map<String, Any>, key: String): Map<String, Any> =
        (data[key] as? Map<String, Any>) ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    private fun getListOfMaps(data: Map<String, Any>, key: String): List<Map<String, Any>> =
        (data[key] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

    /**
     * Build comprehensive PIQ context for AI question generation
     *
     * Extracts ALL relevant PIQ fields organized into meaningful categories:
     * - Personal Background
     * - Family Environment
     * - Education Journey
     * - Career & Work
     * - Activities & Interests
     * - Leadership Exposure
     * - SSB Journey
     * - Self-Assessment
     *
     * @param piqMap The PIQ submission data map from Firestore
     * @return Structured text context for AI consumption
     */
    fun buildComprehensivePIQContext(piqMap: Map<String, Any>): String {
        return try {
            @Suppress("UNCHECKED_CAST")
            val data = (piqMap["data"] as? Map<String, Any>) ?: piqMap

            """
CANDIDATE PROFILE
=================

${buildPersonalBackground(data)}

${buildFamilyEnvironment(data)}

${buildEducationJourney(data)}

${buildCareerAndWork(data)}

${buildActivitiesAndInterests(data)}

${buildLeadershipExposure(data)}

${buildSSBJourney(data)}

${buildSelfAssessment(data)}

PERSONALIZATION NOTES:
${buildPersonalizationNotes(data)}
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(TAG, "Error building comprehensive PIQ context", e)
            "Error processing PIQ data. Basic info only."
        }
    }

    private fun buildPersonalBackground(data: Map<String, Any>): String {
        val maxResidencePopulation = getString(data, "maximumResidencePopulation")
        val permanentAddress = getString(data, "permanentAddress")
        val presentAddress = getString(data, "presentAddress")
        val isDistrictHQ = getBoolean(data, "isDistrictHQ")

        val residenceType = deriveResidenceType(maxResidencePopulation)
        val mobilityContext = if (permanentAddress != presentAddress && presentAddress.isNotBlank()) {
            "Has relocated from permanent address"
        } else {
            "Living at permanent address"
        }

        return """
PERSONAL BACKGROUND:
- Name: ${getString(data, "fullName").ifBlank { "Not provided" }}
- Age: ${getString(data, "age").ifBlank { "Not provided" }}
- Gender: ${getString(data, "gender").ifBlank { "Not provided" }}
- From: ${getString(data, "district").ifBlank { "Unknown district" }}, ${getString(data, "state").ifBlank { "Unknown state" }}
- Background: $residenceType${if (isDistrictHQ) " (District HQ)" else ""}
- Marital Status: ${getString(data, "maritalStatus").ifBlank { "Not provided" }}
- Religion: ${getString(data, "religion").ifBlank { "Not provided" }}
- Mother Tongue: ${getString(data, "motherTongue").ifBlank { "Not provided" }}
- Mobility: $mobilityContext
- Physical: Height ${getString(data, "height").ifBlank { "N/A" }}m, Weight ${getString(data, "weight").ifBlank { "N/A" }}kg
        """.trimIndent()
    }

    private fun deriveResidenceType(population: String): String {
        return when {
            population.isBlank() -> "Unknown background"
            population.contains("metro", ignoreCase = true) -> "Metropolitan city"
            population.contains("lakh", ignoreCase = true) -> "Large city"
            population.contains("50000", ignoreCase = true) ||
                population.contains("50,000", ignoreCase = true) -> "Town"
            population.toIntOrNull()?.let { it > 100000 } == true -> "City"
            population.toIntOrNull()?.let { it > 50000 } == true -> "Town"
            population.toIntOrNull()?.let { it > 10000 } == true -> "Small town"
            else -> "Rural/Village background"
        }
    }

    private fun buildFamilyEnvironment(data: Map<String, Any>): String {
        val fatherName = getString(data, "fatherName")
        val fatherOccupation = getString(data, "fatherOccupation")
        val motherName = getString(data, "motherName")
        val motherOccupation = getString(data, "motherOccupation")
        val parentsAlive = getString(data, "parentsAlive")
        val ageAtFatherDeath = getString(data, "ageAtFatherDeath")
        val ageAtMotherDeath = getString(data, "ageAtMotherDeath")
        val guardianName = getString(data, "guardianName")

        val familyContext = deriveFamilyContext(
            fatherOccupation, motherOccupation, parentsAlive,
            ageAtFatherDeath, ageAtMotherDeath
        )
        val siblingSummary = buildSiblingSummary(getListOfMaps(data, "siblings"))

        return """
FAMILY ENVIRONMENT:
- Father: ${fatherName.ifBlank { "Not provided" }}
  • Occupation: ${fatherOccupation.ifBlank { "Not provided" }}
  • Education: ${getString(data, "fatherEducation").ifBlank { "Not provided" }}
  • Income: ${getString(data, "fatherIncome").ifBlank { "Not provided" }}
- Mother: ${motherName.ifBlank { "Not provided" }}
  • Occupation: ${motherOccupation.ifBlank { "Not provided" }}
  • Education: ${getString(data, "motherEducation").ifBlank { "Not provided" }}
- Parents Status: ${parentsAlive.ifBlank { "Both alive (assumed)" }}${
            if (ageAtFatherDeath.isNotBlank()) "\n  • Lost father at age $ageAtFatherDeath" else ""
        }${
            if (ageAtMotherDeath.isNotBlank()) "\n  • Lost mother at age $ageAtMotherDeath" else ""
        }${
            if (guardianName.isNotBlank()) "\n- Guardian: $guardianName (${getString(data, "guardianOccupation")})" else ""
        }
- Siblings: $siblingSummary
- Family Context: $familyContext
        """.trimIndent()
    }

    private fun buildSiblingSummary(siblings: List<Map<String, Any>>): String {
        if (siblings.isEmpty()) return "Only child / No siblings listed"
        return siblings.mapNotNull { sibling ->
            val name = sibling["name"] as? String ?: ""
            val age = sibling["age"] as? String ?: ""
            val occupation = sibling["occupation"] as? String ?: ""
            if (name.isNotBlank()) {
                "$name (${age.ifBlank { "age unknown" }}, ${occupation.ifBlank { "occupation unknown" }})"
            } else null
        }.joinToString("; ").ifBlank { "Details not provided" }
    }

    private fun deriveFamilyContext(
        fatherOccupation: String,
        motherOccupation: String,
        parentsAlive: String,
        ageAtFatherDeath: String,
        ageAtMotherDeath: String
    ): String {
        val contexts = mutableListOf<String>()

        // Check for defense background
        val defenseKeywords = listOf("army", "navy", "air force", "military", "forces", "jco", "nco", "officer")
        if (defenseKeywords.any { fatherOccupation.contains(it, ignoreCase = true) }) {
            contexts.add("Defense family background")
        }

        // Check for single parent
        if (parentsAlive.contains("only", ignoreCase = true) ||
            ageAtFatherDeath.isNotBlank() || ageAtMotherDeath.isNotBlank()) {
            contexts.add("Single parent/guardian upbringing")
        }

        // Check for working mother
        if (motherOccupation.isNotBlank() &&
            !motherOccupation.contains("housewife", ignoreCase = true) &&
            !motherOccupation.contains("homemaker", ignoreCase = true)) {
            contexts.add("Working mother")
        }

        // Check for government service
        val govtKeywords = listOf("govt", "government", "psu", "public sector", "ias", "ips")
        if (govtKeywords.any { fatherOccupation.contains(it, ignoreCase = true) }) {
            contexts.add("Government service family")
        }

        // Check for business family
        if (fatherOccupation.contains("business", ignoreCase = true) ||
            fatherOccupation.contains("entrepreneur", ignoreCase = true)) {
            contexts.add("Business family")
        }

        return contexts.joinToString(", ").ifBlank { "Standard family environment" }
    }

    private fun educationScoreString(percentage: String, cgpa: String): String = when {
        percentage.isNotBlank() -> "$percentage%"
        cgpa.isNotBlank() -> "CGPA: $cgpa"
        else -> "Score not provided"
    }

    private data class EducationRawFields(
        val institution: String,
        val board: String,
        val year: String,
        val percentage: String,
        val cgpa: String,
        val stream: String,
        val medium: String,
        val boarderDay: String,
        val achievement: String
    )

    private fun educationRawFields(edu: Map<String, Any>) = EducationRawFields(
        institution = edu["institution"] as? String ?: "",
        board = edu["board"] as? String ?: "",
        year = edu["year"] as? String ?: "",
        percentage = edu["percentage"] as? String ?: "",
        cgpa = edu["cgpa"] as? String ?: "",
        stream = edu["stream"] as? String ?: "",
        medium = edu["mediumOfInstruction"] as? String ?: "",
        boarderDay = edu["boarderDayScholar"] as? String ?: "",
        achievement = edu["outstandingAchievement"] as? String ?: ""
    )

    private fun educationDetails(edu: Map<String, Any>): List<String> {
        val f = educationRawFields(edu)
        return listOfNotNull(
            if (f.institution.isNotBlank()) "Institution: ${f.institution}" else null,
            if (f.board.isNotBlank()) "Board: ${f.board}" else null,
            if (f.stream.isNotBlank()) "Stream: ${f.stream}" else null,
            if (f.year.isNotBlank()) "Year: ${f.year}" else null,
            "Performance: ${educationScoreString(f.percentage, f.cgpa)}",
            if (f.medium.isNotBlank()) "Medium: ${f.medium}" else null,
            if (f.boarderDay.isNotBlank()) "Type: ${f.boarderDay}" else null,
            if (f.achievement.isNotBlank()) "Achievement: ${f.achievement}" else null
        )
    }

    private fun formatEducation(edu: Map<String, Any>, level: String): String {
        val details = educationDetails(edu)
        return if (details.isNotEmpty()) {
            "- $level:\n  " + details.joinToString("\n  • ", prefix = "• ")
        } else {
            "- $level: Not provided"
        }
    }

    private fun buildEducationJourney(data: Map<String, Any>): String {
        val education10th = getMap(data, "education10th")
        val education12th = getMap(data, "education12th")
        val educationGraduation = getMap(data, "educationGraduation")
        val educationPostGraduation = getMap(data, "educationPostGraduation")
        val hasPostGrad = (educationPostGraduation["institution"] as? String)?.isNotBlank() == true

        return """
EDUCATION JOURNEY:
${formatEducation(education10th, "10th Standard")}
${formatEducation(education12th, "12th Standard")}
${formatEducation(educationGraduation, "Graduation")}${
            if (hasPostGrad) "\n${formatEducation(educationPostGraduation, "Post-Graduation")}" else ""
        }
        """.trimIndent()
    }

    private fun buildWorkExperienceSummary(workExperience: List<Map<String, Any>>): String {
        if (workExperience.isEmpty()) return "No prior work experience"
        return workExperience.mapNotNull { exp ->
            val company = exp["company"] as? String ?: ""
            val role = exp["role"] as? String ?: ""
            val duration = exp["duration"] as? String ?: ""
            val description = exp["description"] as? String ?: ""
            if (company.isNotBlank() || role.isNotBlank()) {
                buildString {
                    append("• $role")
                    if (company.isNotBlank()) append(" at $company")
                    if (duration.isNotBlank()) append(" ($duration)")
                    if (description.isNotBlank()) append("\n    Details: $description")
                }
            } else null
        }.joinToString("\n  ").ifBlank { "No details provided" }
    }

    private fun buildCareerAndWork(data: Map<String, Any>): String {
        val workSummary = buildWorkExperienceSummary(getListOfMaps(data, "workExperience"))

        return """
CAREER & WORK:
- Current Occupation: ${getString(data, "presentOccupation").ifBlank { "Not specified (likely student/fresher)" }}
- Monthly Income: ${getString(data, "personalMonthlyIncome").ifBlank { "Not applicable / Not provided" }}
- Work Experience:
  $workSummary
        """.trimIndent()
    }

    private fun buildSportsSummary(sportsParticipation: List<Map<String, Any>>, sportsFallback: String): String {
        if (sportsParticipation.isEmpty()) return sportsFallback.ifBlank { "Not specified" }
        return sportsParticipation.mapNotNull { sp ->
            val sport = sp["sport"] as? String ?: ""
            val period = sp["period"] as? String ?: ""
            val represented = sp["representedInstitution"] as? String ?: ""
            val achievement = sp["outstandingAchievement"] as? String ?: ""
            if (sport.isNotBlank()) {
                buildString {
                    append("• $sport")
                    if (period.isNotBlank()) append(" (played: $period)")
                    if (represented.isNotBlank()) append("\n    Represented: $represented")
                    if (achievement.isNotBlank()) append("\n    Achievement: $achievement")
                }
            } else null
        }.joinToString("\n  ").ifBlank { sportsFallback.ifBlank { "Not specified" } }
    }

    private fun buildECASummary(extraCurricularActivities: List<Map<String, Any>>): String {
        if (extraCurricularActivities.isEmpty()) return "None listed"
        return extraCurricularActivities.mapNotNull { eca ->
            val activity = eca["activityName"] as? String ?: ""
            val duration = eca["duration"] as? String ?: ""
            val achievement = eca["outstandingAchievement"] as? String ?: ""
            if (activity.isNotBlank()) {
                buildString {
                    append("• $activity")
                    if (duration.isNotBlank()) append(" ($duration)")
                    if (achievement.isNotBlank()) append(" - $achievement")
                }
            } else null
        }.joinToString("\n  ").ifBlank { "None listed" }
    }

    private fun buildActivitiesAndInterests(data: Map<String, Any>): String {
        val sportsSummary = buildSportsSummary(getListOfMaps(data, "sportsParticipation"), getString(data, "sports"))
        val ecaSummary = buildECASummary(getListOfMaps(data, "extraCurricularActivities"))

        return """
ACTIVITIES & INTERESTS:
- Hobbies: ${getString(data, "hobbies").ifBlank { "Not specified" }}
- Sports:
  $sportsSummary
- Extra-Curricular Activities:
  $ecaSummary
        """.trimIndent()
    }

    private fun buildNCCDetails(nccTraining: Map<String, Any>): String {
        val hasNCC = nccTraining["hasTraining"] as? Boolean ?: false
        if (!hasNCC) return "No NCC training"

        val wing = nccTraining["wing"] as? String ?: ""
        val division = nccTraining["division"] as? String ?: ""
        val certificate = nccTraining["certificateObtained"] as? String ?: ""
        val totalTraining = nccTraining["totalTraining"] as? String ?: ""
        return buildString {
            append("Yes")
            if (wing.isNotBlank()) append(" - $wing Wing")
            if (division.isNotBlank()) append(", $division Division")
            if (certificate.isNotBlank()) append("\n  Certificate: $certificate")
            if (totalTraining.isNotBlank()) append("\n  Training Duration: $totalTraining")
        }
    }

    private fun buildLeadershipExposure(data: Map<String, Any>): String {
        val nccDetails = buildNCCDetails(getMap(data, "nccTraining"))

        return """
LEADERSHIP EXPOSURE:
- NCC Training: $nccDetails
- Positions of Responsibility: ${getString(data, "positionsOfResponsibility").ifBlank { "None mentioned" }}
        """.trimIndent()
    }

    private fun buildInterviewSummary(previousInterviews: List<Map<String, Any>>): String {
        if (previousInterviews.isEmpty()) return "First attempt (Freshie)"
        return previousInterviews.mapIndexed { index, interview ->
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
    }

    private fun deriveAttemptContext(previousInterviews: List<Map<String, Any>>): String = when {
        previousInterviews.isEmpty() -> "Fresh candidate - no prior SSB experience"
        previousInterviews.size == 1 -> "Repeater (1 previous attempt) - has SSB exposure"
        else -> "Multiple attempts (${previousInterviews.size}) - highly determined"
    }

    private fun buildSSBJourney(data: Map<String, Any>): String {
        val previousInterviews = getListOfMaps(data, "previousInterviews")
        val interviewSummary = buildInterviewSummary(previousInterviews)
        val attemptContext = deriveAttemptContext(previousInterviews)

        return """
SSB JOURNEY:
- Choice of Service: ${getString(data, "choiceOfService").ifBlank { "Not specified" }}
- Nature of Commission: ${getString(data, "natureOfCommission").ifBlank { "Not specified" }}
- Chances Availed: ${getString(data, "chancesAvailed").ifBlank { "Not specified" }}
- Previous SSB Attempts:
  $interviewSummary
- Candidate Type: $attemptContext
        """.trimIndent()
    }

    private fun buildSelfAssessment(data: Map<String, Any>): String {
        return """
SELF-ASSESSMENT:
- Why Defense Forces:
  ${getString(data, "whyDefenseForces").ifBlank { "Not provided" }}
- Stated Strengths:
  ${getString(data, "strengths").ifBlank { "Not provided" }}
- Acknowledged Weaknesses:
  ${getString(data, "weaknesses").ifBlank { "Not provided" }}
        """.trimIndent()
    }

    private fun personalizationNoteForNCC(data: Map<String, Any>): String? {
        val nccTraining = getMap(data, "nccTraining")
        if (nccTraining["hasTraining"] != true) return null
        val wing = nccTraining["wing"] as? String ?: ""
        val cert = nccTraining["certificateObtained"] as? String ?: ""
        return "→ Has NCC background ($wing Wing, $cert) - explore leadership experiences"
    }

    private fun personalizationNoteForRepeater(data: Map<String, Any>): String? =
        if (getListOfMaps(data, "previousInterviews").isNotEmpty()) {
            "→ Repeater candidate - ask about learning from previous attempt(s)"
        } else null

    private fun personalizationNoteForDefenseFamily(data: Map<String, Any>): String? {
        val fatherOcc = getString(data, "fatherOccupation")
        return if (listOf("army", "navy", "air force", "forces").any { fatherOcc.contains(it, ignoreCase = true) }) {
            "→ Defense family background - explore influence and expectations"
        } else null
    }

    private fun personalizationNoteForSports(data: Map<String, Any>): String? {
        val sports = getListOfMaps(data, "sportsParticipation")
        val hasAchievements = sports.any {
            (it["outstandingAchievement"] as? String)?.isNotBlank() == true ||
                (it["representedInstitution"] as? String)?.isNotBlank() == true
        }
        return if (hasAchievements) "→ Sports achievements present - ask about teamwork and competition" else null
    }

    private fun personalizationNoteForWorkExperience(data: Map<String, Any>): String? =
        if (getListOfMaps(data, "workExperience").isNotEmpty()) {
            "→ Has work experience - explore professional challenges and growth"
        } else null

    private fun personalizationNoteForLeadership(data: Map<String, Any>): String? =
        if (getString(data, "positionsOfResponsibility").isNotBlank()) {
            "→ Has held leadership positions - probe leadership style and challenges"
        } else null

    private fun personalizationNoteForRuralBackground(data: Map<String, Any>): String? {
        val population = getString(data, "maximumResidencePopulation")
        return if (population.toIntOrNull()?.let { it < 50000 } == true ||
            population.contains("village", ignoreCase = true)
        ) {
            "→ Rural/small town background - ask about adaptability and exposure"
        } else null
    }

    private fun buildPersonalizationNotes(data: Map<String, Any>): String {
        val notes = listOfNotNull(
            personalizationNoteForNCC(data),
            personalizationNoteForRepeater(data),
            personalizationNoteForDefenseFamily(data),
            personalizationNoteForSports(data),
            personalizationNoteForWorkExperience(data),
            personalizationNoteForLeadership(data),
            personalizationNoteForRuralBackground(data)
        )

        return if (notes.isNotEmpty()) {
            notes.joinToString("\n")
        } else {
            "→ Standard profile - use general SSB questioning approach"
        }
    }

}
