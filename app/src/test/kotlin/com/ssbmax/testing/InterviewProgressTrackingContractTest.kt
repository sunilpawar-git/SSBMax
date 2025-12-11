package com.ssbmax.testing

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Contract tests to verify interview progress tracking implementation.
 * 
 * These tests verify that the codebase maintains the correct architecture
 * for tracking interview completions in the progress section.
 * 
 * BACKGROUND:
 * A bug was discovered where interview completions weren't showing up
 * in "Your Progress" because:
 * - Interview results were saved to 'interview_results' collection
 * - Progress tracking queries 'submissions' collection for testType='IO'
 * 
 * These tests ensure the fix remains in place and prevent regression.
 */
class InterviewProgressTrackingContractTest {

    private val firestoreInterviewRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/repository/FirestoreInterviewRepository.kt"
    private val testProgressRepoPath = "core/data/src/main/kotlin/com/ssbmax/core/data/repository/TestProgressRepositoryImpl.kt"

    @Test
    fun `completeInterview must write to submissions collection for progress tracking`() {
        // Given - The FirestoreInterviewRepository implementation
        val repoFile = findProjectFile(firestoreInterviewRepoPath)
        assertTrue("FirestoreInterviewRepository.kt should exist", repoFile.exists())
        
        val content = repoFile.readText()
        
        // Then - It must reference the submissions collection
        assertTrue(
            "completeInterview should save to 'submissions' collection for progress tracking. " +
            "Without this, interviews won't appear in 'Your Progress' section.",
            content.contains("COLLECTION_SUBMISSIONS") || 
            content.contains("collection(\"submissions\")")
        )
    }

    @Test
    fun `completeInterview must set testType to IO for interview submissions`() {
        // Given
        val repoFile = findProjectFile(firestoreInterviewRepoPath)
        val content = repoFile.readText()
        
        // Then - It must set testType to "IO" (not "INTERVIEW" or anything else)
        // because TestProgressRepositoryImpl queries for testType == "IO"
        assertTrue(
            "Interview submission must have testType='IO' to match TestProgressRepositoryImpl query",
            content.contains("\"IO\"") && content.contains("testType")
        )
    }

    @Test
    fun `TestProgressRepository must query for IO testType in Phase2`() {
        // Given - The TestProgressRepositoryImpl that queries for progress
        val progressRepoFile = findProjectFile(testProgressRepoPath)
        assertTrue("TestProgressRepositoryImpl.kt should exist", progressRepoFile.exists())
        
        val content = progressRepoFile.readText()
        
        // Then - Phase2 query must include "IO" for interviews
        assertTrue(
            "getPhase2Progress must query for 'IO' testType to track interviews",
            content.contains("\"IO\"")
        )
    }

    @Test
    fun `submissions collection constants must be defined in interview repository`() {
        // Given
        val repoFile = findProjectFile(firestoreInterviewRepoPath)
        val content = repoFile.readText()
        
        // Then - Required constants for submission creation
        val requiredPatterns = listOf(
            "COLLECTION_SUBMISSIONS" to "submissions collection constant",
            "FIELD_TEST_TYPE" to "testType field constant",
            "FIELD_SUBMITTED_AT" to "submittedAt field constant"
        )
        
        requiredPatterns.forEach { (pattern, description) ->
            assertTrue(
                "Missing $description: $pattern is required for progress tracking",
                content.contains(pattern)
            )
        }
    }

    /**
     * Finds a file relative to the project root.
     * Handles both running from IDE (module root) and Gradle (project root).
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

