package com.ssbmax.testing

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

import org.junit.Ignore

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
 *
 * NOTE: Disabled in CI due to file system access. Run manually locally.
 */
@Ignore("Disabled in CI - file system access. Run manually locally.")
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
        // NOTE: After Phase 2 refactoring, PsychTestSubmissionRepository is a facade
        // that delegates to individual repositories. We check both the facade and the delegates.
        val psychRepoFile = findProjectFile(psychRepoPath)
        val psychContent = psychRepoFile.readText()

        val psychologyTests = listOf(
            "submitTAT" to "TAT",
            "submitWAT" to "WAT",
            "submitSRT" to "SRT",
            "submitSDT" to "SD"
        )

        // Check facade has the methods (delegates to individual repos)
        psychologyTests.forEach { (methodName, _) ->
            assertTrue(
                "Missing $methodName() - Psychology test must create submission record",
                psychContent.contains("fun $methodName")
            )
        }

        // Check individual repositories have the implementations with correct testType
        val tatRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/TATSubmissionRepository.kt")
        assertTrue("TATSubmissionRepository.kt should exist", tatRepoFile.exists())
        val tatContent = tatRepoFile.readText()
        assertTrue(
            "submitTAT must set testType to 'TAT'",
            tatContent.contains("FIELD_TEST_TYPE to TestType.TAT.name")
        )

        val watRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/WATSubmissionRepository.kt")
        assertTrue("WATSubmissionRepository.kt should exist", watRepoFile.exists())
        val watContent = watRepoFile.readText()
        assertTrue(
            "submitWAT must set testType to 'WAT'",
            watContent.contains("FIELD_TEST_TYPE to TestType.WAT.name")
        )

        val srtRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/SRTSubmissionRepository.kt")
        assertTrue("SRTSubmissionRepository.kt should exist", srtRepoFile.exists())
        val srtContent = srtRepoFile.readText()
        assertTrue(
            "submitSRT must set testType to 'SRT'",
            srtContent.contains("FIELD_TEST_TYPE to TestType.SRT.name")
        )

        val sdtRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/SDTSubmissionRepository.kt")
        assertTrue("SDTSubmissionRepository.kt should exist", sdtRepoFile.exists())
        val sdtContent = sdtRepoFile.readText()
        assertTrue(
            "submitSDT must set testType to 'SD'",
            sdtContent.contains("FIELD_TEST_TYPE to TestType.SD.name")
        )
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

        val personalRepoFile = findProjectFile(personalRepoPath)
        val personalContent = personalRepoFile.readText()

        // Then - All submission repositories must use submissionsCollection
        assertTrue(
            "CommonSubmissionRepository must use 'submissions' collection",
            commonContent.contains("submissionsCollection") ||
            commonContent.contains("collection(\"submissions\")")
        )
        assertTrue(
            "PersonalTestSubmissionRepository must use 'submissions' collection",
            personalContent.contains("submissionsCollection") ||
            personalContent.contains("collection(\"submissions\")")
        )

        // After Phase 2 refactoring, check individual psychology test repositories
        val tatRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/TATSubmissionRepository.kt")
        val tatContent = tatRepoFile.readText()
        assertTrue(
            "TATSubmissionRepository must use 'submissions' collection",
            tatContent.contains("submissionsCollection") ||
            tatContent.contains("collection(\"submissions\")")
        )

        val watRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/WATSubmissionRepository.kt")
        val watContent = watRepoFile.readText()
        assertTrue(
            "WATSubmissionRepository must use 'submissions' collection",
            watContent.contains("submissionsCollection") ||
            watContent.contains("collection(\"submissions\")")
        )

        val srtRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/SRTSubmissionRepository.kt")
        val srtContent = srtRepoFile.readText()
        assertTrue(
            "SRTSubmissionRepository must use 'submissions' collection",
            srtContent.contains("submissionsCollection") ||
            srtContent.contains("collection(\"submissions\")")
        )

        val sdtRepoFile = findProjectFile("core/data/src/main/kotlin/com/ssbmax/core/data/remote/SDTSubmissionRepository.kt")
        val sdtContent = sdtRepoFile.readText()
        assertTrue(
            "SDTSubmissionRepository must use 'submissions' collection",
            sdtContent.contains("submissionsCollection") ||
            sdtContent.contains("collection(\"submissions\")")
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

