package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.Phase1Progress
import com.ssbmax.core.domain.model.Phase2Progress
import kotlinx.coroutines.flow.Flow

/**
 * Repository for aggregating test progress from submissions
 */
interface TestProgressRepository {
    
    /**
     * Get Phase 1 progress (OIR, PPDT)
     */
    fun getPhase1Progress(userId: String): Flow<Phase1Progress>
    
    /**
     * Get Phase 2 progress (Psychology, GTO, Interview)
     */
    fun getPhase2Progress(userId: String): Flow<Phase2Progress>
}

