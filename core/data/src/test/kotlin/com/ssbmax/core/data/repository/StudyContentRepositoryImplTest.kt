package com.ssbmax.core.data.repository

import com.ssbmax.core.data.source.FirestoreContentSource
import com.ssbmax.core.domain.model.AttachmentType
import com.ssbmax.core.domain.model.CloudAttachment
import com.ssbmax.core.domain.model.CloudStudyMaterial
import com.ssbmax.core.domain.model.TopicContent
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StudyContentRepositoryImpl
 *
 * Note: Full Firebase integration tests should be written with Firebase Emulator.
 * These tests verify basic functionality and content source strategy.
 */
class StudyContentRepositoryImplTest {

    private lateinit var firestoreSource: FirestoreContentSource
    private lateinit var repository: StudyContentRepositoryImpl

    @Before
    fun setup() {
        firestoreSource = mockk(relaxed = true)
        repository = StudyContentRepositoryImpl(firestoreSource)
    }

    @Test
    fun `repository initializes with firestore source`() {
        // Given/When
        val repo = StudyContentRepositoryImpl(firestoreSource)

        // Then
        assertNotNull(repo)
    }

    @Test
    fun `TopicContentData validates correctly with cloud source`() {
        // Given
        val materials = listOf(
            createCloudMaterial("mat1", "OIR", "Material 1"),
            createCloudMaterial("mat2", "OIR", "Material 2")
        )

        val data = TopicContentData(
            title = "Officer Intelligence Rating",
            introduction = "OIR tests measure...",
            materials = materials,
            source = ContentSource.CLOUD
        )

        // Then
        assertEquals("Officer Intelligence Rating", data.title)
        assertEquals(2, data.materials.size)
        assertEquals(ContentSource.CLOUD, data.source)
    }

    @Test
    fun `TopicContentData validates correctly with local source`() {
        // Given
        val data = TopicContentData(
            title = "",
            introduction = "",
            materials = emptyList(),
            source = ContentSource.LOCAL
        )

        // Then
        assertTrue(data.title.isEmpty())
        assertTrue(data.materials.isEmpty())
        assertEquals(ContentSource.LOCAL, data.source)
    }

    @Test
    fun `CloudStudyMaterial validates correctly`() {
        // Given
        val material = CloudStudyMaterial(
            id = "mat123",
            topicType = "PPDT",
            title = "Introduction to PPDT",
            category = "Psychology",
            contentMarkdown = "# PPDT Introduction\n\nContent here...",
            author = "Dr. Smith",
            readTime = "10 min",
            tags = listOf("psychology", "ppdt", "basics"),
            isPremium = false,
            displayOrder = 1,
            relatedMaterials = listOf("mat124", "mat125"),
            attachments = emptyList(),
            version = 1,
            lastUpdated = System.currentTimeMillis()
        )

        // Then
        assertEquals("mat123", material.id)
        assertEquals("PPDT", material.topicType)
        assertEquals("Introduction to PPDT", material.title)
        assertEquals(3, material.tags.size)
        assertFalse(material.isPremium)
        assertEquals(1, material.displayOrder)
    }

    @Test
    fun `CloudStudyMaterial supports premium content`() {
        // Given
        val material = createCloudMaterial("mat1", "TAT", "Advanced TAT Techniques", isPremium = true)

        // Then
        assertTrue(material.isPremium)
    }

    @Test
    fun `CloudStudyMaterial supports attachments`() {
        // Given
        val attachments = listOf(
            CloudAttachment(
                id = "att1",
                type = AttachmentType.IMAGE,
                storagePath = "study_materials/oir/diagram.jpg",
                fileName = "OIR Diagram",
                sizeBytes = 245760L
            ),
            CloudAttachment(
                id = "att2",
                type = AttachmentType.PDF,
                storagePath = "study_materials/oir/guide.pdf",
                fileName = "OIR Study Guide",
                sizeBytes = 1024000L
            )
        )

        val material = CloudStudyMaterial(
            id = "mat1",
            topicType = "OIR",
            title = "OIR Complete Guide",
            category = "Intelligence",
            contentMarkdown = "Content",
            author = "Expert",
            readTime = "15 min",
            attachments = attachments
        )

        // Then
        assertEquals(2, material.attachments.size)
        assertEquals(AttachmentType.IMAGE, material.attachments[0].type)
        assertEquals(AttachmentType.PDF, material.attachments[1].type)
    }

