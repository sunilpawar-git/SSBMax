package com.ssbmax.core.data.util

import android.util.Log
import com.ssbmax.core.domain.util.DomainLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of DomainLogger using android.util.Log
 * 
 * This implementation bridges the domain layer's platform-independent logging
 * to Android's logging system.
 * 
 * Injected into domain layer use cases via Hilt dependency injection.
 */
@Singleton
class AndroidDomainLogger @Inject constructor() : DomainLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun v(tag: String, message: String) {
        Log.v(tag, message)
    }
}

