package com.ssbmax.core.domain.usecase.study

import com.ssbmax.core.domain.model.CloudStudyMaterial
import com.ssbmax.core.domain.repository.StudyContentRepository
import javax.inject.Inject

/**
 * Use case for getting study materials for a topic
 * Abstracts repository access for study materials listing
 */
class GetStudyMaterialsUseCase @Inject constructor(
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
