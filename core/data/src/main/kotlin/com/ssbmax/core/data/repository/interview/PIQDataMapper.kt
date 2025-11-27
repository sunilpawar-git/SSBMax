package com.ssbmax.core.data.repository.interview

import android.util.Log
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting PIQ data to AI-friendly formats
 *
 * Handles conversion of PIQ submission maps to JSON strings
 * optimized for Gemini AI question generation.
 */
@Singleton
class PIQDataMapper @Inject constructor() {

    private val gson = Gson()

    companion object {
        private const val TAG = "PIQDataMapper"
    }

    /**
     * Convert PIQ Map directly to JSON for AI (avoids complex deserialization)
     * Safely extracts fields with null checks and type casting
     */
    fun convertPIQMapToJson(piqMap: Map<String, Any>): String {
        return try {
            // Safely extract string fields
            fun getString(key: String): String = (piqMap[key] as? String) ?: ""

            // Safely extract list of maps
            @Suppress("UNCHECKED_CAST")
            fun getListOfMaps(key: String): List<Map<String, Any>> =
                (piqMap[key] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

            // Safely extract nested map
            @Suppress("UNCHECKED_CAST")
            fun getMap(key: String): Map<String, Any> = (piqMap[key] as? Map<String, Any>) ?: emptyMap()

            // Build simplified PIQ JSON for AI (only essential fields to reduce token usage)
            val simplifiedPIQ = mapOf(
                "name" to getString("fullName"),
                "age" to getString("age"),
                "education" to getString("educationGraduation").ifBlank {
                    getMap("educationGraduation").let { "${it["level"]} - ${it["institution"]}" }
                },
                "occupation" to getString("presentOccupation"),
                "hobbies" to getString("hobbies"),
                "sports" to getListOfMaps("sportsParticipation").joinToString(", ") {
                    (it["sport"] as? String) ?: ""
                }.ifBlank { getString("sports") },
                "whyDefense" to getString("whyDefenseForces"),
                "serviceChoice" to getString("choiceOfService"),
                "strengths" to getString("strengths"),
                "weaknesses" to getString("weaknesses")
            )

            gson.toJson(simplifiedPIQ)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting PIQ Map to JSON", e)
            "{}"
        }
    }

    /**
     * Build detailed PIQ context string for AI question generation
     *
     * Creates a human-readable summary from PIQ data map
     */
    fun buildPIQContextString(piq: Map<String, Any>): String {
        @Suppress("UNCHECKED_CAST")
        val data = piq["data"] as? Map<*, *> ?: emptyMap<String, Any>()

        return """
        Candidate Profile:
        Name: ${data["fullName"] ?: "Unknown"}
        Education 12th: ${data["education12th"] ?: "Not provided"}
        Education Graduation: ${data["educationGraduation"] ?: "Not provided"}
        Hobbies: ${data["hobbies"] ?: "Not provided"}
        Sports: ${data["sports"] ?: "Not provided"}
        Work Experience: ${data["workExperience"] ?: "None"}
        NCC Training: ${data["nccTraining"] ?: "None"}
        Why Defense Forces: ${data["whyDefenseForces"] ?: "Not provided"}
        Strengths: ${data["strengths"] ?: "Not provided"}
        Weaknesses: ${data["weaknesses"] ?: "Not provided"}
        Previous Interviews: ${data["previousInterviews"] ?: "None"}
        Positions of Responsibility: ${data["positionsOfResponsibility"] ?: "None"}
        Extra-Curricular Activities: ${data["extraCurricularActivities"] ?: "None"}
        """.trimIndent()
    }
}

