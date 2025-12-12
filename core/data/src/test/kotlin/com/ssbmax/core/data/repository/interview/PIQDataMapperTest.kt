package com.ssbmax.core.data.repository.interview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PIQDataMapper
 *
 * Tests the comprehensive PIQ context builder that extracts all 60+ fields
 * from PIQ data and organizes them for AI consumption.
 *
 * Key test areas:
 * - Complete PIQ data extraction
 * - Partial/missing field handling
 * - Family context derivation (defense family, single parent, etc.)
 * - Residence type detection (urban/rural)
 * - Personalization notes generation
 * - Nested data structure handling
 */
class PIQDataMapperTest {

    private lateinit var mapper: PIQDataMapper

    @Before
    fun setUp() {
        mapper = PIQDataMapper()
    }

    // ============================================
    // COMPLETE PIQ DATA TESTS
    // ============================================

    @Test
    fun `buildComprehensivePIQContext extracts all personal background fields`() {
        // Given
        val piqData = createCompletePIQData()

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should contain candidate name", context.contains("John Doe"))
        assertTrue("Should contain age", context.contains("25"))
        assertTrue("Should contain gender", context.contains("Male"))
        assertTrue("Should contain state", context.contains("Maharashtra"))
        assertTrue("Should contain district", context.contains("Pune"))
        assertTrue("Should contain marital status", context.contains("Single"))
    }

    @Test
    fun `buildComprehensivePIQContext extracts family environment fields`() {
        // Given
        val piqData = createCompletePIQData()

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should contain father's name", context.contains("Rajesh Doe"))
        assertTrue("Should contain father's occupation", context.contains("Army Officer"))
        assertTrue("Should contain mother's name", context.contains("Priya Doe"))
        assertTrue("Should contain mother's occupation", context.contains("Teacher"))
    }

    @Test
    fun `buildComprehensivePIQContext extracts education journey fields`() {
        // Given
        val piqData = createCompletePIQData()

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should contain 10th details", context.contains("10th Standard"))
        assertTrue("Should contain 12th details", context.contains("12th Standard"))
        assertTrue("Should contain graduation", context.contains("Graduation"))
    }

    @Test
    fun `buildComprehensivePIQContext extracts activities and interests`() {
        // Given
        val piqData = createCompletePIQData()

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should contain hobbies", context.contains("Reading"))
        assertTrue("Should contain sports", context.contains("Cricket"))
    }

    @Test
    fun `buildComprehensivePIQContext extracts self-assessment`() {
        // Given
        val piqData = createCompletePIQData()

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should contain why defense", context.contains("Serve the nation"))
        assertTrue("Should contain strengths", context.contains("Leadership"))
        assertTrue("Should contain weaknesses", context.contains("Impatient"))
    }

    // ============================================
    // FAMILY CONTEXT DERIVATION TESTS
    // ============================================

