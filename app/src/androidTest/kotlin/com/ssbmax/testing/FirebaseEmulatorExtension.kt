package com.ssbmax.testing

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule for connecting to Firebase Emulator Suite
 * Automatically configures Firebase services to use local emulators
 * 
 * Usage:
 * ```
 * @get:Rule
 * val emulatorRule = FirebaseEmulatorExtension()
 * ```
 * 
 * Prerequisites:
 * - Firebase Emulator Suite must be running locally
 * - Run: `firebase emulators:start`
 */
class FirebaseEmulatorExtension(
    private val firestoreHost: String = "10.0.2.2", // Android emulator loopback
    private val firestorePort: Int = 8080,
    private val authHost: String = "10.0.2.2",
    private val authPort: Int = 9099
) : TestWatcher() {
    
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    
    override fun starting(description: Description) {
        super.starting(description)
        
        // Initialize Firebase if not already done
        if (FirebaseApp.getApps(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext).isEmpty()) {
            FirebaseApp.initializeApp(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
        }
        
        // Configure Firestore to use emulator
        firestore = FirebaseFirestore.getInstance().apply {
            useEmulator(firestoreHost, firestorePort)
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
        }
        
        // Configure Auth to use emulator
        auth = FirebaseAuth.getInstance().apply {
            useEmulator(authHost, authPort)
        }
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        
        // Clear Firestore data after test
        firestore?.clearPersistence()
        
        // Sign out any test users
        auth?.signOut()
    }
    
    /**
     * Get configured Firestore instance
     */
    fun getFirestore(): FirebaseFirestore = firestore
        ?: throw IllegalStateException("Firestore not initialized. Ensure rule is applied.")
    
    /**
     * Get configured Auth instance
     */
    fun getAuth(): FirebaseAuth = auth
        ?: throw IllegalStateException("Auth not initialized. Ensure rule is applied.")
}

