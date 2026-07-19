package com.ssbmax.shared.domain.usecase.study

import com.ssbmax.shared.domain.model.CloudStudyMaterial
import com.ssbmax.shared.domain.repository.StudyContentRepository

/**
 * Use case for getting study materials for a topic
 * Abstracts repository access for study materials listing
 */
class GetStudyMaterialsUseCase constructor(
    private val studyContentRepository: StudyContentRepository
) {
    /**
     * Get all study materials for a specific topic
     * @param topicType The topic category to fetch materials for
     * @return Result containing list of materials or error
     */
    suspend operator fun invoke(topicType: String): Result<List<CloudStudyMaterial>> {
        return studyContentRepository.getStudyMaterials(topicType)
    }
}
