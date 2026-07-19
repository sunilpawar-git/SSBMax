package com.ssbmax.shared.domain.repository

import com.ssbmax.shared.domain.model.OIRTestResult

/**
 * New for the Phase 0 KMP spike. In the existing Android app there is no
 * dedicated OIR-result repository — `OIRSubmissionResultViewModel` calls the
 * generic `SubmissionRepository.getSubmission(id): Result<Map<String, Any>?>`
 * directly and parses the raw Firestore map inline (~110 lines in the
 * ViewModel). That inline parsing is real business logic, so this spike
 * extracts it behind a proper repository interface + use case instead of
 * porting the anti-pattern verbatim (Rule 2: simplicity first, but not at
 * the cost of copying a known code smell into the new SSOT module).
 */
interface OirResultRepository {
    suspend fun getOirResult(submissionId: String): Result<OIRTestResult?>
}
