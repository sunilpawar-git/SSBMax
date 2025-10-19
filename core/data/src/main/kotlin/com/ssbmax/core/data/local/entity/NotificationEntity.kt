package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ssbmax.core.domain.model.NotificationPriority
import com.ssbmax.core.domain.model.NotificationType
import com.ssbmax.core.domain.model.SSBMaxNotification

/**
 * Room entity for local notification storage
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: String, // NotificationType as String
    val priority: String, // NotificationPriority as String
    val title: String,
    val message: String,
    val imageUrl: String?,
    val actionUrl: String?,
    val actionDataJson: String?, // JSON serialized Map<String, String>
    val isRead: Boolean,
    val createdAt: Long,
    val expiresAt: Long?
)

/**
 * Convert domain model to entity
 */
fun SSBMaxNotification.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = id,
        userId = userId,
        type = type.name,
        priority = priority.name,
        title = title,
        message = message,
        imageUrl = imageUrl,
        actionUrl = actionUrl,
        actionDataJson = actionData?.let { 
            // Simple JSON serialization
            it.entries.joinToString(",") { entry -> 
                "\"${entry.key}\":\"${entry.value}\"" 
            }.let { json -> "{$json}" }
        },
        isRead = isRead,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}

/**
 * Convert entity to domain model
 */
fun NotificationEntity.toDomain(): SSBMaxNotification {
    return SSBMaxNotification(
        id = id,
        userId = userId,
        type = NotificationType.valueOf(type),
        priority = NotificationPriority.valueOf(priority),
        title = title,
        message = message,
        imageUrl = imageUrl,
        actionUrl = actionUrl,
        actionData = actionDataJson?.let { json ->
            // Simple JSON deserialization
            json.removeSurrounding("{", "}")
                .split(",")
                .associate { pair ->
                    val (key, value) = pair.split(":")
                    key.removeSurrounding("\"") to value.removeSurrounding("\"")
                }
        },
        isRead = isRead,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}

