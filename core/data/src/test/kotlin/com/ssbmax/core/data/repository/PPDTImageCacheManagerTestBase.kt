package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before

abstract class PPDTImageCacheManagerTestBase {

    protected lateinit var cacheManager: PPDTImageCacheManager
    protected val mockDao = mockk<PPDTImageCacheDao>(relaxed = true)
    protected val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
    protected val mockImages = createMockImages(15)

    @Before
    fun setup() {
        clearAllMocks()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        cacheManager = PPDTImageCacheManager(mockDao, mockFirestore)
    }

    protected fun localMeta(version: String, lastStalenessCheckAt: Long = 0L) =
        PPDTBatchMetadataEntity(
            batchId = "batch_001",
            downloadedAt = System.currentTimeMillis(),
            imageCount = 64,
            version = version,
            lastStalenessCheckAt = lastStalenessCheckAt
        )

    protected fun setupFirestoreVersion(version: String, withImages: Boolean) {
        val mockDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDoc.exists() } returns true
        every { mockDoc.getString("version") } returns version
        if (withImages) {
            every { mockDoc.get("images") } returns mockImages.map { it.toFirestoreMap() }
        }
        setupFirestoreDoc(mockDoc)
    }

    protected fun setupFirestoreVersionFailure() {
        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns false
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns Exception("Network error")
        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef
    }

    protected fun setupFirestoreDoc(mockDoc: DocumentSnapshot) {
        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.result } returns mockDoc
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns null
        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef
    }

    protected fun createMockImages(count: Int): List<CachedPPDTImageEntity> =
        (1..count).map { index ->
            val paddedNum = String.format(java.util.Locale.ROOT, "%03d", index)
            createMockImage(
                "ppdt_$paddedNum",
                "https://storage.googleapis.com/test/ppdt_$paddedNum.jpg"
            )
        }

    protected fun createMockImage(id: String, imageUrl: String): CachedPPDTImageEntity =
        CachedPPDTImageEntity(
            id = id,
            imageUrl = imageUrl,
            localFilePath = null,
            imageDescription = "Test image showing an ambiguous scene",
            imageContextJson = "{}",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 200,
            maxCharacters = 1000,
            category = "leadership",
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = null,
            usageCount = 0,
            imageDownloaded = 0
        )

    protected fun CachedPPDTImageEntity.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "imageUrl" to imageUrl,
        "imageDescription" to imageDescription,
        "imageContext" to emptyMap<String, Any>(),
        "genderTag" to genderTag.name,
        "viewingTimeSeconds" to viewingTimeSeconds.toLong(),
        "writingTimeMinutes" to writingTimeMinutes.toLong(),
        "minCharacters" to minCharacters.toLong(),
        "maxCharacters" to maxCharacters.toLong(),
        "category" to (category ?: ""),
        "difficulty" to (difficulty ?: "medium")
    )
}
