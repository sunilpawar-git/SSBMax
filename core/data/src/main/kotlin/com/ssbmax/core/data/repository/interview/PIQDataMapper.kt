package com.ssbmax.core.data.repository.interview

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
class PIQDataMapper @Inject constructor() {

    companion object {
        private const val TAG = "PIQDataMapper"
    }

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
            // Extract data from nested structure if present
            @Suppress("UNCHECKED_CAST")
            val data = (piqMap["data"] as? Map<String, Any>) ?: piqMap

            // Helper functions for safe extraction
            fun getString(key: String): String = (data[key] as? String)?.trim() ?: ""
            fun getBoolean(key: String): Boolean = data[key] as? Boolean ?: false

            @Suppress("UNCHECKED_CAST")
            fun getMap(key: String): Map<String, Any> =
                (data[key] as? Map<String, Any>) ?: emptyMap()

            @Suppress("UNCHECKED_CAST")
            fun getListOfMaps(key: String): List<Map<String, Any>> =
                (data[key] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

            // Build each section
            val personalBackground = buildPersonalBackground(
                fullName = getString("fullName"),
                age = getString("age"),
                gender = getString("gender"),
                state = getString("state"),
                district = getString("district"),
                maritalStatus = getString("maritalStatus"),
                religion = getString("religion"),
                motherTongue = getString("motherTongue"),
                permanentAddress = getString("permanentAddress"),
                presentAddress = getString("presentAddress"),
                maximumResidence = getString("maximumResidence"),
                maxResidencePopulation = getString("maximumResidencePopulation"),
                presentResidencePopulation = getString("presentResidencePopulation"),
                isDistrictHQ = getBoolean("isDistrictHQ"),
                height = getString("height"),
                weight = getString("weight")
            )

            val familyEnvironment = buildFamilyEnvironment(
                fatherName = getString("fatherName"),
                fatherOccupation = getString("fatherOccupation"),
                fatherEducation = getString("fatherEducation"),
                fatherIncome = getString("fatherIncome"),
                motherName = getString("motherName"),
                motherOccupation = getString("motherOccupation"),
                motherEducation = getString("motherEducation"),
                parentsAlive = getString("parentsAlive"),
                ageAtFatherDeath = getString("ageAtFatherDeath"),
                ageAtMotherDeath = getString("ageAtMotherDeath"),
                guardianName = getString("guardianName"),
                guardianOccupation = getString("guardianOccupation"),
                siblings = getListOfMaps("siblings")
            )

            val educationJourney = buildEducationJourney(
                education10th = getMap("education10th"),
                education12th = getMap("education12th"),
                educationGraduation = getMap("educationGraduation"),
                educationPostGraduation = getMap("educationPostGraduation")
            )

            val careerAndWork = buildCareerAndWork(
                presentOccupation = getString("presentOccupation"),
                personalMonthlyIncome = getString("personalMonthlyIncome"),
                workExperience = getListOfMaps("workExperience")
            )

            val activitiesAndInterests = buildActivitiesAndInterests(
                hobbies = getString("hobbies"),
                sports = getString("sports"),
                sportsParticipation = getListOfMaps("sportsParticipation"),
                extraCurricularActivities = getListOfMaps("extraCurricularActivities")
            )

            val leadershipExposure = buildLeadershipExposure(
                nccTraining = getMap("nccTraining"),
                positionsOfResponsibility = getString("positionsOfResponsibility")
            )

            val ssbJourney = buildSSBJourney(
                previousInterviews = getListOfMaps("previousInterviews"),
                choiceOfService = getString("choiceOfService"),
                natureOfCommission = getString("natureOfCommission"),
                chancesAvailed = getString("chancesAvailed")
            )

            val selfAssessment = buildSelfAssessment(
                whyDefenseForces = getString("whyDefenseForces"),
                strengths = getString("strengths"),
                weaknesses = getString("weaknesses")
            )

            // Combine all sections
            """
CANDIDATE PROFILE
=================

$personalBackground

$familyEnvironment

$educationJourney

$careerAndWork

$activitiesAndInterests

$leadershipExposure

$ssbJourney

$selfAssessment

PERSONALIZATION NOTES:
${buildPersonalizationNotes(data)}
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(TAG, "Error building comprehensive PIQ context", e)
            "Error processing PIQ data. Basic info only."
        }
    }

    private fun buildPersonalBackground(
        fullName: String,
        age: String,
        gender: String,
        state: String,
        district: String,
        maritalStatus: String,
        religion: String,
        motherTongue: String,
        permanentAddress: String,
        presentAddress: String,
        maximumResidence: String,
        maxResidencePopulation: String,
        presentResidencePopulation: String,
        isDistrictHQ: Boolean,
        height: String,
        weight: String
    ): String {
        val residenceType = deriveResidenceType(maxResidencePopulation)
        val mobilityContext = if (permanentAddress != presentAddress && presentAddress.isNotBlank()) {
            "Has relocated from permanent address"
        } else {
            "Living at permanent address"
        }

        return """
PERSONAL BACKGROUND:
- Name: ${fullName.ifBlank { "Not provided" }}
- Age: ${age.ifBlank { "Not provided" }}
- Gender: ${gender.ifBlank { "Not provided" }}
- From: ${district.ifBlank { "Unknown district" }}, ${state.ifBlank { "Unknown state" }}
- Background: $residenceType${if (isDistrictHQ) " (District HQ)" else ""}
- Marital Status: ${maritalStatus.ifBlank { "Not provided" }}
- Religion: ${religion.ifBlank { "Not provided" }}
- Mother Tongue: ${motherTongue.ifBlank { "Not provided" }}
- Mobility: $mobilityContext
- Physical: Height ${height.ifBlank { "N/A" }}m, Weight ${weight.ifBlank { "N/A" }}kg
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

    private fun buildFamilyEnvironment(
        fatherName: String,
        fatherOccupation: String,
        fatherEducation: String,
        fatherIncome: String,
        motherName: String,
        motherOccupation: String,
        motherEducation: String,
        parentsAlive: String,
        ageAtFatherDeath: String,
        ageAtMotherDeath: String,
        guardianName: String,
        guardianOccupation: String,
        siblings: List<Map<String, Any>>
    ): String {
        val familyContext = deriveFamilyContext(
            fatherOccupation, motherOccupation, parentsAlive,
            ageAtFatherDeath, ageAtMotherDeath
        )

        val siblingSummary = if (siblings.isNotEmpty()) {
            siblings.mapNotNull { sibling ->
                val name = sibling["name"] as? String ?: ""
                val age = sibling["age"] as? String ?: ""
                val occupation = sibling["occupation"] as? String ?: ""
                if (name.isNotBlank()) {
                    "$name (${age.ifBlank { "age unknown" }}, ${occupation.ifBlank { "occupation unknown" }})"
                } else null
            }.joinToString("; ").ifBlank { "Details not provided" }
        } else {
            "Only child / No siblings listed"
        }

        return """
FAMILY ENVIRONMENT:
- Father: ${fatherName.ifBlank { "Not provided" }}
  • Occupation: ${fatherOccupation.ifBlank { "Not provided" }}
  • Education: ${fatherEducation.ifBlank { "Not provided" }}
  • Income: ${fatherIncome.ifBlank { "Not provided" }}
- Mother: ${motherName.ifBlank { "Not provided" }}
  • Occupation: ${motherOccupation.ifBlank { "Not provided" }}
  • Education: ${motherEducation.ifBlank { "Not provided" }}
- Parents Status: ${parentsAlive.ifBlank { "Both alive (assumed)" }}${
            if (ageAtFatherDeath.isNotBlank()) "\n  • Lost father at age $ageAtFatherDeath" else ""
        }${
            if (ageAtMotherDeath.isNotBlank()) "\n  • Lost mother at age $ageAtMotherDeath" else ""
        }${
            if (guardianName.isNotBlank()) "\n- Guardian: $guardianName ($guardianOccupation)" else ""
        }
- Siblings: $siblingSummary
- Family Context: $familyContext
        """.trimIndent()
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

    private fun buildEducationJourney(
        education10th: Map<String, Any>,
        education12th: Map<String, Any>,
        educationGraduation: Map<String, Any>,
        educationPostGraduation: Map<String, Any>
    ): String {
        fun formatEducation(edu: Map<String, Any>, level: String): String {
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
                "- $level:\n  " + details.joinToString("\n  • ", prefix = "• ")
            } else {
                "- $level: Not provided"
            }
        }

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

    private fun buildCareerAndWork(
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
                        append("• $role")
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

    private fun buildActivitiesAndInterests(
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
                        append("• $sport")
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
                        append("• $activity")
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

    private fun buildLeadershipExposure(
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

    private fun buildSSBJourney(
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
            previousInterviews.size >= 2 -> "Multiple attempts (${previousInterviews.size}) - highly determined"
            else -> ""
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

    private fun buildSelfAssessment(
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
    private fun buildPersonalizationNotes(data: Map<String, Any>): String {
        val notes = mutableListOf<String>()

        // Check for NCC
        val nccTraining = data["nccTraining"] as? Map<String, Any>
        if (nccTraining?.get("hasTraining") == true) {
            val wing = nccTraining["wing"] as? String ?: ""
            val cert = nccTraining["certificateObtained"] as? String ?: ""
            notes.add("→ Has NCC background ($wing Wing, $cert) - explore leadership experiences")
        }

        // Check for repeat attempts
        val prevInterviews = (data["previousInterviews"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
        if (prevInterviews?.isNotEmpty() == true) {
            notes.add("→ Repeater candidate - ask about learning from previous attempt(s)")
        }

        // Check for defense family
        val fatherOcc = data["fatherOccupation"] as? String ?: ""
        if (listOf("army", "navy", "air force", "forces").any { fatherOcc.contains(it, ignoreCase = true) }) {
            notes.add("→ Defense family background - explore influence and expectations")
        }

        // Check for sports achievements
        val sports = (data["sportsParticipation"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
        val hasAchievements = sports?.any {
            (it["outstandingAchievement"] as? String)?.isNotBlank() == true ||
                (it["representedInstitution"] as? String)?.isNotBlank() == true
        } == true
        if (hasAchievements) {
            notes.add("→ Sports achievements present - ask about teamwork and competition")
        }

        // Check for work experience
        val workExp = (data["workExperience"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
        if (workExp?.isNotEmpty() == true) {
            notes.add("→ Has work experience - explore professional challenges and growth")
        }

        // Check for leadership positions
        val positions = data["positionsOfResponsibility"] as? String ?: ""
        if (positions.isNotBlank()) {
            notes.add("→ Has held leadership positions - probe leadership style and challenges")
        }

        // Check for rural/small town background
        val population = data["maximumResidencePopulation"] as? String ?: ""
        if (population.toIntOrNull()?.let { it < 50000 } == true ||
            population.contains("village", ignoreCase = true)) {
            notes.add("→ Rural/small town background - ask about adaptability and exposure")
        }

        return if (notes.isNotEmpty()) {
            notes.joinToString("\n")
        } else {
            "→ Standard profile - use general SSB questioning approach"
        }
    }

}
