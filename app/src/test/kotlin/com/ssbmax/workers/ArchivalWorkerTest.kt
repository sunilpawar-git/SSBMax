package com.ssbmax.workers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ArchivalWorker
 *
 * Note: ArchivalWorker uses Hilt for dependency injection (@HiltWorker).
 * Testing Hilt workers requires HiltAndroidTest setup which is complex.
 * The actual business logic (archiveOldSubmissions) is tested in
 * SubmissionRepositoryTest. This test verifies the Worker constants.
 */
class ArchivalWorkerTest {

    @Test
    fun `worker has correct retry limit`() {
        // Verify max retries constant via reflection
        val maxRetriesField = ArchivalWorker::class.java.getDeclaredField("MAX_RETRIES")
        maxRetriesField.isAccessible = true
        val maxRetries = maxRetriesField.get(null) as Int

        assertEquals("MAX_RETRIES should be 3", 3, maxRetries)
    }

    @Test
    fun `worker has unique work name`() {
        // Verify work name constant
        val workNameField = ArchivalWorker::class.java.getDeclaredField("WORK_NAME")
        workNameField.isAccessible = true
        val workName = workNameField.get(null) as String

        assertTrue("WORK_NAME should not be empty", workName.isNotEmpty())
        assertEquals("WORK_NAME should be 'archival_worker'", "archival_worker", workName)
    }

    @Test
    fun `worker tag is consistent`() {
        // Verify TAG constant
        val tagField = ArchivalWorker::class.java.getDeclaredField("TAG")
        tagField.isAccessible = true
        val tag = tagField.get(null) as String

        assertEquals("TAG should be 'ArchivalWorker'", "ArchivalWorker", tag)
    }
}
