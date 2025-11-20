package com.ssbmax.core.domain.model.interview

/**
 * Interview mode types based on subscription tier
 *
 * - TEXT_BASED: Available for Pro and Premium tiers (typing responses)
 * - VOICE_BASED: Available for Premium tier only (speech recognition)
 */
enum class InterviewMode {
    /**
     * Text-based interview where candidate types responses
     * Available for: Pro (2/month), Premium (unlimited)
     */
    TEXT_BASED,

    /**
     * Voice-based interview with speech recognition and TTS
     * Available for: Premium (2 voice + 2 text/month = 4 total)
     */
    VOICE_BASED;

    val displayName: String
        get() = when (this) {
            TEXT_BASED -> "Text Interview"
            VOICE_BASED -> "Voice Interview"
        }
}
