package com.ssbmax.shared.domain.repository

import com.ssbmax.shared.domain.model.TestType

/**
 * Repository interface for managing test sessions.
 * Extracted from [TestContentRepository] to separate content concerns from session lifecycle.
 */
interface TestSessionRepository {

    /** Returns true if the user has an active (non-expired) session for the given test. */
    suspend fun hasActiveTestSession(userId: String, testId: String): Result<Boolean>

    /**
     * Creates a test session for the user. Must be called before accessing test content.
     * @return The new session ID on success.
     */
    suspend fun createTestSession(userId: String, testId: String, testType: TestType): Result<String>

    /** Ends the test session, invalidating further access to test content. */
    suspend fun endTestSession(sessionId: String): Result<Unit>
}
