package com.ssbmax.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.ssbmax.shared.db.Notification as NotificationRow
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.NotificationPriority
import com.ssbmax.shared.domain.model.NotificationType
import com.ssbmax.shared.domain.model.SSBMaxNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * SQLDelight-backed port of the Android `NotificationDao` (Room). Only the
 * surface `NotificationRepositoryImpl` actually calls is ported here --
 * insert, getNotifications, getUnreadCount, markAsRead, markAllAsRead,
 * delete. `deleteAll(userId)`/`deleteExpired(currentTime)` were confirmed
 * (grep across core/data + app) to have zero callers outside
 * `NotificationDaoTest` -- same "dead scaffolding in the Android original"
 * category as prior slices' findings; see `SharedDatabase.sq`'s Notification
 * table comment.
 *
 * One real (deliberate, documented) behavior difference from the Android
 * original: `actionData`'s `Map<String, String>` was hand-serialized to JSON
 * via string concatenation/splitting (`NotificationEntity.toEntity()`/
 * `.toDomain()`) with no escaping -- a key or value containing a comma or
 * quote would corrupt the round-trip. This port uses
 * `kotlinx.serialization.json` (`MapSerializer(String, String)`) instead,
 * the same approach `GitLiveGPEImageCacheManager` uses for its list column --
 * a strictly safer encoding of the same shape, not a behavior change to what
 * ViewModels observe.
 */
private val actionDataMapSerializer = MapSerializer(String.serializer(), String.serializer())

class GitLiveNotificationCacheManager(
    private val database: SharedDatabase
) {
    private val queries get() = database.sharedDatabaseQueries

    suspend fun insert(notification: SSBMaxNotification) {
        queries.insertOrReplaceNotification(
            id = notification.id,
            userId = notification.userId,
            type = notification.type.name,
            priority = notification.priority.name,
            title = notification.title,
            message = notification.message,
            imageUrl = notification.imageUrl,
            actionUrl = notification.actionUrl,
            actionDataJson = notification.actionData?.let { Json.encodeToString(actionDataMapSerializer, it) },
            isRead = if (notification.isRead) 1L else 0L,
            createdAt = notification.createdAt,
            expiresAt = notification.expiresAt
        )
    }

    fun getNotifications(userId: String): Flow<List<SSBMaxNotification>> =
        queries.selectNotificationsByUser(userId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    fun getUnreadCount(userId: String): Flow<Int> =
        queries.selectUnreadNotificationCount(userId)
            .asFlow()
            .mapToOne(Dispatchers.Default)
            .map { it.toInt() }

    suspend fun markAsRead(notificationId: String) {
        queries.markNotificationAsRead(notificationId)
    }

    suspend fun markAllAsRead(userId: String) {
        queries.markAllNotificationsAsRead(userId)
    }

    suspend fun delete(notificationId: String) {
        queries.deleteNotificationById(notificationId)
    }
}

private fun NotificationRow.toDomain(): SSBMaxNotification = SSBMaxNotification(
    id = id,
    userId = userId,
    type = runCatching { NotificationType.valueOf(type) }.getOrDefault(NotificationType.GENERAL_ANNOUNCEMENT),
    priority = runCatching { NotificationPriority.valueOf(priority) }.getOrDefault(NotificationPriority.NORMAL),
    title = title,
    message = message,
    imageUrl = imageUrl,
    actionUrl = actionUrl,
    actionData = actionDataJson?.let {
        runCatching { Json.decodeFromString(actionDataMapSerializer, it) }.getOrNull()
    },
    isRead = isRead == 1L,
    createdAt = createdAt,
    expiresAt = expiresAt
)
