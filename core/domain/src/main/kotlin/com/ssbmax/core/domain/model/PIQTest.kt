package com.ssbmax.core.domain.model

import java.util.UUID

/**
 * PIQ (Personal Information Questionnaire) - Phase 2 Assessment
 * 
 * Two-page form matching the actual SSB PIQ format.
 * Untimed, allows free navigation between pages.
 * Used by assessors for reference, not scored.
 */

/**
 * PIQ pages representing the two-page SSB form
 */
enum class PIQPage {
    PAGE_1, // Personal & Family Details
    PAGE_2; // Education & Career Details
    
    val displayName: String
        get() = when (this) {
            PAGE_1 -> "Personal & Family"
            PAGE_2 -> "Education & Career"
        }
    
    val pageNumber: Int
        get() = ordinal + 1
}

/**
 * Field types for PIQ questions
 */
enum class PIQFieldType {
    TEXT,           // Single-line text
    TEXT_MULTILINE, // Multi-line text area
    DATE,           // Date picker
    PHONE,          // Phone number
    EMAIL,          // Email address
    DROPDOWN,       // Dropdown selection
    NUMBER;         // Numeric input
}

/**
 * Represents a single field/question in PIQ
 */
data class PIQField(
    val id: String,
    val page: PIQPage,
    val label: String,
    val fieldType: PIQFieldType,
    val isRequired: Boolean = false,
    val placeholder: String = "",
    val options: List<String> = emptyList(), // For dropdown fields
    val maxLength: Int? = null,
    val section: String = "" // Grouping: "Personal", "Father", "Mother", etc.
)

/**
 * User's answer to a PIQ field
 */
data class PIQAnswer(
    val fieldId: String,
    val value: String,
    val lastModifiedAt: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean get() = value.isBlank()
}

/**
 * Sibling information (dynamic list on Page 1)
 */
data class Sibling(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val age: String = "",
    val occupation: String = "",
    val education: String = ""
)

/**
 * Educational qualification entry
 */
data class Education(
    val level: String, // "10th", "12th", "Graduation"
    val institution: String = "",
    val board: String = "",
    val stream: String = "", // For 12th
    val year: String = "",
    val percentage: String = "",
    val cgpa: String = "" // For graduation
)

/**
 * Work experience entry
 */
data class WorkExperience(
    val id: String = UUID.randomUUID().toString(),
    val company: String = "",
    val role: String = "",
    val duration: String = "",
    val description: String = ""
)

/**
 * Complete PIQ submission
 */
