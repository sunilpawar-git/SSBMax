package com.ssbmax.core.domain.config

/**
 * Feature flags for content delivery
 * Allows instant rollback without app updates
 * 
 * Usage:
 * - Set useCloudContent = true to enable cloud content globally
 * - Use enableTopicCloud() to enable specific topics gradually
 * - Set fallbackToLocalOnError = true for safety (default)
 */
object ContentFeatureFlags {
    // Master switch: Enable/disable cloud content
    // TEMPORARY: Hardcoded to true for testing (normally loaded from SharedPreferences)
    var useCloudContent: Boolean = true
    
    // Always fallback to local on errors (safety net)
    var fallbackToLocalOnError: Boolean = true
    
    // Per-topic rollout flags (for gradual migration)
    // TEMPORARY: OIR & PPDT pre-enabled for testing (normally loaded from SharedPreferences)
    private val topicFlags = mutableMapOf<String, Boolean>(
        "OIR" to true,
        "PPDT" to true
    )
    
    /**
     * Check if cloud content is enabled for a specific topic
     * Case-insensitive to handle navigation inconsistencies
     */
    fun isTopicCloudEnabled(topicType: String): Boolean {
        if (!useCloudContent) return false
        // Normalize to uppercase for consistent lookup
        return topicFlags[topicType.uppercase()] ?: false
    }
    
    /**
     * Enable cloud content for a specific topic
     * Stores in uppercase for consistency
     */
    fun enableTopicCloud(topicType: String) {
        topicFlags[topicType.uppercase()] = true
    }
    
    /**
     * Disable cloud content for a specific topic
     * Uses uppercase for consistency
     */
    fun disableTopicCloud(topicType: String) {
        topicFlags[topicType.uppercase()] = false
    }
    
    /**
     * Emergency kill switch - disable all cloud content instantly
     */
    fun disableAllCloud() {
        useCloudContent = false
        topicFlags.clear()
    }
    
    /**
     * Enable all topics at once (use after successful pilot)
     */
    fun enableAllTopics() {
        val allTopics = listOf(
            "OIR", "PPDT", "PIQ_FORM", "PSYCHOLOGY", 
            "GTO", "INTERVIEW", "CONFERENCE", "MEDICALS", "SSB_OVERVIEW"
        )
        allTopics.forEach { enableTopicCloud(it) }
    }
    
    // Query optimization flags
    var enableOfflinePersistence: Boolean = true
    var cacheExpiryDays: Int = 7
    
    /**
     * Get current configuration as string (for debugging)
     */
    fun getStatus(): String {
        return """
            Cloud Content: ${if (useCloudContent) "ENABLED" else "DISABLED"}
            Fallback to Local: ${if (fallbackToLocalOnError) "ENABLED" else "DISABLED"}
            Offline Persistence: ${if (enableOfflinePersistence) "ENABLED" else "DISABLED"}
            Cache Expiry: $cacheExpiryDays days
            Enabled Topics: ${topicFlags.filter { it.value }.keys.joinToString(", ")}
        """.trimIndent()
    }
}

