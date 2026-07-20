package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.AttachmentType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * DTO<->domain mapping tests for GitLiveStudyContentRepository -- same
 * testable-internal-DTO convention as UserDtoTest/UserProfileDtoTest.
 */
class StudyContentDtoTest {

    @Test
    fun `TopicContentDto maps every field to domain`() {
        val dto = TopicContentDto(
            id = "OIR", topicType = "OIR", title = "Officer Intelligence Rating",
            introduction = "intro", version = 2, lastUpdated = 100L,
            isPremium = true, estimatedReadTime = 10
        )

        val domain = dto.toDomain()

        assertEquals("OIR", domain.id)
        assertEquals("Officer Intelligence Rating", domain.title)
        assertEquals(2, domain.version)
        assertEquals(true, domain.isPremium)
        assertEquals(10, domain.estimatedReadTime)
    }

    @Test
    fun `CloudAttachmentDto falls back to IMAGE for an unrecognized type same as unrecognized-enum handling elsewhere`() {
        val dto = CloudAttachmentDto(id = "a1", type = "SOME_FUTURE_TYPE", storagePath = "path/x")

        assertEquals(AttachmentType.IMAGE, dto.toDomain().type)
    }

    @Test
    fun `CloudAttachmentDto maps a recognized type correctly`() {
        val dto = CloudAttachmentDto(id = "a1", type = "PDF", storagePath = "path/x", fileName = "doc.pdf", sizeBytes = 1024L)

        val domain = dto.toDomain()

        assertEquals(AttachmentType.PDF, domain.type)
        assertEquals(1024L, domain.sizeBytes)
        assertEquals(null, domain.downloadUrl)
    }

    @Test
    fun `CloudStudyMaterialDto maps nested attachments and preserves ordering fields`() {
        val dto = CloudStudyMaterialDto(
            id = "m1", topicType = "OIR", title = "Material 1", displayOrder = 3,
            tags = listOf("verbal", "reasoning"),
            attachments = listOf(CloudAttachmentDto(id = "a1", type = "IMAGE", storagePath = "p1"))
        )

        val domain = dto.toDomain()

        assertEquals(3, domain.displayOrder)
        assertEquals(listOf("verbal", "reasoning"), domain.tags)
        assertEquals(1, domain.attachments.size)
        assertEquals("a1", domain.attachments.first().id)
    }

    @Test
    fun `ContentVersionDto defaults match the Android original's version-1 fallback`() {
        val domain = ContentVersionDto().toDomain()

        assertEquals(1, domain.topicsVersion)
        assertEquals(1, domain.materialsVersion)
        assertEquals(0L, domain.lastUpdated)
    }
}
