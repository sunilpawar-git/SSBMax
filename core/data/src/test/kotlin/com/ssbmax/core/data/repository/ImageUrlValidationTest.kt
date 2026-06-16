package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.dao.TATImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.URL

/**
 * Validates that TAT and PPDT image URLs are well-formed and loadable.
 *
 * These tests catch common image loading failures:
 * - Empty or blank URLs passed to Coil (silent failure, blank screen)
 * - gs:// URLs not normalized to https:// (Coil can't load gs://)
 * - Malformed URLs that crash or fail at runtime
 * - Mock data fallback producing placeholder URLs in production
 *
 * Run: ./gradlew :core:data:testDebugUnitTest --tests "*.ImageUrlValidationTest"
 */
class ImageUrlValidationTest {

    private val mockTATDao = mockk<TATImageCacheDao>(relaxed = true)
    private val mockPPDTDao = mockk<PPDTImageCacheDao>(relaxed = true)
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)

    private lateinit var tatCacheManager: TATImageCacheManager
    private lateinit var ppdtCacheManager: PPDTImageCacheManager

    @Before
    fun setup() {
        clearAllMocks()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        tatCacheManager = TATImageCacheManager(mockTATDao, mockFirestore)
        ppdtCacheManager = PPDTImageCacheManager(mockPPDTDao, mockFirestore)
    }

    // ==================== Mock Data URL Validation ====================

    @Test
    fun `mock TAT questions have non-empty image URLs`() {
        val questions = MockTestDataProvider.getTATQuestions()
        assertTrue("Mock TAT data should not be empty", questions.isNotEmpty())
        // Blank card (position 12) has empty URL by design — only validate real image cards
        questions.filter { it.id != "blank_card" }.forEach { q ->
            assertTrue(
                "TAT mock ${q.id} imageUrl must not be blank",
                q.imageUrl.isNotBlank()
            )
        }
    }

    @Test
    fun `mock TAT questions have valid URL format`() {
        val questions = MockTestDataProvider.getTATQuestions()
        // Blank card (position 12) has empty URL by design — only validate real image cards
        questions.filter { it.id != "blank_card" }.forEach { q ->
            assertTrue(
                "TAT mock ${q.id} imageUrl must start with https://",
                q.imageUrl.startsWith("https://")
            )
            assertDoesNotThrow("TAT mock ${q.id} imageUrl must be valid URL") {
                URL(q.imageUrl)
            }
        }
    }

    @Test
    fun `mock PPDT questions have non-empty image URLs`() {
        val questions = MockTestDataProvider.getPPDTQuestions()
        assertTrue("Mock PPDT data should not be empty", questions.isNotEmpty())
        questions.forEach { q ->
            assertTrue(
                "PPDT mock ${q.id} imageUrl must not be blank",
                q.imageUrl.isNotBlank()
            )
        }
    }

    @Test
    fun `mock PPDT questions have valid URL format`() {
        val questions = MockTestDataProvider.getPPDTQuestions()
        questions.forEach { q ->
            assertTrue(
                "PPDT mock ${q.id} imageUrl must start with https://",
                q.imageUrl.startsWith("https://")
            )
            assertDoesNotThrow("PPDT mock ${q.id} imageUrl must be valid URL") {
                URL(q.imageUrl)
            }
        }
    }

    // ==================== TAT URL Normalization ====================

    @Test
    fun `TAT cache manager normalizes gs URLs to https`() = runTest {
        val gsImage = createTATEntity("tat_gs", "gs://my-bucket/tat/image001.jpg")
        val fallback = createTATEntity("fallback", "https://storage.googleapis.com/bucket/fallback.jpg")
        coEvery { mockTATDao.getTotalImageCount() } returns 40
        coEvery { mockTATDao.getLeastUsedImageByPosition(any(), any()) } answers {
            if (firstArg<Int>() == 1) gsImage else fallback
        }

        val result = tatCacheManager.getImagesForTest()

        assertTrue("Should succeed", result.isSuccess)
        val url = result.getOrNull()!!.first().imageUrl
        assertTrue(
            "gs:// URL must be normalized to https:// but got: $url",
            url.startsWith("https://")
        )
        assertEquals(
            "https://storage.googleapis.com/my-bucket/tat/image001.jpg",
            url
        )
    }

    @Test
    fun `TAT cache manager preserves https URLs`() = runTest {
        val httpsImage = createTATEntity("tat_https", "https://storage.googleapis.com/bucket/img.jpg")
        coEvery { mockTATDao.getTotalImageCount() } returns 40
        coEvery { mockTATDao.getLeastUsedImageByPosition(any(), any()) } returns httpsImage

        val result = tatCacheManager.getImagesForTest()

        assertTrue("Should succeed", result.isSuccess)
        assertEquals(
            "https://storage.googleapis.com/bucket/img.jpg",
            result.getOrNull()!!.first().imageUrl
        )
    }

    // ==================== PPDT URL Normalization ====================

    @Test
    fun `PPDT cache manager normalizes gs URLs to https`() = runTest {
        val gsImage = createPPDTEntity("ppdt_gs", "gs://my-bucket/ppdt/image001.jpg")
        coEvery { mockPPDTDao.getTotalImageCount() } returns 15
        coEvery { mockPPDTDao.getLeastUsedImages(any()) } returns listOf(gsImage)

        val result = ppdtCacheManager.getImageForTest()

        assertTrue("Should succeed", result.isSuccess)
        val url = result.getOrNull()!!.imageUrl
        assertTrue(
            "gs:// URL must be normalized to https:// but got: $url",
            url.startsWith("https://")
        )
        assertEquals(
            "https://storage.googleapis.com/my-bucket/ppdt/image001.jpg",
            url
        )
    }

    // ==================== Output URL Validation ====================

    @Test
    fun `TAT images returned from cache have valid https URLs`() = runTest {
        coEvery { mockTATDao.getTotalImageCount() } returns 40
        coEvery { mockTATDao.getLeastUsedImageByPosition(any(), any()) } answers {
            val pos = firstArg<Int>()
            createTATEntity("tat_$pos", "https://storage.googleapis.com/bucket/tat_$pos.jpg")
        }

        val result = tatCacheManager.getImagesForTest()

        assertTrue("Should succeed", result.isSuccess)
        // 11 real + 1 blank card; blank card has empty URL — only validate real images
        result.getOrNull()!!.filter { it.id != "blank_card" }.forEach { q ->
            assertTrue(
                "TAT ${q.id} URL must be https but got: ${q.imageUrl}",
                q.imageUrl.startsWith("https://")
            )
            assertTrue(
                "TAT ${q.id} URL must not be blank",
                q.imageUrl.isNotBlank()
            )
            assertDoesNotThrow("TAT ${q.id} URL must be parseable") {
                URL(q.imageUrl)
            }
        }
    }

    @Test
    fun `PPDT images returned from cache have valid https URLs`() = runTest {
        val images = (1..5).map { i ->
            createPPDTEntity("ppdt_$i", "https://storage.googleapis.com/bucket/ppdt_$i.jpg")
        }
        coEvery { mockPPDTDao.getTotalImageCount() } returns 15
        coEvery { mockPPDTDao.getLeastUsedImages(5) } returns images

        val result = ppdtCacheManager.getImagesForTest(5)

        assertTrue("Should succeed", result.isSuccess)
        result.getOrNull()!!.forEach { q ->
            assertTrue(
                "PPDT ${q.id} URL must be https but got: ${q.imageUrl}",
                q.imageUrl.startsWith("https://")
            )
            assertTrue(
                "PPDT ${q.id} URL must not be blank",
                q.imageUrl.isNotBlank()
            )
            assertDoesNotThrow("PPDT ${q.id} URL must be parseable") {
                URL(q.imageUrl)
            }
        }
    }

    @Test
    fun `mixed gs and https URLs all normalize to https`() = runTest {
        coEvery { mockTATDao.getTotalImageCount() } returns 40
        coEvery { mockTATDao.getLeastUsedImageByPosition(any(), any()) } answers {
            val pos = firstArg<Int>()
            val url = when (pos) {
                1 -> "gs://bucket/a.jpg"
                2 -> "https://storage.googleapis.com/bucket/b.jpg"
                3 -> "gs://other-bucket/path/c.jpg"
                else -> "https://storage.googleapis.com/bucket/pos_$pos.jpg"
            }
            createTATEntity("tat_pos_$pos", url)
        }

        val result = tatCacheManager.getImagesForTest()

        assertTrue("Should succeed", result.isSuccess)
        // Blank card has empty URL — only validate real images
        val urls = result.getOrNull()!!.filter { it.id != "blank_card" }.map { it.imageUrl }
        urls.forEach { url ->
            assertTrue("All URLs must be https:// after normalization: $url", url.startsWith("https://"))
            assertFalse("No gs:// URLs should remain: $url", url.startsWith("gs://"))
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `mock TAT questions have valid card positions`() {
        val questions = MockTestDataProvider.getTATQuestions()
        questions.forEachIndexed { index, q ->
            assertEquals(
                "TAT question ${q.id} should have cardPosition ${index + 1}",
                index + 1,
                q.cardPosition
            )
        }
    }

    @Test
    fun `mock TAT questions have valid timing parameters`() {
        val questions = MockTestDataProvider.getTATQuestions()
        questions.forEach { q ->
            assertTrue("viewingTimeSeconds must be positive", q.viewingTimeSeconds > 0)
            assertTrue("writingTimeMinutes must be positive", q.writingTimeMinutes > 0)
        }
    }

    @Test
    fun `mock PPDT questions have valid timing parameters`() {
        val questions = MockTestDataProvider.getPPDTQuestions()
        questions.forEach { q ->
            assertTrue("viewingTimeSeconds must be positive", q.viewingTimeSeconds > 0)
            assertTrue("writingTimeMinutes must be positive", q.writingTimeMinutes > 0)
        }
    }

    @Test
    fun `mock PPDT questions have non-empty descriptions`() {
        val questions = MockTestDataProvider.getPPDTQuestions()
        questions.forEach { q ->
            assertTrue(
                "PPDT mock ${q.id} must have non-blank imageDescription",
                q.imageDescription.isNotBlank()
            )
        }
    }

    // ==================== Helpers ====================

    private fun createTATEntity(id: String, imageUrl: String): CachedTATImageEntity {
        return CachedTATImageEntity(
            id = id,
            imageUrl = imageUrl,
            cardPosition = 1,
            genderTag = "MIXED",
            imageContextJson = "{}",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 150,
            maxCharacters = 1500,
            category = null,
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = null,
            usageCount = 0
        )
    }

    private fun createPPDTEntity(id: String, imageUrl: String): CachedPPDTImageEntity {
        return CachedPPDTImageEntity(
            id = id,
            imageUrl = imageUrl,
            localFilePath = null,
            imageDescription = "Test image",
            imageContextJson = "{}",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 200,
            maxCharacters = 1000,
            category = null,
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = null,
            usageCount = 0,
            imageDownloaded = 0
        )
    }

    private fun assertDoesNotThrow(message: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("$message — threw ${e::class.simpleName}: ${e.message}")
        }
    }
}
