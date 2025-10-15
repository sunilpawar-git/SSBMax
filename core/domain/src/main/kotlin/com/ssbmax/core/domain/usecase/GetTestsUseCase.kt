package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.SSBCategory
import com.ssbmax.core.domain.model.SSBTest
import com.ssbmax.core.domain.repository.TestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving SSB tests
 * Encapsulates business logic for fetching tests
 */
class GetTestsUseCase @Inject constructor(
    private val repository: TestRepository
) {
    /**
     * Get all tests for a specific SSB category
     *
     * @param category The SSB category to filter by
     * @return Flow of Result containing list of tests
     */
    operator fun invoke(category: SSBCategory): Flow<Result<List<SSBTest>>> {
        return repository.getTests(category)
    }
    
    /**
     * Get all tests (overload)
     */
    operator fun invoke(): Flow<Result<List<SSBTest>>> {
        // For getting all tests, we'd need to combine all categories
        // For now, return psychology tests as default
        return repository.getTests(SSBCategory.PSYCHOLOGY)
    }
}

