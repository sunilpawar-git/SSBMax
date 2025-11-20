package com.ssbmax.core.domain.usecase.progress

import com.ssbmax.core.domain.model.Phase1Progress
import com.ssbmax.core.domain.model.Phase2Progress
import com.ssbmax.core.domain.repository.TestProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving test progress across SSB phases
 *
 * This encapsulates the business logic for:
 * - Aggregating progress from multiple test submissions
 * - Calculating completion percentages
 * - Providing reactive progress updates
 */
class GetTestProgressUseCase @Inject constructor(
    private val testProgressRepository: TestProgressRepository
) {
    /**
     * Get Phase 1 progress (OIR, PPDT)
     *
     * @param userId The user ID to fetch progress for
     * @return Flow emitting Phase1Progress that updates when progress changes
     */
    fun getPhase1Progress(userId: String): Flow<Phase1Progress> {
        return testProgressRepository.getPhase1Progress(userId)
    }

    /**
     * Get Phase 2 progress (Psychology, GTO, Interview)
     *
     * @param userId The user ID to fetch progress for
     * @return Flow emitting Phase2Progress that updates when progress changes
     */
    fun getPhase2Progress(userId: String): Flow<Phase2Progress> {
        return testProgressRepository.getPhase2Progress(userId)
    }
}
