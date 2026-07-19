package com.ssbmax.shared.domain.repository

import com.ssbmax.shared.domain.model.TestType

/**
 * Domain interface for recording test usage against subscription limits.
 *
 * Implemented by SubscriptionManager in core:data.
 * Lives in domain so that SubmitOIRTestUseCase (and future submit use cases)
 * can depend on it without a core:data import.
 */
interface TestUsageRecorder {
    /**
     * Record that the user has consumed one test attempt for the given [testType].
     * @param testType The type of test that was completed.
     * @param userId   The authenticated user's ID.
     * @param submissionId Optional submission document ID for audit trail.
     */
    suspend fun recordTestUsage(
        testType: TestType,
        userId: String,
        submissionId: String? = null
    )
}
