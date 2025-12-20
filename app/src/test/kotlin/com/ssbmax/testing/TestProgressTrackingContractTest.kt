package com.ssbmax.testing

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Contract tests to verify all test types create submission records for progress tracking.
 * 
 * These tests ensure that when any test is completed, a submission record is created
 * in the 'submissions' collection with the correct testType, so it appears in "Your Progress".
 * 
 * BACKGROUND:
 * A bug was discovered where interview completions weren't showing up because:
 * - Interview results were saved to 'interview_results' collection
 * - Progress tracking queries 'submissions' collection
 * 
 * This test ensures ALL test types follow the correct pattern.
 */
class TestProgressTrackingContractTest {

    private val submissionRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/remote/FirestoreSubmissionRepository.kt"
    private val personalRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/remote/PersonalTestSubmissionRepository.kt"
    private val psychRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/remote/PsychTestSubmissionRepository.kt"
    private val commonRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/remote/CommonSubmissionRepository.kt"
    private val progressRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/repository/TestProgressRepositoryImpl.kt"

    @Test
    fun `Phase1 tests must create submissions with correct testType`() {
        // Given - Phase 1 tests: OIR, PPDT
        val repoFile = findProjectFile(personalRepoPath)
        assertTrue("PersonalTestSubmissionRepository.kt should exist", repoFile.exists())
        
        val content = repoFile.readText()
        
        // Then - Must have submission methods for Phase 1 tests
        val phase1Tests = listOf(
            "submitOIR" to "OIR",
            "submitPPDT" to "PPDT"
        )
        
        phase1Tests.forEach { (methodName, testType) ->
            assertTrue(
                "Missing $methodName() - Phase 1 test $testType must create submission record",
                content.contains("fun $methodName")
            )
            
            assertTrue(
                "$methodName must set testType to '$testType'",
                content.contains("FIELD_TEST_TYPE to TestType.$testType.name") ||
                content.contains("testType.*$testType")
            )
        }
    }

    @Test
    fun `Phase2 psychology tests must create submissions with correct testType`() {
        // Given - Phase 2 psychology tests: TAT, WAT, SRT, SD
        val repoFile = findProjectFile(psychRepoPath)
        val content = repoFile.readText()
        
        val psychologyTests = listOf(
            "submitTAT" to "TAT",
            "submitWAT" to "WAT",
            "submitSRT" to "SRT",
            "submitSDT" to "SD"
        )
        
        psychologyTests.forEach { (methodName, testType) ->
            assertTrue(
                "Missing $methodName() - Psychology test $testType must create submission record",
                content.contains("fun $methodName")
            )
            
            assertTrue(
                "$methodName must set testType to '$testType'",
                content.contains("FIELD_TEST_TYPE to TestType.$testType.name") ||
                content.contains("testType.*$testType")
            )
        }
    }

    @Test
    fun `TestProgressRepository must query for all Phase1 testTypes`() {
        // Given
        val progressRepoFile = findProjectFile(progressRepoPath)
        assertTrue("TestProgressRepositoryImpl.kt should exist", progressRepoFile.exists())
        
        val content = progressRepoFile.readText()
        
        // Then - Phase1 query must include OIR and PPDT
        assertTrue(
            "getPhase1Progress must query for 'OIR' testType",
            content.contains("\"OIR\"")
        )
        
        assertTrue(
            "getPhase1Progress must query for 'PPDT' testType",
            content.contains("\"PPDT\"")
        )
    }

    @Test
    fun `TestProgressRepository must query for all Phase2 testTypes`() {
        // Given
        val progressRepoFile = findProjectFile(progressRepoPath)
        val content = progressRepoFile.readText()
        
        // Then - Phase2 query must include all psychology tests, GTO, and IO
        val phase2TestTypes = listOf("TAT", "WAT", "SRT", "SD", "GTO", "IO")
        
        phase2TestTypes.forEach { testType ->
            assertTrue(
                "getPhase2Progress must query for '$testType' testType",
                content.contains("\"$testType\"")
            )
        }
    }

    @Test
    fun `all submission methods must write to submissions collection`() {
        // Given
        val commonRepoFile = findProjectFile(commonRepoPath)
        val commonContent = commonRepoFile.readText()
        
        val psychRepoFile = findProjectFile(psychRepoPath)
        val psychContent = psychRepoFile.readText()
        
        val personalRepoFile = findProjectFile(personalRepoPath)
        val personalContent = personalRepoFile.readText()
        
        // Then - All submission repositories must use submissionsCollection
        assertTrue(
            "CommonSubmissionRepository must use 'submissions' collection",
            commonContent.contains("submissionsCollection") ||
            commonContent.contains("collection(\"submissions\")")
        )
        assertTrue(
            "PsychTestSubmissionRepository must use 'submissions' collection",
            psychContent.contains("submissionsCollection") ||
            psychContent.contains("collection(\"submissions\")")
        )
        assertTrue(
            "PersonalTestSubmissionRepository must use 'submissions' collection",
            personalContent.contains("submissionsCollection") ||
            personalContent.contains("collection(\"submissions\")")
        )
    }

    @Test
    fun `interview completion must create submission record`() {
        // Given - Interview uses different repository
        val interviewRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/repository/FirestoreInterviewRepository.kt"
        val interviewRepoFile = findProjectFile(interviewRepoPath)
        assertTrue("FirestoreInterviewRepository.kt should exist", interviewRepoFile.exists())
        
        val content = interviewRepoFile.readText()
        
        // Then - completeInterview must create submission record
        assertTrue(
            "completeInterview() must create submission record in 'submissions' collection",
            content.contains("COLLECTION_SUBMISSIONS") ||
            content.contains("collection(\"submissions\")")
        )
        
        assertTrue(
            "Interview submission must have testType='IO'",
            content.contains("\"IO\"") && content.contains("testType")
        )
    }

    /**
     * Finds a file relative to the project root.
     */
    private fun findProjectFile(relativePath: String): File {
        // Try from project root (most common)
        var file = File(relativePath)
        if (file.exists()) return file
        
        // Try going up from app module
        var dir = File(".").absoluteFile
        repeat(5) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: return File(relativePath)
        }
        
        return File(relativePath)
    }
}

