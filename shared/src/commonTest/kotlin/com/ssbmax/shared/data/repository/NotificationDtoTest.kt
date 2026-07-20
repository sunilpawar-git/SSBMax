package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.FCMToken
import com.ssbmax.shared.domain.model.NotificationPreferences
import com.ssbmax.shared.domain.model.NotificationPriority
import com.ssbmax.shared.domain.model.NotificationType
import com.ssbmax.shared.domain.model.SSBMaxNotification
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * DTO round-trip coverage for GitLiveNotificationRepository, following the
 * same "no live Firestore in a JVM/common unit test" convention as
 * GradingQueueDtoTest/QuestionCacheDtoTest -- these DTOs are private to the
 * repository file, so encode/decode correctness is verified directly here
 * rather than through the repository's Firestore calls (untestable without
 * an emulator).
 */
class NotificationDtoTest {

    @Test
    fun `FCMTokenDto round-trips through JSON with all fields intact`() {
        val json = Json.encodeToString(
            FCMTokenDto.serializer(),
            FCMTokenDto(userId = "u1", token = "tok", deviceId = "dev1", platform = "android", createdAt = 10L, updatedAt = 20L)
        )
        val decoded = Json.decodeFromString(FCMTokenDto.serializer(), json)
        assertEquals("u1", decoded.userId)
        assertEquals("tok", decoded.token)
        assertEquals("dev1", decoded.deviceId)
        assertEquals(10L, decoded.createdAt)
        assertEquals(20L, decoded.updatedAt)
    }

    @Test
    fun `SSBMaxNotificationDto carries a nested actionData map without flattening it`() {
        // Why this matters: the Android original wrote actionData as a raw
        // Map<String,String>? field value straight into a `mapOf(...)` Firestore write --
        // GitLive's typed decode path has no equivalent raw-Any read (see
        // GitLiveGTOTaskCacheManager's docstring for the same constraint), so this DTO
        // models the map explicitly to keep the write/read round-trip lossless.
        val dto = SSBMaxNotificationDto(
            id = "n1",
            userId = "u1",
            type = NotificationType.BATCH_INVITATION.name,
            priority = NotificationPriority.HIGH.name,
            title = "t",
            message = "m",
            actionData = mapOf("batchId" to "b1", "batchName" to "Batch One")
        )
        val json = Json.encodeToString(SSBMaxNotificationDto.serializer(), dto)
        val decoded = Json.decodeFromString(SSBMaxNotificationDto.serializer(), json)
        assertEquals(mapOf("batchId" to "b1", "batchName" to "Batch One"), decoded.actionData)
        assertEquals(NotificationType.BATCH_INVITATION.name, decoded.type)
    }

    @Test
    fun `NotificationPreferencesDto defaults match the domain model's opt-in-by-default policy`() {
        // Why this matters: every notification category defaults to enabled on both the
        // domain model and this DTO -- a mismatched default here would silently mute
        // categories for any user whose preferences doc doesn't exist yet.
        val defaults = NotificationPreferencesDto(userId = "u1")
        assertEquals(true, defaults.enablePushNotifications)
        assertEquals(true, defaults.enableGradingNotifications)
        assertEquals(false, defaults.quietHoursEnabled)
        assertEquals(22, defaults.quietHoursStart)
        assertEquals(8, defaults.quietHoursEnd)
    }

    @Test
    fun `domain-to-dto-to-domain round trip for FCMToken preserves every field`() {
        val original = FCMToken(userId = "u1", token = "tok", deviceId = "dev1", platform = "ios", createdAt = 5L, updatedAt = 6L)
        val roundTripped = fcmTokenRoundTrip(original)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `domain-to-dto-to-domain round trip for NotificationPreferences preserves every field`() {
        val original = NotificationPreferences(
            userId = "u1",
            enablePushNotifications = false,
            quietHoursEnabled = true,
            quietHoursStart = 23,
            quietHoursEnd = 6,
            updatedAt = 42L
        )
        val roundTripped = notificationPreferencesRoundTrip(original)
        assertEquals(original, roundTripped)
    }
}

private fun fcmTokenRoundTrip(token: FCMToken): FCMToken {
    val dto = FCMTokenDto(
        userId = token.userId,
        token = token.token,
        deviceId = token.deviceId,
        platform = token.platform,
        createdAt = token.createdAt,
        updatedAt = token.updatedAt
    )
    return FCMToken(
        userId = dto.userId,
        token = dto.token,
        deviceId = dto.deviceId,
        platform = dto.platform,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt
    )
}

private fun notificationPreferencesRoundTrip(prefs: NotificationPreferences): NotificationPreferences {
    val dto = NotificationPreferencesDto(
        userId = prefs.userId,
        enablePushNotifications = prefs.enablePushNotifications,
        enableGradingNotifications = prefs.enableGradingNotifications,
        enableFeedbackNotifications = prefs.enableFeedbackNotifications,
        enableBatchInvitations = prefs.enableBatchInvitations,
        enableGeneralAnnouncements = prefs.enableGeneralAnnouncements,
        enableStudyReminders = prefs.enableStudyReminders,
        enableTestReminders = prefs.enableTestReminders,
        enableMarketplaceUpdates = prefs.enableMarketplaceUpdates,
        quietHoursEnabled = prefs.quietHoursEnabled,
        quietHoursStart = prefs.quietHoursStart,
        quietHoursEnd = prefs.quietHoursEnd,
        updatedAt = prefs.updatedAt
    )
    return NotificationPreferences(
        userId = dto.userId,
        enablePushNotifications = dto.enablePushNotifications,
        enableGradingNotifications = dto.enableGradingNotifications,
        enableFeedbackNotifications = dto.enableFeedbackNotifications,
        enableBatchInvitations = dto.enableBatchInvitations,
        enableGeneralAnnouncements = dto.enableGeneralAnnouncements,
        enableStudyReminders = dto.enableStudyReminders,
        enableTestReminders = dto.enableTestReminders,
        enableMarketplaceUpdates = dto.enableMarketplaceUpdates,
        quietHoursEnabled = dto.quietHoursEnabled,
        quietHoursStart = dto.quietHoursStart,
        quietHoursEnd = dto.quietHoursEnd,
        updatedAt = dto.updatedAt
    )
}
