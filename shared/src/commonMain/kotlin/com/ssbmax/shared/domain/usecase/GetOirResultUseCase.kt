package com.ssbmax.shared.domain.usecase

import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.repository.OirResultRepository

/**
 * New for the Phase 0 KMP spike (see OirResultRepository doc comment for why
 * this exists — it replaces inline parsing that used to live directly in the
 * Android ViewModel). Follows the `{Verb}{Subject}UseCase` naming convention
 * from core/domain/CLAUDE.md.
 */
class GetOirResultUseCase(
    private val repository: OirResultRepository
) {
    suspend operator fun invoke(submissionId: String): Result<OIRTestResult?> {
        return repository.getOirResult(submissionId)
    }
}
