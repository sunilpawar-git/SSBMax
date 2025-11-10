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
    val education: String = "",
    val income: String = "" // Monthly income
)

/**
 * Educational qualification entry
 */
data class Education(
    val level: String, // "10th", "12th", "Graduation", "Post-Graduation"
    val institution: String = "",
    val board: String = "",
    val stream: String = "", // For 12th
    val year: String = "",
    val percentage: String = "",
    val cgpa: String = "", // For graduation
    val mediumOfInstruction: String = "", // NEW: Medium of instruction
    val boarderDayScholar: String = "", // NEW: "Boarder" or "Day Scholar"
    val outstandingAchievement: String = "" // NEW: Outstanding achievements
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
 * NCC Training information
 */
data class NCCTraining(
    val hasTraining: Boolean = false,
    val totalTraining: String = "",
    val wing: String = "", // "Army", "Navy", "Air Force"
    val division: String = "",
    val certificateObtained: String = ""
)

/**
 * Sports participation entry
 */
data class SportsParticipation(
    val id: String = UUID.randomUUID().toString(),
    val sport: String = "",
    val period: String = "",
    val representedInstitution: String = "",
    val outstandingAchievement: String = ""
)

/**
 * Extra-curricular activity entry
 */
data class ExtraCurricularActivity(
    val id: String = UUID.randomUUID().toString(),
    val activityName: String = "",
    val duration: String = "",
    val outstandingAchievement: String = ""
)

/**
 * Previous interview entry
 */
data class PreviousInterview(
    val id: String = UUID.randomUUID().toString(),
    val typeOfEntry: String = "",
    val ssbNumber: String = "",
    val ssbPlace: String = "",
    val date: String = "",
    val chestNumber: String = "",
    val batchNumber: String = ""
)

/**
 * Complete PIQ submission
 */
data class PIQSubmission(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val testId: String = "piq_standard",
    
    // Header Section
    val oirNumber: String = "", // Auto-filled from OIR result
    val selectionBoard: String = "", // Dropdown: Army/Navy/Air Force/Coast Guard locations
    val batchNumber: String = "", // Disabled: "To be filled at SSB"
    val chestNumber: String = "", // Disabled: "To be filled at SSB"
    val upscRollNumber: String = "", // Disabled: "To be filled at SSB"
    
    // Page 1: Personal & Family
    val fullName: String = "",
    val dateOfBirth: String = "",
    val age: String = "",
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    
    // Personal Details Table
    val state: String = "",
    val district: String = "",
    val religion: String = "",
    val scStObcStatus: String = "", // "SC", "ST", "OBC", or ""
    val motherTongue: String = "",
    val maritalStatus: String = "", // "Married", "Single", "Widower"
    
    // Residence Information
    val permanentAddress: String = "",
    val presentAddress: String = "",
    val maximumResidence: String = "",
    val maximumResidencePopulation: String = "",
    val presentResidencePopulation: String = "",
    val permanentResidencePopulation: String = "",
    val isDistrictHQ: Boolean = false,
    
    // Physical Details
    val height: String = "", // in metres
    val weight: String = "", // in kilograms
    
    // Father details
    val fatherName: String = "",
    val fatherOccupation: String = "",
    val fatherEducation: String = "",
    val fatherIncome: String = "",
    
    // Mother details
    val motherName: String = "",
    val motherOccupation: String = "",
    val motherEducation: String = "",
    
    // Family Enhancement
    val parentsAlive: String = "", // "Both", "Father Only", "Mother Only", "None"
    val ageAtFatherDeath: String = "",
    val ageAtMotherDeath: String = "",
    val guardianName: String = "",
    val guardianOccupation: String = "",
    val guardianEducation: String = "",
    val guardianIncome: String = "",
    
    // Siblings
    val siblings: List<Sibling> = emptyList(),
    
    // Occupation
    val presentOccupation: String = "",
    val personalMonthlyIncome: String = "",
    
    // Page 2: Education & Career
    val education10th: Education = Education("10th"),
    val education12th: Education = Education("12th"),
    val educationGraduation: Education = Education("Graduation"),
    val educationPostGraduation: Education = Education("Post-Graduation"),
    
    val hobbies: String = "",
    val sports: String = "", // Legacy field, use sportsParticipation for structured data
    val sportsParticipation: List<SportsParticipation> = emptyList(),
    val extraCurricularActivities: List<ExtraCurricularActivity> = emptyList(),
    val positionsOfResponsibility: String = "",
    
    val workExperience: List<WorkExperience> = emptyList(),
    
    // NCC Training
    val nccTraining: NCCTraining = NCCTraining(),
    
    // Service Selection
    val natureOfCommission: String = "",
    val choiceOfService: String = "", // "Army", "Navy", "Air Force", "Coast Guard", "Any"
    val chancesAvailed: String = "",
    
    // Previous Interviews
    val previousInterviews: List<PreviousInterview> = emptyList(),
    
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
            
            // Header Section
            "oirNumber" to oirNumber,
            "selectionBoard" to selectionBoard,
            "batchNumber" to batchNumber,
            "chestNumber" to chestNumber,
            "upscRollNumber" to upscRollNumber,
            
            // Personal Information
            "fullName" to fullName,
            "dateOfBirth" to dateOfBirth,
            "age" to age,
            "gender" to gender,
            "phone" to phone,
            "email" to email,
            
            // Personal Details Table
            "state" to state,
            "district" to district,
            "religion" to religion,
            "scStObcStatus" to scStObcStatus,
            "motherTongue" to motherTongue,
            "maritalStatus" to maritalStatus,
            
            // Residence Information
            "permanentAddress" to permanentAddress,
            "presentAddress" to presentAddress,
            "maximumResidence" to maximumResidence,
            "maximumResidencePopulation" to maximumResidencePopulation,
            "presentResidencePopulation" to presentResidencePopulation,
            "permanentResidencePopulation" to permanentResidencePopulation,
            "isDistrictHQ" to isDistrictHQ,
            
            // Physical Details
            "height" to height,
            "weight" to weight,
            
            // Father details
            "fatherName" to fatherName,
            "fatherOccupation" to fatherOccupation,
            "fatherEducation" to fatherEducation,
            "fatherIncome" to fatherIncome,
            
            // Mother details
            "motherName" to motherName,
            "motherOccupation" to motherOccupation,
            "motherEducation" to motherEducation,
            
            // Family Enhancement
            "parentsAlive" to parentsAlive,
            "ageAtFatherDeath" to ageAtFatherDeath,
            "ageAtMotherDeath" to ageAtMotherDeath,
            "guardianName" to guardianName,
            "guardianOccupation" to guardianOccupation,
            "guardianEducation" to guardianEducation,
            "guardianIncome" to guardianIncome,
            
            // Siblings
            "siblings" to siblings.map { mapOf(
                "id" to it.id,
                "name" to it.name,
                "age" to it.age,
                "occupation" to it.occupation,
                "education" to it.education,
                "income" to it.income
            ) },
            
            // Occupation
            "presentOccupation" to presentOccupation,
            "personalMonthlyIncome" to personalMonthlyIncome,
            
            // Education
            "education10th" to mapOf(
                "level" to education10th.level,
                "institution" to education10th.institution,
                "board" to education10th.board,
                "year" to education10th.year,
                "percentage" to education10th.percentage,
                "mediumOfInstruction" to education10th.mediumOfInstruction,
                "boarderDayScholar" to education10th.boarderDayScholar,
                "outstandingAchievement" to education10th.outstandingAchievement
            ),
            "education12th" to mapOf(
                "level" to education12th.level,
                "institution" to education12th.institution,
                "board" to education12th.board,
                "stream" to education12th.stream,
                "year" to education12th.year,
                "percentage" to education12th.percentage,
                "mediumOfInstruction" to education12th.mediumOfInstruction,
                "boarderDayScholar" to education12th.boarderDayScholar,
                "outstandingAchievement" to education12th.outstandingAchievement
            ),
            "educationGraduation" to mapOf(
                "level" to educationGraduation.level,
                "institution" to educationGraduation.institution,
                "board" to educationGraduation.board,
                "year" to educationGraduation.year,
                "cgpa" to educationGraduation.cgpa,
                "mediumOfInstruction" to educationGraduation.mediumOfInstruction,
                "boarderDayScholar" to educationGraduation.boarderDayScholar,
                "outstandingAchievement" to educationGraduation.outstandingAchievement
            ),
            "educationPostGraduation" to mapOf(
                "level" to educationPostGraduation.level,
                "institution" to educationPostGraduation.institution,
                "board" to educationPostGraduation.board,
                "year" to educationPostGraduation.year,
                "cgpa" to educationPostGraduation.cgpa,
                "mediumOfInstruction" to educationPostGraduation.mediumOfInstruction,
                "boarderDayScholar" to educationPostGraduation.boarderDayScholar,
                "outstandingAchievement" to educationPostGraduation.outstandingAchievement
            ),
            
            // Activities
            "hobbies" to hobbies,
            "sports" to sports, // Legacy field
            "sportsParticipation" to sportsParticipation.map { mapOf(
                "id" to it.id,
                "sport" to it.sport,
                "period" to it.period,
                "representedInstitution" to it.representedInstitution,
                "outstandingAchievement" to it.outstandingAchievement
            ) },
            "extraCurricularActivities" to extraCurricularActivities.map { mapOf(
                "id" to it.id,
                "activityName" to it.activityName,
                "duration" to it.duration,
                "outstandingAchievement" to it.outstandingAchievement
            ) },
            "positionsOfResponsibility" to positionsOfResponsibility,
            
            // Work Experience
            "workExperience" to workExperience.map { mapOf(
                "id" to it.id,
                "company" to it.company,
                "role" to it.role,
                "duration" to it.duration,
                "description" to it.description
            ) },
            
            // NCC Training
            "nccTraining" to mapOf(
                "hasTraining" to nccTraining.hasTraining,
                "totalTraining" to nccTraining.totalTraining,
                "wing" to nccTraining.wing,
                "division" to nccTraining.division,
                "certificateObtained" to nccTraining.certificateObtained
            ),
            
            // Service Selection
            "natureOfCommission" to natureOfCommission,
            "choiceOfService" to choiceOfService,
            "chancesAvailed" to chancesAvailed,
            
            // Previous Interviews
            "previousInterviews" to previousInterviews.map { mapOf(
                "id" to it.id,
                "typeOfEntry" to it.typeOfEntry,
                "ssbNumber" to it.ssbNumber,
                "ssbPlace" to it.ssbPlace,
                "date" to it.date,
                "chestNumber" to it.chestNumber,
                "batchNumber" to it.batchNumber
            ) },
            
            // Motivation & Self Assessment
            "whyDefenseForces" to whyDefenseForces,
            "strengths" to strengths,
            "weaknesses" to weaknesses,
            
            // Metadata
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

