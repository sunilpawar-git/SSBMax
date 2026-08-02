package com.ssbmax.shared.platform.util

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.ssbmax.shared.domain.util.AnalyticsTracker

/** Android actual: wraps Firebase Analytics (`firebase-analytics-ktx`). */
class AndroidAnalyticsTracker : AnalyticsTracker {
    override fun trackEvent(name: String, params: Map<String, Any?>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    null -> Unit
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        Firebase.analytics.logEvent(name, bundle)
    }
}
