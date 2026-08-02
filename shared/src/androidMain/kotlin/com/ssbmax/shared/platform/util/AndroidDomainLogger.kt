package com.ssbmax.shared.platform.util

import android.util.Log
import com.ssbmax.shared.domain.util.DomainLogger

/** Android actual: thin wrapper over `android.util.Log`. */
class AndroidDomainLogger : DomainLogger {
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
