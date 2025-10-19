package com.ssbmax.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SSBMaxNotification
import com.ssbmax.core.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Notification Center screen.
 * Manages notification list, filtering, and actions.
 */
@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(NotificationFilter.ALL)
    val selectedFilter: StateFlow<NotificationFilter> = _selectedFilter.asStateFlow()

    init {
        loadNotifications()
        observeUnreadCount()
    }

    /**
     * Load notifications for the current user.
     * TODO: Get userId from AuthRepository when available.
     */
    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // TODO: Replace with actual userId from AuthRepository
            val userId = "current_user_id"

            combine(
                notificationRepository.getNotifications(userId),
                _selectedFilter
            ) { notifications: List<SSBMaxNotification>, filter: NotificationFilter ->
                notifications.filter { notification ->
                    when (filter) {
                        NotificationFilter.ALL -> true
                        NotificationFilter.UNREAD -> !notification.isRead
                        NotificationFilter.GRADING -> notification.type == com.ssbmax.core.domain.model.NotificationType.GRADING_COMPLETE
                        NotificationFilter.FEEDBACK -> notification.type == com.ssbmax.core.domain.model.NotificationType.FEEDBACK_AVAILABLE
                        NotificationFilter.ANNOUNCEMENTS -> notification.type == com.ssbmax.core.domain.model.NotificationType.GENERAL_ANNOUNCEMENT
                    }
                }
            }
                .catch { e: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load notifications"
                    )
                }
                .collect { filteredNotifications: List<SSBMaxNotification> ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        notifications = groupNotificationsByDate(filteredNotifications),
                        error = null
                    )
                }
        }
    }

    /**
     * Observe unread notification count.
     */
    private fun observeUnreadCount() {
        viewModelScope.launch {
            // TODO: Replace with actual userId from AuthRepository
            val userId = "current_user_id"

            notificationRepository.getUnreadCount(userId)
                .catch { e: Throwable ->
                    // Log error but don't update UI state
                }
                .collect { count: Int ->
                    _uiState.value = _uiState.value.copy(unreadCount = count)
                }
        }
    }

    /**
     * Group notifications by date for better organization.
     */
    private fun groupNotificationsByDate(notifications: List<SSBMaxNotification>): List<NotificationGroup> {
        val now = System.currentTimeMillis()
        val today = now - (now % (24 * 60 * 60 * 1000))
        val yesterday = today - (24 * 60 * 60 * 1000)

        val grouped = notifications.groupBy { notification ->
            when {
                notification.createdAt >= today -> "Today"
                notification.createdAt >= yesterday -> "Yesterday"
                else -> {
                    val daysAgo = ((now - notification.createdAt) / (24 * 60 * 60 * 1000)).toInt()
                    if (daysAgo < 7) "$daysAgo days ago"
                    else java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(notification.createdAt))
                }
            }
        }

        return grouped.map { (date: String, notificationList: List<SSBMaxNotification>) ->
            NotificationGroup(date, notificationList.sortedByDescending { it.createdAt })
        }
    }

    /**
     * Mark a notification as read.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
                .onFailure { e: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to mark notification as read"
                    )
                }
        }
    }

    /**
     * Delete a notification.
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onFailure { e: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to delete notification"
                    )
                }
        }
    }

    /**
     * Archive a notification.
     * Note: Using deleteNotification for now as archiveNotification doesn't exist in repository
     */
    fun archiveNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onFailure { e: Throwable ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to archive notification"
                    )
                }
        }
    }

    /**
     * Update the selected filter.
     */
    fun updateFilter(filter: NotificationFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for NotificationCenter screen.
 */
data class NotificationCenterUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationGroup> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null
)

/**
 * Represents a group of notifications by date.
 */
data class NotificationGroup(
    val date: String,
    val notifications: List<SSBMaxNotification>
)

/**
 * Filter options for notifications.
 */
enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    GRADING("Grading"),
    FEEDBACK("Feedback"),
    ANNOUNCEMENTS("Announcements")
}

