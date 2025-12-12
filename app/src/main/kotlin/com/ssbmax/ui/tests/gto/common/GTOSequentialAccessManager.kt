package com.ssbmax.ui.tests.gto.common

import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.utils.ErrorLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for enforcing sequential access to GTO tests
 * 
 * Ensures users complete tests in the correct order:
 * 1. Group Discussion
 * 2. Group Planning Exercise
 * 3. Lecturette
 * 4. Progressive Group Task
 * 5. Half Group Task
 * 6. Group Obstacle Race
 * 7. Individual Obstacles
 * 8. Command Task
 * 
 * Users must complete each test before accessing the next one.
 */
@Singleton
class GTOSequentialAccessManager @Inject constructor(
    private val gtoRepository: GTORepository
) {
    
    /**
     * Check if user can access a specific GTO test
     * 
     * @param userId User ID
     * @param testType Test to check access for
     * @return Pair of (canAccess, errorMessage)
     *         - If canAccess is true, errorMessage is null
     *         - If canAccess is false, errorMessage explains what's needed
     */
    suspend fun checkAccess(
        userId: String,
        testType: GTOTestType
    ): Pair<Boolean, String?> {
        return try {
            // Get user's completed tests
            val completedTests = gtoRepository.getCompletedTests(userId).getOrNull() ?: emptyList()
            
            // Check if all prerequisite tests are completed
            val prerequisites = GTOTestType.getPrerequisites(testType)
            val missingTests = prerequisites.filterNot { it in completedTests }
            
            if (missingTests.isEmpty()) {
                // User can access this test
                true to null
            } else {
                // User is missing prerequisite tests
                val missingNames = missingTests.joinToString(", ") { it.displayName }
                val message = buildAccessDeniedMessage(testType, missingTests)
                false to message
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to check GTO test access")
            false to "Unable to verify test access. Please try again."
        }
    }
    
    /**
     * Get the next available test for the user
     * 
     * @param userId User ID
     * @return Next test type to complete, or null if all tests are done
     */
    suspend fun getNextTest(userId: String): GTOTestType? {
        return try {
            val completedTests = gtoRepository.getCompletedTests(userId).getOrNull() ?: emptyList()
            
            // Find the first test that hasn't been completed
            GTOTestType.entries
                .sortedBy { it.order }
                .firstOrNull { it !in completedTests }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to get next GTO test")
            null
        }
    }
    
    /**
     * Get user's progress through GTO tests
     * 
     * @param userId User ID
     * @return GTOAccessProgress with completion status
     */
    suspend fun getProgress(userId: String): GTOAccessProgress {
        return try {
            val completedTests = gtoRepository.getCompletedTests(userId).getOrNull() ?: emptyList()
            val nextTest = getNextTest(userId)
            
            GTOAccessProgress(
                completedTests = completedTests,
                nextTest = nextTest,
                totalTests = GTOTestType.entries.size,
                completionPercentage = (completedTests.size.toFloat() / GTOTestType.entries.size) * 100f,
                isComplete = completedTests.size == GTOTestType.entries.size
            )
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to get GTO progress")
            GTOAccessProgress(
                completedTests = emptyList(),
                nextTest = GTOTestType.GROUP_DISCUSSION,
                totalTests = GTOTestType.entries.size,
                completionPercentage = 0f,
                isComplete = false
            )
        }
    }
    
    /**
     * Validate if a test submission should be allowed
     * This is a final check before accepting a submission
     * 
     * @param userId User ID
     * @param testType Test being submitted
     * @return true if submission should be accepted
     */
    suspend fun validateSubmission(
        userId: String,
        testType: GTOTestType
    ): Boolean {
        val (canAccess, _) = checkAccess(userId, testType)
        return canAccess
    }
    
    /**
     * Build a user-friendly error message for access denial
     */
    private fun buildAccessDeniedMessage(
        testType: GTOTestType,
        missingTests: List<GTOTestType>
    ): String {
        return when {
            missingTests.size == 1 -> {
                "Complete ${missingTests.first().displayName} before accessing ${testType.displayName}"
            }
            missingTests.size == 2 -> {
                "Complete ${missingTests[0].displayName} and ${missingTests[1].displayName} before accessing ${testType.displayName}"
            }
            else -> {
                val allButLast = missingTests.dropLast(1).joinToString(", ") { it.displayName }
                val last = missingTests.last().displayName
                "Complete $allButLast, and $last before accessing ${testType.displayName}"
            }
        }
    }
}

/**
 * User's progress through GTO test sequence
 */
data class GTOAccessProgress(
    val completedTests: List<GTOTestType>,
    val nextTest: GTOTestType?,
    val totalTests: Int,
    val completionPercentage: Float,
    val isComplete: Boolean
) {
    /**
     * Check if a specific test is accessible
     */
    fun isTestAccessible(testType: GTOTestType): Boolean {
        val prerequisites = GTOTestType.getPrerequisites(testType)
        return prerequisites.all { it in completedTests }
    }
    
    /**
     * Get the status of a specific test
     */
    fun getTestStatus(testType: GTOTestType): GTOTestStatus {
        return when {
            testType in completedTests -> GTOTestStatus.COMPLETED
            isTestAccessible(testType) -> GTOTestStatus.AVAILABLE
            else -> GTOTestStatus.LOCKED
        }
    }
}

/**
 * Status of a GTO test for the user
 */
enum class GTOTestStatus {
    LOCKED,      // Prerequisites not met
    AVAILABLE,   // Can be taken now
    COMPLETED    // Already completed
}
