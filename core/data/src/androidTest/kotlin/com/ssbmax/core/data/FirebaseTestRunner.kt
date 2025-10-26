package com.ssbmax.core.data

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.FirebaseApp

/**
 * Custom test runner that initializes Firebase before running tests
 */
class FirebaseTestRunner : AndroidJUnitRunner() {
    
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TestApplication::class.java.name, context)
    }
}

/**
 * Test application that initializes Firebase
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase for tests
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
    }
}

