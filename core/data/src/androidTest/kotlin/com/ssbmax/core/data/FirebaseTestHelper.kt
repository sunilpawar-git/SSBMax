package com.ssbmax.core.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Helper object for initializing Firebase in tests
 */
object FirebaseTestHelper {
    
    private var firestoreInitialized = false
    private var firestoreInstance: FirebaseFirestore? = null
    
    /**
     * Initializes Firebase and returns a Firestore instance configured for emulator.
     * Must be called BEFORE any other Firebase/Firestore operations.
     * 
     * @param emulatorHost The emulator host (default: "10.0.2.2" for Android emulator)
     * @param emulatorPort The emulator port (default: 8080 for Firestore)
     * @return Configured FirebaseFirestore instance
     */
    fun getEmulatorFirestore(
        emulatorHost: String = "10.0.2.2",
        emulatorPort: Int = 8080
    ): FirebaseFirestore {
        if (firestoreInstance != null) {
            return firestoreInstance!!
        }
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize Firebase if not already done
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        
        // Get Firestore instance and configure emulator
        val firestore = FirebaseFirestore.getInstance()
        
        if (!firestoreInitialized) {
            firestore.useEmulator(emulatorHost, emulatorPort)
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
            firestoreInitialized = true
            firestoreInstance = firestore
        }
        
        return firestore
    }
    
    /**
     * Resets the Firestore instance (for testing purposes).
     * WARNING: Only call this if you need to reinitialize Firestore.
     */
    fun reset() {
        firestoreInstance = null
        firestoreInitialized = false
    }
}

