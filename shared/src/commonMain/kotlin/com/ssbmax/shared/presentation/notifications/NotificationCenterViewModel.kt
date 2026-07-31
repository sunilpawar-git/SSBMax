package com.ssbmax.shared.presentation.notifications

import com.ssbmax.shared.domain.model.NotificationType
import com.ssbmax.shared.domain.model.SSBMaxNotification
import com.ssbmax.shared.domain.repository.NotificationRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.ui.util.formatFullDate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * KMP port of the Android `app/.../ui/notifications/NotificationCenterViewModel.kt`.
 * Manages notification list, filtering, and actions.
 *
 * Uses a real `androidx.lifecycle.ViewModel` with `viewModelScope` (Phase 1 of
 * the KMP-convergence plan, see
 * [com.ssbmax.shared.presentation.oir.OIRTestViewModel]'s doc comment for the
 * precedent this mirrors).
 * `System.currentTimeMillis()` (JVM-only) replaced with
 * `Clock.System.now().toEpochMilliseconds()`; `java.text.SimpleDateFormat`/
 * `java.util.Locale`/`java.util.Date` (JVM-only) replaced with
 * [formatFullDate] (kotlinx-datetime-backed, same seam already established by
 * this phase's `ui/util/DateFormat.kt`).
 */
class NotificationCenterViewModel(
    private val notificationRepository: NotificationRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState.asStateFlow()
    private val _selectedFilter = MutableStateFlow(NotificationFilter.ALL)
    val selectedFilter: StateFlow<NotificationFilter> = _selectedFilter.asStateFlow()

    init {
        loadNotifications()
        observeUnreadCount()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUser = observeCurrentUser().first()
            if (currentUser == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Please sign in to view notifications")
                }
                return@launch
            }

            combine(
                notificationRepository.getNotifications(currentUser.id),
                _selectedFilter
            ) { notifications: List<SSBMaxNotification>, filter: NotificationFilter ->
                notifications.filter { notification ->
                    when (filter) {
                        NotificationFilter.ALL -> true
                        NotificationFilter.UNREAD -> !notification.isRead
                        NotificationFilter.GRADING -> notification.type == NotificationType.GRADING_COMPLETE
                        NotificationFilter.FEEDBACK -> notification.type == NotificationType.FEEDBACK_AVAILABLE
                        NotificationFilter.ANNOUNCEMENTS -> notification.type == NotificationType.GENERAL_ANNOUNCEMENT
                    }
                }
            }
                .catch { e: Throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to load notifications")
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
                .collect { filteredNotifications: List<SSBMaxNotification> ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = groupNotificationsByDate(filteredNotifications),
                            error = null
                        )
                    }
                }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            val currentUser = observeCurrentUser().first() ?: return@launch

            notificationRepository.getUnreadCount(currentUser.id)
                .catch { /* Unread count is best-effort UI decoration; don't surface an error state for it. */ }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0
                )
                .collect { count: Int ->
                    _uiState.update { it.copy(unreadCount = count) }
                }
        }
    }

    private fun groupNotificationsByDate(notifications: List<SSBMaxNotification>): List<NotificationGroup> {
        val now = Clock.System.now().toEpochMilliseconds()
        val dayMillis = 24 * 60 * 60 * 1000L
        val today = now - (now % dayMillis)
        val yesterday = today - dayMillis
        val grouped = notifications.groupBy { notification ->
            when {
                notification.createdAt >= today -> "Today"
                notification.createdAt >= yesterday -> "Yesterday"
                else -> {
                    val daysAgo = ((now - notification.createdAt) / dayMillis).toInt()
                    if (daysAgo < 7) "$daysAgo days ago" else formatFullDate(notification.createdAt)
                }
            }
        }
        return grouped.map { (date: String, notificationList: List<SSBMaxNotification>) ->
            NotificationGroup(date, notificationList.sortedByDescending { it.createdAt })
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
                .onFailure { e: Throwable ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to mark notification as read") }
                }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onFailure { e: Throwable ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to delete notification") }
                }
        }
    }

    fun updateFilter(filter: NotificationFilter) {
        _selectedFilter.value = filter
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
