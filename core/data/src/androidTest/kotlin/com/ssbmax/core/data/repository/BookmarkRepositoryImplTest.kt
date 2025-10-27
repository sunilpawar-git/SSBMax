package com.ssbmax.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirebaseInitializer
import com.ssbmax.core.data.remote.FirestoreSubmissionRepository
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.BookmarkRepository
import com.ssbmax.testing.BaseRepositoryTest
import com.ssbmax.testing.FirebaseTestHelper
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for BookmarkRepositoryImpl
 *
 * Tests the bookmarking system for study materials, allowing users to save
 * and organize their favorite content for quick access.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BookmarkRepositoryImplTest : BaseRepositoryTest() {

    @Inject
    lateinit var bookmarkRepository: BookmarkRepository

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var firebaseTestHelper: FirebaseTestHelper

    private lateinit var testUser: SSBMaxUser

    @Before
    override fun setup() {
        super.setup()

        // Initialize Firebase for testing
        FirebaseInitializer.initialize()
        firebaseTestHelper.setupEmulator()

        // Create test user
        testUser = SSBMaxUser(
            id = "student_001",
            email = "student@test.com",
            displayName = "Test Student",
            photoUrl = null,
            role = UserRole.STUDENT,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        firebaseTestHelper.cleanup()
    }

    @Test
    fun `addBookmark successfully bookmarks study material`() = runTest(timeout = 30.seconds) {
        // Given: Study material ID to bookmark
        val materialId = "study_material_001"
        val bookmarkNote = "Important notes on leadership qualities"

        // When: Add bookmark
        val addResult = bookmarkRepository.addBookmark(
            userId = testUser.id,
            materialId = materialId,
            note = bookmarkNote
        )

        // Then: Bookmark should be added successfully
        Assert.assertTrue("Add bookmark should succeed", addResult.isSuccess)

        // Verify bookmark exists
        val userBookmarks = bookmarkRepository.getUserBookmarks(testUser.id).first()
        Assert.assertTrue("Should get user bookmarks", userBookmarks.isSuccess)

        val bookmarks = userBookmarks.getOrNull() ?: emptyList()
        val addedBookmark = bookmarks.find { it.materialId == materialId }
        Assert.assertNotNull("Bookmark should exist", addedBookmark)
        Assert.assertEquals("Should have correct material ID", materialId, addedBookmark?.materialId)
        Assert.assertEquals("Should have correct note", bookmarkNote, addedBookmark?.note)
        Assert.assertEquals("Should be associated with correct user", testUser.id, addedBookmark?.userId)
    }

    @Test
    fun `removeBookmark successfully removes bookmark`() = runTest(timeout = 30.seconds) {
        // Given: Add a bookmark first
        val materialId = "study_material_002"
        bookmarkRepository.addBookmark(testUser.id, materialId, "Test note")

        // Verify bookmark exists
        val bookmarksBefore = bookmarkRepository.getUserBookmarks(testUser.id).first()
        Assert.assertTrue("Should have bookmark before removal",
            bookmarksBefore.getOrNull()?.any { it.materialId == materialId } == true)

        // When: Remove bookmark
        val removeResult = bookmarkRepository.removeBookmark(testUser.id, materialId)

        // Then: Removal should succeed
        Assert.assertTrue("Remove bookmark should succeed", removeResult.isSuccess)

        // Bookmark should no longer exist
        val bookmarksAfter = bookmarkRepository.getUserBookmarks(testUser.id).first()
        Assert.assertFalse("Bookmark should be removed",
            bookmarksAfter.getOrNull()?.any { it.materialId == materialId } == true)
    }

    @Test
    fun `getUserBookmarks returns all user bookmarks sorted by creation time`() = runTest(timeout = 30.seconds) {
        // Given: Add multiple bookmarks at different times
        val bookmark1 = createTestBookmark("material_001", "First bookmark", System.currentTimeMillis() - 2000)
        val bookmark2 = createTestBookmark("material_002", "Second bookmark", System.currentTimeMillis() - 1000)
        val bookmark3 = createTestBookmark("material_003", "Third bookmark", System.currentTimeMillis())

        // Add bookmarks (simulate different creation times)
        listOf(bookmark1, bookmark2, bookmark3).forEach { bookmark ->
            firebaseTestHelper.createTestBookmark(bookmark)
        }

        // When: Get user bookmarks
        val userBookmarks = bookmarkRepository.getUserBookmarks(testUser.id).first()

        // Then: Should return all bookmarks sorted by creation time (newest first)
        Assert.assertTrue("Should get user bookmarks", userBookmarks.isSuccess)
        val bookmarks = userBookmarks.getOrNull() ?: emptyList()

        Assert.assertEquals("Should return 3 bookmarks", 3, bookmarks.size)

        // Verify sorting (newest first)
        Assert.assertEquals("Newest bookmark should be first", bookmark3.materialId, bookmarks[0].materialId)
        Assert.assertEquals("Middle bookmark should be second", bookmark2.materialId, bookmarks[1].materialId)
        Assert.assertEquals("Oldest bookmark should be last", bookmark1.materialId, bookmarks[2].materialId)

        // Verify timestamps are in descending order
        for (i in 0 until bookmarks.size - 1) {
            Assert.assertTrue(
                "Bookmarks should be sorted newest first",
                bookmarks[i].createdAt >= bookmarks[i + 1].createdAt
            )
        }
    }

    @Test
    fun `isBookmarked returns correct bookmark status`() = runTest(timeout = 30.seconds) {
        // Given: Add bookmark for one material but not another
        val bookmarkedMaterialId = "bookmarked_material"
        val notBookmarkedMaterialId = "not_bookmarked_material"

        bookmarkRepository.addBookmark(testUser.id, bookmarkedMaterialId, "Bookmarked")

        // When: Check bookmark status
        val isBookmarkedResult = bookmarkRepository.isBookmarked(testUser.id, bookmarkedMaterialId)
        val isNotBookmarkedResult = bookmarkRepository.isBookmarked(testUser.id, notBookmarkedMaterialId)

        // Then: Should return correct status
        Assert.assertTrue("Bookmarked material should return true", isBookmarkedResult.getOrNull() == true)
        Assert.assertTrue("Non-bookmarked material should return false", isNotBookmarkedResult.getOrNull() == false)
    }

    @Test
    fun `updateBookmarkNote successfully updates bookmark note`() = runTest(timeout = 30.seconds) {
        // Given: Add bookmark with initial note
        val materialId = "material_update_test"
        val initialNote = "Initial note"
        val updatedNote = "Updated note with more details"

        bookmarkRepository.addBookmark(testUser.id, materialId, initialNote)

        // Verify initial note
        val bookmarksBefore = bookmarkRepository.getUserBookmarks(testUser.id).first()
        val bookmarkBefore = bookmarksBefore.getOrNull()?.find { it.materialId == materialId }
        Assert.assertEquals("Should have initial note", initialNote, bookmarkBefore?.note)

        // When: Update bookmark note
        val updateResult = bookmarkRepository.updateBookmarkNote(testUser.id, materialId, updatedNote)

        // Then: Update should succeed
        Assert.assertTrue("Update note should succeed", updateResult.isSuccess)

        // Note should be updated
        val bookmarksAfter = bookmarkRepository.getUserBookmarks(testUser.id).first()
        val bookmarkAfter = bookmarksAfter.getOrNull()?.find { it.materialId == materialId }
        Assert.assertEquals("Should have updated note", updatedNote, bookmarkAfter?.note)
    }

    @Test
    fun `getBookmarkCount returns correct count of user bookmarks`() = runTest(timeout = 30.seconds) {
        // Given: Add different numbers of bookmarks for different users
        val otherUserId = "other_student"

        // Add 3 bookmarks for test user
        repeat(3) { index ->
            bookmarkRepository.addBookmark(testUser.id, "material_$index", "Note $index")
        }

        // Add 2 bookmarks for other user
        repeat(2) { index ->
            bookmarkRepository.addBookmark(otherUserId, "other_material_$index", "Other note $index")
        }

        // When: Get bookmark count for test user
        val countResult = bookmarkRepository.getBookmarkCount(testUser.id)

        // Then: Should return correct count (3)
        Assert.assertTrue("Should get bookmark count", countResult.isSuccess)
        Assert.assertEquals("Should have 3 bookmarks", 3, countResult.getOrNull())
    }

    @Test
    fun `observeBookmarkUpdates provides real-time bookmark changes`() = runTest(timeout = 30.seconds) {
        // Given: Start observing bookmarks
        bookmarkRepository.observeBookmarkUpdates(testUser.id).test {
            val initialBookmarks = awaitItem()
            Assert.assertTrue("Initial bookmarks should be empty or existing",
                initialBookmarks.size >= 0)

            // When: Add new bookmark
            val materialId = "realtime_bookmark"
            bookmarkRepository.addBookmark(testUser.id, materialId, "Real-time test")

            // Then: Should receive real-time update
            val updatedBookmarks = awaitItem()
            val newBookmark = updatedBookmarks.find { it.materialId == materialId }
            Assert.assertNotNull("New bookmark should appear in real-time updates", newBookmark)

            // When: Update bookmark note
            val updatedNote = "Updated real-time note"
            bookmarkRepository.updateBookmarkNote(testUser.id, materialId, updatedNote)

            // Then: Should receive another update
            val noteUpdatedBookmarks = awaitItem()
            val updatedBookmark = noteUpdatedBookmarks.find { it.materialId == materialId }
            Assert.assertNotNull("Updated bookmark should exist", updatedBookmark)
            Assert.assertEquals("Should have updated note", updatedNote, updatedBookmark?.note)

            // When: Remove bookmark
            bookmarkRepository.removeBookmark(testUser.id, materialId)

            // Then: Should receive removal update
            val removedBookmarks = awaitItem()
            val removedBookmark = removedBookmarks.find { it.materialId == materialId }
            Assert.assertNull("Removed bookmark should not exist in updates", removedBookmark)
        }
    }

    @Test
    fun `addBookmark fails for already bookmarked material`() = runTest(timeout = 30.seconds) {
        // Given: Add bookmark
        val materialId = "duplicate_bookmark"
        bookmarkRepository.addBookmark(testUser.id, materialId, "First note")

        // When: Try to add bookmark for same material again
        val duplicateResult = bookmarkRepository.addBookmark(testUser.id, materialId, "Second note")

        // Then: Should fail due to duplicate
        Assert.assertTrue("Duplicate bookmark should fail", duplicateResult.isFailure)
        Assert.assertTrue("Should indicate already bookmarked",
            duplicateResult.exceptionOrNull()?.message?.contains("already bookmarked") == true)
    }

    @Test
    fun `bookmarks are isolated between users`() = runTest(timeout = 30.seconds) {
        // Given: Two different users bookmark the same material
        val otherUserId = "other_student"
        val sharedMaterialId = "shared_material"

        bookmarkRepository.addBookmark(testUser.id, sharedMaterialId, "My bookmark")
        bookmarkRepository.addBookmark(otherUserId, sharedMaterialId, "Other user's bookmark")

        // When: Get bookmarks for each user
        val userBookmarks = bookmarkRepository.getUserBookmarks(testUser.id).first()
        val otherUserBookmarks = bookmarkRepository.getUserBookmarks(otherUserId).first()

        // Then: Each user should only see their own bookmarks
        Assert.assertTrue("Test user should get their bookmarks", userBookmarks.isSuccess)
        Assert.assertTrue("Other user should get their bookmarks", otherUserBookmarks.isSuccess)

        val userBookmark = userBookmarks.getOrNull()?.find { it.materialId == sharedMaterialId }
        val otherUserBookmark = otherUserBookmarks.getOrNull()?.find { it.materialId == sharedMaterialId }

        Assert.assertNotNull("Test user should have their bookmark", userBookmark)
        Assert.assertNotNull("Other user should have their bookmark", otherUserBookmark)

        // Bookmarks should have different notes
        Assert.assertEquals("Test user should have their note", "My bookmark", userBookmark?.note)
        Assert.assertEquals("Other user should have their note", "Other user's bookmark", otherUserBookmark?.note)

        // Verify user isolation
        Assert.assertEquals("Test user should have 1 bookmark", 1, userBookmarks.getOrNull()?.size)
        Assert.assertEquals("Other user should have 1 bookmark", 1, otherUserBookmarks.getOrNull()?.size)
    }

    @Test
    fun `getBookmarksByCategory returns bookmarks filtered by category`() = runTest(timeout = 30.seconds) {
        // Given: Add bookmarks for different categories
        val psychologyBookmark = createTestBookmark("psych_material", "Psychology note").copy(
            category = SSBCategory.PSYCHOLOGY
        )
        val gtoBookmark = createTestBookmark("gto_material", "GTO note").copy(
            category = SSBCategory.GTO
        )
        val interviewBookmark = createTestBookmark("interview_material", "Interview note").copy(
            category = SSBCategory.INTERVIEW
        )

        listOf(psychologyBookmark, gtoBookmark, interviewBookmark).forEach {
            firebaseTestHelper.createTestBookmark(it)
        }

        // When: Get bookmarks by psychology category
        val psychologyBookmarks = bookmarkRepository.getBookmarksByCategory(testUser.id, SSBCategory.PSYCHOLOGY).first()

        // Then: Should return only psychology bookmarks
        Assert.assertTrue("Should get psychology bookmarks", psychologyBookmarks.isSuccess)
        val psychBookmarks = psychologyBookmarks.getOrNull() ?: emptyList()

        Assert.assertEquals("Should return 1 psychology bookmark", 1, psychBookmarks.size)
        Assert.assertEquals("Should be psychology material", psychologyBookmark.materialId, psychBookmarks[0].materialId)
        Assert.assertEquals("Should have psychology category", SSBCategory.PSYCHOLOGY, psychBookmarks[0].category)
    }

    // ==================== HELPER METHODS ====================

    private fun createTestBookmark(
        materialId: String,
        note: String,
        createdAt: Long = System.currentTimeMillis(),
        category: SSBCategory = SSBCategory.PSYCHOLOGY
    ): Bookmark {
        return Bookmark(
            id = "bookmark_${materialId}",
            userId = testUser.id,
            materialId = materialId,
            category = category,
            note = note,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
}
