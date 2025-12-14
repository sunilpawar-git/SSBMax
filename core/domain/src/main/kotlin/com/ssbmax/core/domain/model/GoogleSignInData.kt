package com.ssbmax.core.domain.model

/**
 * Platform-agnostic representation of Google Sign-In data
 * 
 * This abstraction allows the domain layer to remain platform-independent
 * while supporting Google Sign-In flows across different platforms.
 * 
 * The actual platform-specific data (Intent on Android, etc.) is stored
 * in the data layer implementation.
 */
sealed class GoogleSignInData {
    /**
     * Represents the intent/action needed to launch Google Sign-In
     * Contains platform-specific data wrapped in an opaque manner
     */
    data class LaunchData(val platformData: Any) : GoogleSignInData()
    
    /**
     * Represents the result returned from Google Sign-In flow
     * Contains platform-specific result data
     */
    data class ResultData(val platformData: Any?) : GoogleSignInData()
    
    /**
     * Represents a cancelled sign-in attempt
     */
    data object Cancelled : GoogleSignInData()
    
    /**
     * Represents an error during sign-in
     */
    data class Error(val message: String, val exception: Throwable? = null) : GoogleSignInData()
}

/**
 * Helper extension to extract platform data safely
 */
inline fun <reified T> GoogleSignInData.getPlatformData(): T? {
    return when (this) {
        is GoogleSignInData.LaunchData -> platformData as? T
        is GoogleSignInData.ResultData -> platformData as? T
        else -> null
    }
}
