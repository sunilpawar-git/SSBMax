package com.ssbmax.ui.notifications

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for NotificationCenterViewModel
 * Tests notification loading, filtering, grouping, and actions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCenterViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: NotificationCenterViewModel
    private lateinit var mockNotificationRepository: NotificationRepository
    private lateinit var mockObserveCurrentUser: ObserveCurrentUserUseCase
    private lateinit var mockCurrentUserFlow: MutableStateFlow<SSBMaxUser?>
    
    private val testUser = SSBMaxUser(
        id = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    
    private val now = System.currentTimeMillis()
    private val yesterday = now - (24 * 60 * 60 * 1000)
    private val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)
    
    private val notification1 = SSBMaxNotification(
        id = "notif-1",
        userId = "user-123",
        type = NotificationType.GRADING_COMPLETE,
        title = "Test Graded",
        message = "Your OIR test has been graded",
        isRead = false,
        createdAt = now
    )
    
    private val notification2 = SSBMaxNotification(
        id = "notif-2",
        userId = "user-123",
        type = NotificationType.FEEDBACK_AVAILABLE,
        title = "New Feedback",
        message = "Instructor provided feedback",
        isRead = true,
        createdAt = yesterday
    )
    
    private val notification3 = SSBMaxNotification(
        id = "notif-3",
        userId = "user-123",
        type = NotificationType.GENERAL_ANNOUNCEMENT,
        title = "App Update",
        message = "New features available",
        isRead = false,
        createdAt = twoDaysAgo
    )
    
    private val allNotifications = listOf(notification1, notification2, notification3)
    
    @Before
    fun setup() {
        mockNotificationRepository = mockk(relaxed = true)
        mockObserveCurrentUser = mockk(relaxed = true)
        mockCurrentUserFlow = MutableStateFlow(null)
        
        every { mockObserveCurrentUser() } returns mockCurrentUserFlow
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Load Notifications Tests ====================
    
    @Test
    fun `init loads notifications successfully`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        
        // When
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertFalse("Should have notifications", state.notifications.isEmpty())
        assertEquals("Should have unread count", 2, state.unreadCount)
    }
    
    @Test
    fun `notifications are grouped by date`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        
        // When
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val groups = state.notifications
        assertTrue("Should have at least one group", groups.isNotEmpty())
        
        // Verify groups have correct date labels
        val dateLabels = groups.map { it.date }
        assertTrue("Should contain 'Today'", dateLabels.any { it == "Today" })
    }
    
    // ==================== Filter Tests ====================
    
    @Test
    fun `updateFilter to UNREAD shows only unread notifications`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.updateFilter(NotificationFilter.UNREAD)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val filterState = viewModel.selectedFilter.value
        assertEquals("Filter should be UNREAD", NotificationFilter.UNREAD, filterState)
        
        // Verify only unread notifications are shown
        val allNotifs = state.notifications.flatMap { it.notifications }
        assertTrue("Should have notifications", allNotifs.isNotEmpty())
        assertTrue("All should be unread", allNotifs.all { !it.isRead })
    }
    
    @Test
    fun `updateFilter to GRADING shows only grading notifications`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.updateFilter(NotificationFilter.GRADING)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val allNotifs = state.notifications.flatMap { it.notifications }
        assertTrue("Should have notifications", allNotifs.isNotEmpty())
        assertTrue("All should be grading type", 
            allNotifs.all { it.type == NotificationType.GRADING_COMPLETE }
        )
    }
    
    @Test
    fun `updateFilter to FEEDBACK shows only feedback notifications`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.updateFilter(NotificationFilter.FEEDBACK)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val allNotifs = state.notifications.flatMap { it.notifications }
        assertTrue("Should have notifications", allNotifs.isNotEmpty())
        assertTrue("All should be feedback type", 
            allNotifs.all { it.type == NotificationType.FEEDBACK_AVAILABLE }
        )
    }
    
    @Test
    fun `updateFilter to ANNOUNCEMENTS shows only announcement notifications`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.updateFilter(NotificationFilter.ANNOUNCEMENTS)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        val allNotifs = state.notifications.flatMap { it.notifications }
        assertTrue("Should have notifications", allNotifs.isNotEmpty())
        assertTrue("All should be announcement type", 
            allNotifs.all { it.type == NotificationType.GENERAL_ANNOUNCEMENT }
        )
    }
    
    @Test
    fun `updateFilter to ALL shows all notifications`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // First set a filter
        viewModel.updateFilter(NotificationFilter.UNREAD)
        advanceUntilIdle()
        
        // When - reset to ALL
        viewModel.updateFilter(NotificationFilter.ALL)
        advanceUntilIdle()
        
        // Then
        val filterState = viewModel.selectedFilter.value
        assertEquals("Filter should be ALL", NotificationFilter.ALL, filterState)
    }
    
    // ==================== Action Tests ====================
    
    @Test
    fun `markAsRead calls repository`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        coEvery { mockNotificationRepository.markAsRead(any()) } coAnswers { Result.success(Unit) }
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.markAsRead("notif-1")
        advanceUntilIdle()
        
        // Then
        io.mockk.coVerify { mockNotificationRepository.markAsRead("notif-1") }
    }
    
    @Test
    fun `markAsRead handles error gracefully`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        coEvery { mockNotificationRepository.markAsRead(any()) } coAnswers {
            Result.failure(Exception("Network error"))
        }
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.markAsRead("notif-1")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Should have error", state.error)
        assertTrue("Error should mention failure", 
            state.error?.contains("Network error") == true || 
            state.error?.contains("Failed") == true
        )
    }
    
    @Test
    fun `deleteNotification calls repository`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        coEvery { mockNotificationRepository.deleteNotification(any()) } coAnswers { Result.success(Unit) }
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.deleteNotification("notif-1")
        advanceUntilIdle()
        
        // Then
        io.mockk.coVerify { mockNotificationRepository.deleteNotification("notif-1") }
    }
    
    @Test
    fun `archiveNotification delegates to deleteNotification`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        coEvery { mockNotificationRepository.deleteNotification(any()) } coAnswers { Result.success(Unit) }
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // When
        viewModel.archiveNotification("notif-1")
        advanceUntilIdle()
        
        // Then
        io.mockk.coVerify { mockNotificationRepository.deleteNotification("notif-1") }
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `no user shows error`() = runTest {
        // Given - no user
        mockCurrentUserFlow.value = null
        
        // When
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue("Error should mention sign in", 
            state.error?.contains("sign in") == true
        )
    }
    
    @Test
    fun `clearError removes error from state`() = runTest {
        // Given
        mockCurrentUserFlow.value = testUser
        coEvery { mockNotificationRepository.getNotifications(testUser.id) } returns 
            MutableStateFlow(allNotifications)
        coEvery { mockNotificationRepository.getUnreadCount(testUser.id) } returns 
            MutableStateFlow(2)
        coEvery { mockNotificationRepository.markAsRead(any()) } coAnswers {
            Result.failure(Exception("Error"))
        }
        
        viewModel = NotificationCenterViewModel(mockNotificationRepository, mockObserveCurrentUser)
        advanceUntilIdle()
        
        // Trigger error
        viewModel.markAsRead("notif-1")
        advanceUntilIdle()
        
        assertNotNull("Should have error", viewModel.uiState.value.error)
        
        // When
        viewModel.clearError()
        
        // Then
        val state = viewModel.uiState.value
        assertNull("Should have cleared error", state.error)
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `ui state has correct default values`() {
        val defaultState = NotificationCenterUiState()
        
        assertFalse(defaultState.isLoading)
        assertTrue(defaultState.notifications.isEmpty())
        assertEquals(0, defaultState.unreadCount)
        assertNull(defaultState.error)
    }
    
    @Test
    fun `filter enum has correct values`() {
        assertEquals("All", NotificationFilter.ALL.displayName)
        assertEquals("Unread", NotificationFilter.UNREAD.displayName)
        assertEquals("Grading", NotificationFilter.GRADING.displayName)
        assertEquals("Feedback", NotificationFilter.FEEDBACK.displayName)
        assertEquals("Announcements", NotificationFilter.ANNOUNCEMENTS.displayName)
    }
}