    @Test
    fun `deriveFamilyContext identifies defense family background`() {
        // Given
        val piqData = mapOf(
            "fatherOccupation" to "Army Officer",
            "motherOccupation" to "Homemaker",
            "parentsAlive" to "Both"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify defense family", context.contains("Defense family background"))
    }

    @Test
    fun `deriveFamilyContext identifies single parent upbringing`() {
        // Given
        val piqData = mapOf(
            "fatherOccupation" to "Engineer",
            "motherOccupation" to "",
            "parentsAlive" to "Father Only",
            "ageAtMotherDeath" to "10"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify single parent", context.contains("Single parent"))
    }

    @Test
    fun `deriveFamilyContext identifies working mother`() {
        // Given
        val piqData = mapOf(
            "fatherOccupation" to "Businessman",
            "motherOccupation" to "Doctor",
            "parentsAlive" to "Both"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify working mother", context.contains("Working mother"))
    }

    @Test
    fun `deriveFamilyContext identifies government service family`() {
        // Given
        val piqData = mapOf(
            "fatherOccupation" to "IAS Officer",
            "motherOccupation" to "Homemaker",
            "parentsAlive" to "Both"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify government family", context.contains("Government service family"))
    }

    // ============================================
    // RESIDENCE TYPE DETECTION TESTS
    // ============================================

    @Test
    fun `deriveResidenceType identifies metropolitan city`() {
        // Given
        val piqData = mapOf(
            "maximumResidencePopulation" to "Metro city"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify metro", context.contains("Metropolitan city"))
    }

    @Test
    fun `deriveResidenceType identifies rural background`() {
        // Given
        val piqData = mapOf(
            "maximumResidencePopulation" to "5000"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify rural", context.contains("Rural") || context.contains("Village"))
    }

    @Test
    fun `deriveResidenceType identifies town`() {
        // Given
        val piqData = mapOf(
            "maximumResidencePopulation" to "75000"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify town", context.contains("Town"))
    }

    // ============================================
    // PERSONALIZATION NOTES TESTS
    // ============================================

    @Test
    fun `buildPersonalizationNotes includes NCC guidance`() {
        // Given
        val piqData = mapOf(
            "nccTraining" to mapOf(
                "hasTraining" to true,
                "wing" to "Army",
                "certificateObtained" to "C Certificate"
            )
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should include NCC guidance", context.contains("NCC background"))
    }

    @Test
    fun `buildPersonalizationNotes includes repeater guidance`() {
        // Given
        val piqData = mapOf(
            "previousInterviews" to listOf(
                mapOf(
                    "typeOfEntry" to "CDS",
                    "ssbPlace" to "Allahabad",
                    "date" to "2024-01"
                )
            )
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should include repeater guidance", context.contains("Repeater candidate"))
    }

    @Test
    fun `buildPersonalizationNotes includes sports achievement guidance`() {
        // Given
        val piqData = mapOf(
            "sportsParticipation" to listOf(
                mapOf(
                    "sport" to "Cricket",
                    "representedInstitution" to "State Level",
                    "outstandingAchievement" to "Captain of state team"
                )
            )
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should include sports guidance", context.contains("Sports achievements"))
    }

    @Test
    fun `buildPersonalizationNotes includes work experience guidance`() {
        // Given
        val piqData = mapOf(
            "workExperience" to listOf(
                mapOf(
                    "company" to "TCS",
                    "role" to "Software Engineer",
                    "duration" to "2 years"
                )
            )
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should include work guidance", context.contains("work experience"))
    }

    // ============================================
    // NESTED DATA STRUCTURE TESTS
    // ============================================

    @Test
    fun `buildComprehensivePIQContext handles nested data structure`() {
        // Given - PIQ data with nested "data" field (as stored in Firestore)
        val piqData = mapOf(
            "id" to "submission-123",
            "data" to mapOf(
                "fullName" to "Nested User",
                "age" to "24",
                "hobbies" to "Music"
            )
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should extract from nested data", context.contains("Nested User"))
        assertTrue("Should extract age from nested", context.contains("24"))
        assertTrue("Should extract hobbies from nested", context.contains("Music"))
    }

    @Test
    fun `buildComprehensivePIQContext handles flat data structure`() {
        // Given - PIQ data without nested "data" field
        val piqData = mapOf(
            "fullName" to "Flat User",
            "age" to "26",
            "hobbies" to "Sports"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should extract from flat data", context.contains("Flat User"))
    }

    // ============================================
    // PARTIAL/MISSING DATA TESTS
    // ============================================

    @Test
    fun `buildComprehensivePIQContext handles missing fields gracefully`() {
        // Given - Minimal PIQ data
        val piqData = mapOf(
            "fullName" to "Minimal User"
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should contain name", context.contains("Minimal User"))
        assertTrue("Should contain Not provided for missing fields", 
            context.contains("Not provided") || context.contains("Not specified"))
        assertFalse("Should not throw exception", context.contains("Error"))
    }

    @Test
    fun `buildComprehensivePIQContext handles empty PIQ data`() {
        // Given
        val piqData = emptyMap<String, Any>()

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should return valid context", context.contains("CANDIDATE PROFILE"))
        assertFalse("Should not throw exception", context.contains("Error processing"))
    }

    @Test
    fun `buildComprehensivePIQContext handles null-like values`() {
        // Given
        val piqData = mapOf(
            "fullName" to "",
            "age" to "",
            "hobbies" to "   "  // Whitespace only
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should handle blank values", context.contains("CANDIDATE PROFILE"))
    }

    // ============================================
    // SSB JOURNEY TESTS
    // ============================================

    @Test
    fun `buildComprehensivePIQContext identifies first attempt candidate`() {
        // Given
        val piqData = mapOf(
            "previousInterviews" to emptyList<Map<String, Any>>()
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify first attempt", 
            context.contains("First attempt") || context.contains("Freshie"))
    }

    @Test
    fun `buildComprehensivePIQContext identifies multiple attempt candidate`() {
        // Given
        val piqData = mapOf(
            "previousInterviews" to listOf(
                mapOf("typeOfEntry" to "CDS", "ssbPlace" to "Allahabad"),
                mapOf("typeOfEntry" to "NDA", "ssbPlace" to "Bangalore")
            )
        )

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should identify multiple attempts", context.contains("Multiple attempts"))
    }

    // ============================================
    // OUTPUT FORMAT TESTS
    // ============================================

    @Test
    fun `buildComprehensivePIQContext output contains all sections`() {
        // Given
        val piqData = createCompletePIQData()

        // When
        val context = mapper.buildComprehensivePIQContext(piqData)

        // Then
        assertTrue("Should contain CANDIDATE PROFILE header", context.contains("CANDIDATE PROFILE"))
        assertTrue("Should contain PERSONAL BACKGROUND section", context.contains("PERSONAL BACKGROUND"))
        assertTrue("Should contain FAMILY ENVIRONMENT section", context.contains("FAMILY ENVIRONMENT"))
        assertTrue("Should contain EDUCATION JOURNEY section", context.contains("EDUCATION JOURNEY"))
        assertTrue("Should contain CAREER & WORK section", context.contains("CAREER & WORK"))
        assertTrue("Should contain ACTIVITIES & INTERESTS section", context.contains("ACTIVITIES & INTERESTS"))
        assertTrue("Should contain LEADERSHIP EXPOSURE section", context.contains("LEADERSHIP EXPOSURE"))
        assertTrue("Should contain SSB JOURNEY section", context.contains("SSB JOURNEY"))
        assertTrue("Should contain SELF-ASSESSMENT section", context.contains("SELF-ASSESSMENT"))
        assertTrue("Should contain PERSONALIZATION NOTES section", context.contains("PERSONALIZATION NOTES"))
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private fun createCompletePIQData(): Map<String, Any> {
        return mapOf(
            "fullName" to "John Doe",
            "age" to "25",
            "gender" to "Male",
            "state" to "Maharashtra",
            "district" to "Pune",
            "maritalStatus" to "Single",
            "religion" to "Hindu",
            "motherTongue" to "Marathi",
            "maximumResidencePopulation" to "500000",
            "fatherName" to "Rajesh Doe",
            "fatherOccupation" to "Army Officer",
            "fatherEducation" to "Graduate",
            "motherName" to "Priya Doe",
            "motherOccupation" to "Teacher",
            "motherEducation" to "Post Graduate",
            "parentsAlive" to "Both",
            "siblings" to listOf(
                mapOf("name" to "Jane Doe", "age" to "22", "occupation" to "Student")
            ),
            "education10th" to mapOf(
                "institution" to "ABC School",
                "board" to "CBSE",
                "percentage" to "85"
            ),
            "education12th" to mapOf(
                "institution" to "XYZ School",
                "board" to "CBSE",
                "stream" to "Science",
                "percentage" to "88"
            ),
            "educationGraduation" to mapOf(
                "institution" to "IIT Delhi",
                "board" to "B.Tech",
                "cgpa" to "8.5"
            ),
            "hobbies" to "Reading, Traveling",
            "sportsParticipation" to listOf(
                mapOf("sport" to "Cricket", "representedInstitution" to "College")
            ),
            "nccTraining" to mapOf(
                "hasTraining" to true,
                "wing" to "Army",
                "certificateObtained" to "B Certificate"
            ),
            "choiceOfService" to "Army",
            "whyDefenseForces" to "Serve the nation",
            "strengths" to "Leadership, Communication",
            "weaknesses" to "Impatient at times"
        )
    }
}













