package com.ssbmax.core.domain.model.interview

/**
 * Interview mode types
 *
 * UNIFIED MODEL (Current):
 * - VOICE_BASED: Unified interview with TTS support (mutable)
 *   - FREE: 1/month with Android TTS
 *   - PRO: 1/month with Qwen TTS
 *   - PREMIUM: 3/month with Qwen TTS
 *
 * LEGACY (Deprecated):
 * - TEXT_BASED: Old text-only interview (removed from UI, kept for data migration)
 */
enum class InterviewMode {
    /**
     * Legacy text-based interview (DEPRECATED)
     *
     * This mode has been removed from the app. All existing TEXT_BASED sessions
     * will be automatically migrated to VOICE_BASED with TTS enabled.
     *
     * @deprecated Use VOICE_BASED instead. This exists only for backward compatibility
     * with existing Firestore data during migration period.
     */
    @Deprecated(
        message = "TEXT_BASED interview has been removed. Use VOICE_BASED instead.",
        replaceWith = ReplaceWith("VOICE_BASED"),
        level = DeprecationLevel.WARNING
    )
    TEXT_BASED,

    /**
     * Unified interview with optional TTS voice
     *
     * Features:
     * - Type or use keyboard voice input for responses
     * - TTS voice quality based on subscription (Android TTS or Qwen TTS)
     * - Mute toggle to disable TTS if preferred
     *
     * Available for:
     * - FREE: 1/month with Android TTS
     * - PRO: 1/month with Qwen TTS
     * - PREMIUM: 3/month with Qwen TTS
     */
    VOICE_BASED;

    val displayName: String
        @Suppress("DEPRECATION")
        get() = when (this) {
            TEXT_BASED -> "Interview (Legacy)"
            VOICE_BASED -> "Interview"
        }
}
