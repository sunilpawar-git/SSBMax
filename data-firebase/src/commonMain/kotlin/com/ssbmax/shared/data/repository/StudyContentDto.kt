package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.AttachmentType
import com.ssbmax.shared.domain.model.CloudAttachment
import com.ssbmax.shared.domain.model.CloudStudyMaterial
import com.ssbmax.shared.domain.model.ContentVersion
import com.ssbmax.shared.domain.model.TopicContent
import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs for GitLiveStudyContentRepository, mirroring the domain
 * models 1:1 (same convention as UserDto/UserProfileDto elsewhere in this
 * module -- a separate @Serializable DTO family rather than annotating the
 * domain model directly, so `shared/commonMain/domain` stays free of
 * serialization-library coupling).
 */
@Serializable
internal data class TopicContentDto(
    val id: String = "",
    val topicType: String = "",
    val title: String = "",
    val introduction: String = "",
    val version: Int = 1,
    val lastUpdated: Long = 0L,
    val isPremium: Boolean = false,
    val estimatedReadTime: Int = 5
) {
    fun toDomain() = TopicContent(
        id = id,
        topicType = topicType,
        title = title,
        introduction = introduction,
        version = version,
        lastUpdated = lastUpdated,
        isPremium = isPremium,
        estimatedReadTime = estimatedReadTime
    )
}

@Serializable
internal data class CloudAttachmentDto(
    val id: String = "",
    val type: String = AttachmentType.IMAGE.name,
    val storagePath: String = "",
    val fileName: String = "",
    val sizeBytes: Long = 0L
) {
    fun toDomain() = CloudAttachment(
        id = id,
        type = runCatching { AttachmentType.valueOf(type) }.getOrDefault(AttachmentType.IMAGE),
        storagePath = storagePath,
        fileName = fileName,
        sizeBytes = sizeBytes,
        downloadUrl = null
    )
}

@Serializable
internal data class CloudStudyMaterialDto(
    val id: String = "",
    val topicType: String = "",
    val title: String = "",
    val category: String = "",
    val contentMarkdown: String = "",
    val author: String = "",
    val readTime: String = "",
    val tags: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val displayOrder: Int = 0,
    val relatedMaterials: List<String> = emptyList(),
    val attachments: List<CloudAttachmentDto> = emptyList(),
    val version: Int = 1,
    val lastUpdated: Long = 0L
) {
    fun toDomain() = CloudStudyMaterial(
        id = id,
        topicType = topicType,
        title = title,
        category = category,
        contentMarkdown = contentMarkdown,
        author = author,
        readTime = readTime,
        tags = tags,
        isPremium = isPremium,
        displayOrder = displayOrder,
        relatedMaterials = relatedMaterials,
        attachments = attachments.map { it.toDomain() },
        version = version,
        lastUpdated = lastUpdated
    )
}

@Serializable
internal data class ContentVersionDto(
    val topicsVersion: Int = 1,
    val materialsVersion: Int = 1,
    val lastUpdated: Long = 0L
) {
    fun toDomain() = ContentVersion(
        topicsVersion = topicsVersion,
        materialsVersion = materialsVersion,
        lastUpdated = lastUpdated
    )
}