data class PIQSubmission(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String = "piq_standard",
    
    // Page 1: Personal & Family
    val fullName: String = "",
    val dateOfBirth: String = "",
    val age: String = "",
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    val permanentAddress: String = "",
    val presentAddress: String = "",
    
    // Father details
    val fatherName: String = "",
    val fatherOccupation: String = "",
    val fatherEducation: String = "",
    val fatherIncome: String = "",
    
    // Mother details
    val motherName: String = "",
    val motherOccupation: String = "",
    val motherEducation: String = "",
    
    // Siblings
    val siblings: List<Sibling> = emptyList(),
    
    // Page 2: Education & Career
    val education10th: Education = Education("10th"),
    val education12th: Education = Education("12th"),
    val educationGraduation: Education = Education("Graduation"),
    
    val hobbies: String = "",
    val sports: String = "",
    val workExperience: List<WorkExperience> = emptyList(),
    val whyDefenseForces: String = "",
    val strengths: String = "",
    val weaknesses: String = "",
    
    // Metadata
    val status: SubmissionStatus = SubmissionStatus.DRAFT,
    val submittedAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val gradedByInstructorId: String? = null,
    val gradingTimestamp: Long? = null,
    val aiPreliminaryScore: PIQAIScore? = null
) {
    /**
     * Convert to Firestore map format
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "testId" to testId,
            "fullName" to fullName,
            "dateOfBirth" to dateOfBirth,
            "age" to age,
            "gender" to gender,
            "phone" to phone,
            "email" to email,
            "permanentAddress" to permanentAddress,
            "presentAddress" to presentAddress,
            "fatherName" to fatherName,
            "fatherOccupation" to fatherOccupation,
            "fatherEducation" to fatherEducation,
            "fatherIncome" to fatherIncome,
            "motherName" to motherName,
            "motherOccupation" to motherOccupation,
            "motherEducation" to motherEducation,
            "siblings" to siblings.map { mapOf(
                "id" to it.id,
                "name" to it.name,
                "age" to it.age,
                "occupation" to it.occupation,
                "education" to it.education
            ) },
            "education10th" to mapOf(
                "level" to education10th.level,
                "institution" to education10th.institution,
                "board" to education10th.board,
                "year" to education10th.year,
                "percentage" to education10th.percentage
            ),
            "education12th" to mapOf(
                "level" to education12th.level,
                "institution" to education12th.institution,
                "board" to education12th.board,
                "stream" to education12th.stream,
                "year" to education12th.year,
                "percentage" to education12th.percentage
            ),
            "educationGraduation" to mapOf(
                "level" to educationGraduation.level,
                "institution" to educationGraduation.institution,
                "board" to educationGraduation.board,
                "year" to educationGraduation.year,
                "cgpa" to educationGraduation.cgpa
            ),
            "hobbies" to hobbies,
            "sports" to sports,
            "workExperience" to workExperience.map { mapOf(
                "id" to it.id,
                "company" to it.company,
                "role" to it.role,
                "duration" to it.duration,
                "description" to it.description
            ) },
            "whyDefenseForces" to whyDefenseForces,
            "strengths" to strengths,
            "weaknesses" to weaknesses,
            "status" to status.name,
            "submittedAt" to submittedAt,
            "lastModifiedAt" to lastModifiedAt,
            "gradedByInstructorId" to gradedByInstructorId,
            "gradingTimestamp" to gradingTimestamp,
            "aiPreliminaryScore" to aiPreliminaryScore?.let { score ->
                mapOf(
                    "overallScore" to score.overallScore,
                    "personalInfoScore" to score.personalInfoScore,
                    "familyInfoScore" to score.familyInfoScore,
                    "motivationScore" to score.motivationScore,
                    "selfAssessmentScore" to score.selfAssessmentScore,
                    "feedback" to score.feedback,
                    "strengths" to score.strengths,
                    "areasForImprovement" to score.areasForImprovement,
                    "completenessPercentage" to score.completenessPercentage,
                    "clarityScore" to score.clarityScore,
                    "consistencyScore" to score.consistencyScore,
                    "analysisTimestamp" to score.analysisTimestamp
                )
            }
        )
    }
    
    val isComplete: Boolean
        get() = fullName.isNotBlank() && 
                dateOfBirth.isNotBlank() && 
                phone.isNotBlank()
}

/**
 * AI Quality Assessment for PIQ
 * Mock scores until actual AI integration
 */
data class PIQAIScore(
    val overallScore: Float, // 0-100
    val personalInfoScore: Float, // 0-25 (completeness, clarity)
    val familyInfoScore: Float, // 0-25 (completeness, consistency)
    val motivationScore: Float, // 0-25 (depth, clarity of "Why Defense Forces")
    val selfAssessmentScore: Float, // 0-25 (strengths/weaknesses quality)
    val feedback: String,
    val strengths: List<String>,
    val areasForImprovement: List<String>,
    val completenessPercentage: Int, // % of fields filled
    val clarityScore: Float, // How detailed are answers (0-10)
    val consistencyScore: Float, // Logical consistency (0-10)
    val analysisTimestamp: Long = System.currentTimeMillis()
)

/**
 * PIQ test configuration
 */
data class PIQTestConfig(
    val testId: String = "piq_standard",
    val title: String = "PIQ - Personal Information Questionnaire",
    val description: String = "Complete your personal information form",
    val totalPages: Int = 2,
    val requiresInstructorReview: Boolean = false,
    val allowDraft: Boolean = true,
    val autoSaveIntervalMs: Long = 2000 // 2 seconds
)

/**
 * Factory method to create standard PIQ fields for Page 1
 */
fun createPage1Fields(): List<PIQField> {
    return listOf(
        // Personal Information Section
        PIQField("fullName", PIQPage.PAGE_1, "Full Name", PIQFieldType.TEXT, isRequired = true, section = "Personal"),
        PIQField("dateOfBirth", PIQPage.PAGE_1, "Date of Birth", PIQFieldType.DATE, isRequired = true, section = "Personal"),
        PIQField("age", PIQPage.PAGE_1, "Age", PIQFieldType.NUMBER, section = "Personal"),
        PIQField("gender", PIQPage.PAGE_1, "Gender", PIQFieldType.DROPDOWN, 
            options = listOf("Male", "Female", "Other"), isRequired = true, section = "Personal"),
        PIQField("phone", PIQPage.PAGE_1, "Phone Number", PIQFieldType.PHONE, isRequired = true, section = "Personal"),
        PIQField("email", PIQPage.PAGE_1, "Email", PIQFieldType.EMAIL, section = "Personal"),
        PIQField("permanentAddress", PIQPage.PAGE_1, "Permanent Address", PIQFieldType.TEXT_MULTILINE, 
            isRequired = true, section = "Personal", maxLength = 500),
        PIQField("presentAddress", PIQPage.PAGE_1, "Present Address", PIQFieldType.TEXT_MULTILINE, 
            section = "Personal", maxLength = 500),
        
        // Father Details Section
        PIQField("fatherName", PIQPage.PAGE_1, "Father's Name", PIQFieldType.TEXT, isRequired = true, section = "Father"),
        PIQField("fatherOccupation", PIQPage.PAGE_1, "Father's Occupation", PIQFieldType.TEXT, section = "Father"),
        PIQField("fatherEducation", PIQPage.PAGE_1, "Father's Education", PIQFieldType.TEXT, section = "Father"),
        PIQField("fatherIncome", PIQPage.PAGE_1, "Father's Annual Income", PIQFieldType.TEXT, section = "Father"),
        
        // Mother Details Section
        PIQField("motherName", PIQPage.PAGE_1, "Mother's Name", PIQFieldType.TEXT, isRequired = true, section = "Mother"),
        PIQField("motherOccupation", PIQPage.PAGE_1, "Mother's Occupation", PIQFieldType.TEXT, section = "Mother"),
        PIQField("motherEducation", PIQPage.PAGE_1, "Mother's Education", PIQFieldType.TEXT, section = "Mother")
    )
}

/**
 * Factory method to create standard PIQ fields for Page 2
 */
fun createPage2Fields(): List<PIQField> {
    return listOf(
        // 10th Standard Section
        PIQField("10th_institution", PIQPage.PAGE_2, "School Name", PIQFieldType.TEXT, section = "10th Standard"),
        PIQField("10th_board", PIQPage.PAGE_2, "Board", PIQFieldType.TEXT, section = "10th Standard"),
        PIQField("10th_year", PIQPage.PAGE_2, "Year of Passing", PIQFieldType.NUMBER, section = "10th Standard"),
        PIQField("10th_percentage", PIQPage.PAGE_2, "Percentage", PIQFieldType.NUMBER, section = "10th Standard"),
        
        // 12th Standard Section
        PIQField("12th_institution", PIQPage.PAGE_2, "School Name", PIQFieldType.TEXT, section = "12th Standard"),
        PIQField("12th_board", PIQPage.PAGE_2, "Board", PIQFieldType.TEXT, section = "12th Standard"),
        PIQField("12th_stream", PIQPage.PAGE_2, "Stream", PIQFieldType.DROPDOWN, 
            options = listOf("Science", "Commerce", "Arts"), section = "12th Standard"),
        PIQField("12th_year", PIQPage.PAGE_2, "Year of Passing", PIQFieldType.NUMBER, section = "12th Standard"),
        PIQField("12th_percentage", PIQPage.PAGE_2, "Percentage", PIQFieldType.NUMBER, section = "12th Standard"),
        
        // Graduation Section
        PIQField("grad_institution", PIQPage.PAGE_2, "College Name", PIQFieldType.TEXT, section = "Graduation"),
        PIQField("grad_university", PIQPage.PAGE_2, "University", PIQFieldType.TEXT, section = "Graduation"),
        PIQField("grad_degree", PIQPage.PAGE_2, "Degree", PIQFieldType.TEXT, section = "Graduation"),
        PIQField("grad_year", PIQPage.PAGE_2, "Year of Passing", PIQFieldType.NUMBER, section = "Graduation"),
        PIQField("grad_cgpa", PIQPage.PAGE_2, "CGPA/Percentage", PIQFieldType.TEXT, section = "Graduation"),
        
        // Additional Information
        PIQField("hobbies", PIQPage.PAGE_2, "Hobbies & Interests", PIQFieldType.TEXT_MULTILINE, 
            section = "Additional", maxLength = 500),
        PIQField("sports", PIQPage.PAGE_2, "Sports Played", PIQFieldType.TEXT_MULTILINE, 
            section = "Additional", maxLength = 300),
        PIQField("whyDefenseForces", PIQPage.PAGE_2, "Why do you want to join the Defense Forces?", 
            PIQFieldType.TEXT_MULTILINE, section = "Motivation", maxLength = 1000, isRequired = true),
        PIQField("strengths", PIQPage.PAGE_2, "Your Strengths", PIQFieldType.TEXT_MULTILINE, 
            section = "Self Assessment", maxLength = 500),
        PIQField("weaknesses", PIQPage.PAGE_2, "Areas for Improvement", PIQFieldType.TEXT_MULTILINE, 
            section = "Self Assessment", maxLength = 500)
    )
}

