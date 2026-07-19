package com.ssbmax.shared.domain.usecase.study

import com.ssbmax.shared.domain.model.CloudStudyMaterial
import com.ssbmax.shared.domain.repository.StudyContentRepository

/**
 * Use case for getting study material detail
 * Abstracts repository access for single material retrieval
 */
class GetStudyMaterialDetailUseCase constructor(
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
