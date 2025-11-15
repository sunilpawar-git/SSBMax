package com.ssbmax.core.domain.usecase.study

import com.ssbmax.core.domain.model.CloudStudyMaterial
import com.ssbmax.core.domain.repository.StudyContentRepository
import javax.inject.Inject

/**
 * Use case for getting study material detail
 * Abstracts repository access for single material retrieval
 */
class GetStudyMaterialDetailUseCase @Inject constructor(
    private val studyContentRepository: StudyContentRepository
) {
    /**
     * Get detailed information for a specific study material
     * @param materialId The unique identifier of the material
     * @return Result containing material details or error
     */
    suspend operator fun invoke(materialId: String): Result<CloudStudyMaterial> {
        return studyContentRepository.getStudyMaterial(materialId)
    }
}
