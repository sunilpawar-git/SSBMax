package com.ssbmax.shared.platform.util

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.ssbmax.shared.domain.util.CrashReporter

/** Android actual: wraps Firebase Crashlytics (`firebase-crashlytics-ktx`). */
class AndroidCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) {
        Firebase.crashlytics.recordException(throwable)
    }

    override fun setUserId(userId: String) {
        Firebase.crashlytics.setUserId(userId)
    }

    override fun log(message: String) {
        Firebase.crashlytics.log(message)
    }
}
