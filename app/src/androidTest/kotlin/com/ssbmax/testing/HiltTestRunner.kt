package com.ssbmax.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner for Hilt instrumented tests
 * Configures the test application to use Hilt dependency injection
 * 
 * Add to build.gradle.kts:
 * ```
 * android {
 *     defaultConfig {
 *         testInstrumentationRunner = "com.ssbmax.testing.HiltTestRunner"
 *     }
 * }
 * ```
 */
class HiltTestRunner : AndroidJUnitRunner() {
    
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}