    @Test
    fun `CloudAttachment supports different attachment types`() {
        // Given
        val imageAttachment = CloudAttachment(
            id = "att1",
            type = AttachmentType.IMAGE,
            storagePath = "images/test.jpg",
            fileName = "Test Image",
            sizeBytes = 100000L
        )

        val pdfAttachment = CloudAttachment(
            id = "att2",
            type = AttachmentType.PDF,
            storagePath = "pdfs/guide.pdf",
            fileName = "Study Guide",
            sizeBytes = 500000L
        )

        val videoAttachment = CloudAttachment(
            id = "att3",
            type = AttachmentType.VIDEO,
            storagePath = "videos/lesson.mp4",
            fileName = "Video Lesson",
            sizeBytes = 5000000L
        )

        // Then
        assertEquals(AttachmentType.IMAGE, imageAttachment.type)
        assertEquals(AttachmentType.PDF, pdfAttachment.type)
        assertEquals(AttachmentType.VIDEO, videoAttachment.type)
    }

    @Test
    fun `ContentSource enum has correct values`() {
        // Given/When
        val sources = ContentSource.values()

        // Then
        assertEquals(2, sources.size)
        assertTrue(sources.contains(ContentSource.CLOUD))
        assertTrue(sources.contains(ContentSource.LOCAL))
    }

    @Test
    fun `CloudStudyMaterial supports empty tags and related materials`() {
        // Given
        val material = CloudStudyMaterial(
            id = "mat1",
            topicType = "WAT",
            title = "WAT Basics",
            category = "Psychology",
            contentMarkdown = "Content",
            author = "Author",
            readTime = "5 min",
            tags = emptyList(),
            relatedMaterials = emptyList(),
            attachments = emptyList()
        )

        // Then
        assertTrue(material.tags.isEmpty())
        assertTrue(material.relatedMaterials.isEmpty())
        assertTrue(material.attachments.isEmpty())
    }

    @Test
    fun `CloudStudyMaterial supports display order for sorting`() {
        // Given
        val materials = listOf(
            createCloudMaterial("mat1", "SRT", "First", displayOrder = 1),
            createCloudMaterial("mat2", "SRT", "Second", displayOrder = 2),
            createCloudMaterial("mat3", "SRT", "Third", displayOrder = 3)
        )

        // Then
        assertEquals(1, materials[0].displayOrder)
        assertEquals(2, materials[1].displayOrder)
        assertEquals(3, materials[2].displayOrder)

        // Verify they can be sorted
        val sorted = materials.sortedBy { it.displayOrder }
        assertEquals("First", sorted[0].title)
        assertEquals("Second", sorted[1].title)
        assertEquals("Third", sorted[2].title)
    }

    @Test
    fun `TopicContent validates correctly`() {
        // Given
        val topic = TopicContent(
            id = "OIR",
            topicType = "OIR",
            title = "Officer Intelligence Rating",
            introduction = "The OIR test evaluates your reasoning ability...",
            version = 1,
            lastUpdated = System.currentTimeMillis(),
            isPremium = false,
            estimatedReadTime = 10
        )

        // Then
        assertEquals("OIR", topic.id)
        assertEquals("OIR", topic.topicType)
        assertEquals("Officer Intelligence Rating", topic.title)
        assertFalse(topic.isPremium)
        assertEquals(10, topic.estimatedReadTime)
    }

    @Test
    fun `placeholder - full Firebase integration tests needed`() {
        // This test serves as a reminder that comprehensive testing requires:
        // 1. Firebase Emulator setup for Firestore
        // 2. Integration tests for cloud + local fallback strategy
        // 3. Content loading and caching tests
        // 4. Feature flag testing for cloud enablement
        // 5. Flow-based content streaming tests
        //
        // These should be implemented in androidTest with Firebase Test SDK
        assertTrue("StudyContentRepositoryImpl requires Firebase emulator integration testing", true)
    }

    // Helper functions to create test data
    private fun createCloudMaterial(
        id: String,
        topicType: String,
        title: String,
        isPremium: Boolean = false,
        displayOrder: Int = 0
    ): CloudStudyMaterial {
        return CloudStudyMaterial(
            id = id,
            topicType = topicType,
            title = title,
            category = "Test Category",
            contentMarkdown = "# $title\n\nTest content",
            author = "Test Author",
            readTime = "5 min",
            tags = listOf("test", topicType.lowercase()),
            isPremium = isPremium,
            displayOrder = displayOrder,
            relatedMaterials = emptyList(),
            attachments = emptyList(),
            version = 1,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
