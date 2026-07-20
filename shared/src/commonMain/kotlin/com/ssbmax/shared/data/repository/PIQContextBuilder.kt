package com.ssbmax.shared.data.repository

/**
 * KMP port of the Android original's `PIQDataMapper` class. Ported as stateless top-level
 * functions (split across [PIQContextPersonalSections], [PIQContextLifeSections] and this file to
 * stay under this repo's 300-line-per-file limit) instead of one class — no `@Inject`/Android
 * `Log` dependency survives the port (the original body's only Android dependency was
 * `android.util.Log`, which no other data-layer file in `shared` uses either); errors here
 * surface as the same `"Error processing PIQ data..."` fallback string the original returned,
 * just without the `Log.e` side-channel.
 *
 * Builds comprehensive PIQ context for AI question generation, extracting all ~60 PIQ fields
 * organized into meaningful categories: Personal Background, Family Environment, Education
 * Journey, Career & Work, Activities & Interests, Leadership Exposure, SSB Journey,
 * Self-Assessment.
 */
internal object PIQContextBuilder {

    fun buildComprehensivePIQContext(piqMap: Map<String, Any>): String {
        return try {
            @Suppress("UNCHECKED_CAST")
            val data = (piqMap["data"] as? Map<String, Any>) ?: piqMap

            fun getString(key: String): String = (data[key] as? String)?.trim() ?: ""
            fun getBoolean(key: String): Boolean = data[key] as? Boolean ?: false

            @Suppress("UNCHECKED_CAST")
            fun getMap(key: String): Map<String, Any> =
                (data[key] as? Map<String, Any>) ?: emptyMap()

            @Suppress("UNCHECKED_CAST")
            fun getListOfMaps(key: String): List<Map<String, Any>> =
                (data[key] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

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
                maxResidencePopulation = getString("maximumResidencePopulation"),
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
            "Error processing PIQ data. Basic info only."
        }
    }
}
