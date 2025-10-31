package com.ssbmax.core.data.analytics

/**
 * Convenience extensions for analytics tracking
 */

/**
 * Test types for analytics
 */
object TestTypes {
    const val OIR = "OIR"
    const val TAT = "TAT"
    const val WAT = "WAT"
    const val SRT = "SRT"
    const val PPDT = "PPDT"
    const val GTO = "GTO"
    const val INTERVIEW = "Interview"
}

/**
 * Screen names for analytics
 */
object ScreenNames {
    const val HOME = "Home"
    const val SUBSCRIPTION = "Subscription Management"
    const val SETTINGS = "Settings"
    const val STUDY_MATERIALS = "Study Materials"
    const val TEST_OIR = "OIR Test"
    const val TEST_TAT = "TAT Test"
    const val TEST_WAT = "WAT Test"
    const val TEST_SRT = "SRT Test"
    const val TEST_PPDT = "PPDT Test"
    const val PROFILE = "Profile"
    const val UPGRADE = "Upgrade Screen"
}

/**
 * Analytics sources (where user initiated action from)
 */
object AnalyticsSources {
    const val TEST_LIMIT_DIALOG = "test_limit_dialog"
    const val SETTINGS_SCREEN = "settings_screen"
    const val HOME_SCREEN = "home_screen"
    const val PREMIUM_LOCK = "premium_lock"
    const val NAVIGATION_DRAWER = "navigation_drawer"
}

/**
 * Feature names for analytics
 */
object FeatureNames {
    const val DOWNLOAD_MATERIAL = "download_study_material"
    const val BOOKMARK_MATERIAL = "bookmark_material"
    const val SHARE_RESULT = "share_test_result"
    const val DARK_MODE = "dark_mode_toggle"
    const val NOTIFICATIONS = "notifications_toggle"
    const val CACHE_CLEAR = "clear_cache"
}

/**
 * Subscription tiers for analytics
 */
object SubscriptionTiers {
    const val FREE = "FREE"
    const val PRO = "PRO"
    const val PREMIUM = "PREMIUM"
}

