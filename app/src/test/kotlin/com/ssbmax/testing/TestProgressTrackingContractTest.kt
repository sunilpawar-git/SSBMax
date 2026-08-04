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

    // Move 2 (iOS CocoaPods->SPM convergence): the GitLive* repositories moved
    // from :shared to :data-firebase, so :shared carries no Firebase and its
    // Kotlin/Native test binaries link without CocoaPods. These paths are
    // repointed with that move -- this test is @Ignore'd (file-system access),
    // so a stale path would rot silently rather than fail.
    // KMP-convergence Phase 9e: PersonalTestSubmissionRepository/PsychTestSubmissionRepository/
    // CommonSubmissionRepository (core:data) deleted, GitLive* equivalents (shared) are the sole
    // implementations now — repointed rather than left referencing files that no longer exist.
    private val repoDir = "data-firebase/src/commonMain/kotlin/com/ssbmax/shared/data/repository"
    private val personalRepoPath = "$repoDir/GitLivePersonalTestSubmissionRepository.kt"
    private val psychRepoPath = "$repoDir/GitLivePsychTestSubmissionRepository.kt"
    private val commonRepoPath = "$repoDir/GitLiveCommonSubmissionRepository.kt"
    private val tatRepoPath = "$repoDir/GitLivePsychTestSubmissionRepository.kt"
    private val watRepoPath = "$repoDir/GitLiveWATSubmissionDelegate.kt"
    private val srtRepoPath = "$repoDir/GitLiveSRTSubmissionDelegate.kt"
    private val sdtRepoPath = "$repoDir/GitLiveSDTSubmissionDelegate.kt"
    // KMP-convergence Phase 9a: TestProgressRepositoryImpl (core:data) deleted,
    // GitLiveTestProgressRepository (shared) is the sole implementation now —
    // repointed rather than left referencing a file that no longer exists.
    private val progressRepoPath = "$repoDir/GitLiveTestProgressRepository.kt"

    @Test
    fun `Phase1 tests must create submissions with correct testType`() {
        // Given - Phase 1 tests: OIR, PPDT
        val repoFile = findProjectFile(personalRepoPath)
        assertTrue("GitLivePersonalTestSubmissionRepository.kt should exist", repoFile.exists())

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
                content.contains("testType = TestType.$testType.name")
            )
        }
    }

    @Test
    fun `Phase2 psychology tests must create submissions with correct testType`() {
        // Given - Phase 2 psychology tests: TAT, WAT, SRT, SD
        // NOTE: TAT stays on the GitLivePsychTestSubmissionRepository facade; WAT/SRT/SDT are
        // delegated to their own GitLive*SubmissionDelegate classes (structural split to stay
        // under the 300-line-per-file limit — see GitLivePsychTestSubmissionRepository's class doc).
        val psychRepoFile = findProjectFile(psychRepoPath)
        val psychContent = psychRepoFile.readText()

        // TAT stays directly on the facade
        assertTrue(
            "Missing submitTAT() - Psychology test must create submission record",
            psychContent.contains("fun submitTAT")
        )
        // WAT/SRT/SDT are delegated (still reachable through the facade's public API)
        listOf("submitWAT", "submitSRT", "submitSDT").forEach { methodName ->
            assertTrue(
                "Missing $methodName() delegation - Psychology test must create submission record",
                psychContent.contains("fun $methodName")
            )
        }

        // Check individual repositories have the implementations with correct testType
        val tatRepoFile = findProjectFile(tatRepoPath)
        assertTrue("GitLivePsychTestSubmissionRepository.kt should exist", tatRepoFile.exists())
        val tatContent = tatRepoFile.readText()
        assertTrue(
            "submitTAT must set testType to 'TAT'",
            tatContent.contains("testType = TestType.TAT.name")
        )

        val watRepoFile = findProjectFile(watRepoPath)
        assertTrue("GitLiveWATSubmissionDelegate.kt should exist", watRepoFile.exists())
        val watContent = watRepoFile.readText()
        assertTrue(
            "submitWAT must set testType to 'WAT'",
            watContent.contains("testType = TestType.WAT.name")
        )

        val srtRepoFile = findProjectFile(srtRepoPath)
        assertTrue("GitLiveSRTSubmissionDelegate.kt should exist", srtRepoFile.exists())
        val srtContent = srtRepoFile.readText()
        assertTrue(
            "submitSRT must set testType to 'SRT'",
            srtContent.contains("testType = TestType.SRT.name")
        )

        val sdtRepoFile = findProjectFile(sdtRepoPath)
        assertTrue("GitLiveSDTSubmissionDelegate.kt should exist", sdtRepoFile.exists())
        val sdtContent = sdtRepoFile.readText()
        assertTrue(
            "submitSDT must set testType to 'SD'",
            sdtContent.contains("testType = TestType.SD.name")
        )
    }

    @Test
    fun `TestProgressRepository must query for all Phase1 testTypes`() {
        // Given
        val progressRepoFile = findProjectFile(progressRepoPath)
        assertTrue("GitLiveTestProgressRepository.kt should exist", progressRepoFile.exists())

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
            "GitLiveCommonSubmissionRepository must use 'submissions' collection",
            commonContent.contains("submissionsCollection") ||
            commonContent.contains("collection(\"submissions\")") ||
            commonContent.contains("collection(SUBMISSIONS_COLLECTION)")
        )
        assertTrue(
            "GitLivePersonalTestSubmissionRepository must use 'submissions' collection",
            personalContent.contains("submissionsCollection") ||
            personalContent.contains("collection(\"submissions\")") ||
            personalContent.contains("collection(SUBMISSIONS_COLLECTION)")
        )

        val tatRepoFile = findProjectFile(tatRepoPath)
        val tatContent = tatRepoFile.readText()
        assertTrue(
            "GitLivePsychTestSubmissionRepository must use 'submissions' collection",
            tatContent.contains("submissionsCollection") ||
            tatContent.contains("collection(\"submissions\")")
        )

        val watRepoFile = findProjectFile(watRepoPath)
        val watContent = watRepoFile.readText()
        assertTrue(
            "GitLiveWATSubmissionDelegate must use 'submissions' collection",
            watContent.contains("submissionsCollection") ||
            watContent.contains("collection(\"submissions\")")
        )

        val srtRepoFile = findProjectFile(srtRepoPath)
        val srtContent = srtRepoFile.readText()
        assertTrue(
            "GitLiveSRTSubmissionDelegate must use 'submissions' collection",
            srtContent.contains("submissionsCollection") ||
            srtContent.contains("collection(\"submissions\")")
        )

        val sdtRepoFile = findProjectFile(sdtRepoPath)
        val sdtContent = sdtRepoFile.readText()
        assertTrue(
            "GitLiveSDTSubmissionDelegate must use 'submissions' collection",
            sdtContent.contains("submissionsCollection") ||
            sdtContent.contains("collection(\"submissions\")")
        )
    }

    @Test
    fun `interview completion must create submission record`() {
        // Given - Interview uses different repository. KMP-convergence Phase 9c:
        // FirestoreInterviewRepository (core:data) deleted, GitLiveInterviewRepository
        // (shared) is the sole implementation now.
        val interviewRepoPath = "data-firebase/src/commonMain/kotlin/com/ssbmax/shared/data/repository/GitLiveInterviewRepository.kt"
        val interviewRepoFile = findProjectFile(interviewRepoPath)
        assertTrue("GitLiveInterviewRepository.kt should exist", interviewRepoFile.exists())

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
