package com.ssbmax.workers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ArchivalWorker
 *
 * Note: ArchivalWorker resolves its dependencies via Koin's `KoinComponent`/
 * `by inject()` (converted from Hilt's `@HiltWorker`/`@AssistedInject`).
 * Testing a worker that resolves from a live Koin graph requires a started
 * Koin instance, which is out of scope for a plain unit test. The actual
 * business logic (archiveOldSubmissions) is tested in
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
    fun `scheduler uses the same unique work name archival was scheduled under pre-shim`() {
        // WORK_NAME (and the scheduling logic that used it) moved to
        // com.ssbmax.shared.platform.worker.WorkManagerBackgroundTaskScheduler
        // (Phase 4 platform shim) -- verify the literal is unchanged there,
        // so WorkManager's uniqueWork identity stays stable across the move.
        val workNameField = com.ssbmax.shared.platform.worker.WorkManagerBackgroundTaskScheduler::class.java
            .getDeclaredField("ARCHIVAL_WORK_NAME")
        workNameField.isAccessible = true
        val workName = workNameField.get(null) as String

        assertTrue("ARCHIVAL_WORK_NAME should not be empty", workName.isNotEmpty())
        assertEquals("ARCHIVAL_WORK_NAME should be 'archival_worker'", "archival_worker", workName)
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
