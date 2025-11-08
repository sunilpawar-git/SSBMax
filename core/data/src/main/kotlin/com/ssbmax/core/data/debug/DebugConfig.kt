package com.ssbmax.core.data.debug

/**
 * Debug configuration interface
 * Allows app module to provide debug-specific configurations to data layer
 */
interface DebugConfig {
    /**
     * Whether to bypass subscription limits for development testing
     * Should ONLY return true in debug builds
     */
    val bypassSubscriptionLimits: Boolean
}

/**
 * Production implementation - subscription limits always enforced
 */
class ProductionDebugConfig : DebugConfig {
    override val bypassSubscriptionLimits: Boolean = false
}

