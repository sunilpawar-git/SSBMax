package com.ssbmax

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * SSBMax Application class
 * Hilt entry point for dependency injection
 */
@HiltAndroidApp
class SSBMaxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
}

