package com.ssbmax.shared.domain.model

/**
 * Ported verbatim from core/domain/model/GoogleSignInData.kt (Phase 0 KMP spike).
 * Already platform-agnostic in the original — no changes needed for KMP.
 */
sealed class GoogleSignInData {
    data class LaunchData(val platformData: Any) : GoogleSignInData()
    data class ResultData(val platformData: Any?) : GoogleSignInData()
    data object Cancelled : GoogleSignInData()
    data class Error(val message: String, val exception: Throwable? = null) : GoogleSignInData()
}
