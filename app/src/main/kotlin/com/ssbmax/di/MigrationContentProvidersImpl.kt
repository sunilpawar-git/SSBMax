package com.ssbmax.di

import com.ssbmax.core.data.repository.MigrationContentProviders
import com.ssbmax.ui.study.StudyMaterialContentProvider
import com.ssbmax.ui.topic.StudyMaterialsProvider
import com.ssbmax.ui.topic.TopicContentLoader
import javax.inject.Inject

/**
 * Bridge implementation that adapts UI layer providers to MigrationContentProviders interface
 *
 * Architecture Note:
 * This is a temporary adapter pattern to allow data layer (MigrationRepositoryImpl) to access
 * UI layer providers during the migration process. This bridge should be removed once the
 * UI layer providers are refactored into proper repository implementations in the data layer.
 *
 * This class lives in the app layer because:
 * 1. It depends on UI layer providers (which can't be moved yet without breaking existing code)
 * 2. It's injected into the data layer as an interface implementation
 * 3. It will be deleted after provider refactoring is complete
 */
class MigrationContentProvidersImpl @Inject constructor(
    // No dependencies needed - using singletons for now
    // TODO: Refactor providers to be injectable
) : MigrationContentProviders {

    override fun getTopicInfo(topicType: String): MigrationContentProviders.TopicInfo {
        val topicInfo = TopicContentLoader.getTopicInfo(topicType)
        return MigrationContentProviders.TopicInfo(
            title = topicInfo.title,
            introduction = topicInfo.introduction
        )
    }

    override fun getStudyMaterials(topicType: String): List<MigrationContentProviders.MaterialItem> {
        val materials = StudyMaterialsProvider.getStudyMaterials(topicType)
        return materials.map { material ->
            MigrationContentProviders.MaterialItem(
                id = material.id,
                duration = material.duration,
                isPremium = material.isPremium
            )
        }
    }

    override fun getMaterialContent(materialId: String): MigrationContentProviders.MaterialContent {
        val content = StudyMaterialContentProvider.getMaterial(materialId)
        return MigrationContentProviders.MaterialContent(
            title = content.title,
            category = content.category,
            content = content.content,
            author = content.author,
            publishedDate = content.publishedDate,
            tags = extractTags(materialId, content.category)
        )
    }

    /**
     * Extract tags from material ID and category
     * This is a helper method since the original content doesn't have explicit tags
     */
    private fun extractTags(materialId: String, category: String): List<String> {
        val topicTag = when {
            materialId.startsWith("oir_") -> listOf("OIR", "Screening", "Intelligence Test")
            materialId.startsWith("ppdt_") -> listOf("PPDT", "Screening", "Picture Test")
            materialId.startsWith("psy_") -> listOf("Psychology", "TAT", "WAT", "SRT")
            materialId.startsWith("gto_") -> listOf("GTO", "Group Testing", "Leadership")
            materialId.startsWith("int_") -> listOf("Interview", "Personal Interview")
            materialId.startsWith("piq_") -> listOf("PIQ", "Personal Information")
            materialId.startsWith("ssb_") -> listOf("SSB Overview", "General")
            materialId.startsWith("med_") -> listOf("Medicals", "Health")
            materialId.startsWith("conf_") -> listOf("Conference", "Final Stage")
            else -> listOf(category)
        }
        return topicTag
    }
}
