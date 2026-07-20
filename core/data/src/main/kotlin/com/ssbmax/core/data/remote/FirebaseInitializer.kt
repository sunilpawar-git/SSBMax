package com.ssbmax.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Firebase Initializer
 * Configures Firebase services for optimal performance
 */
class FirebaseInitializer {

    init {
        initializeFirestore()
    }

    /**
     * Initialize Firestore with offline persistence
     */
    private fun initializeFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        
        // Enable offline persistence
        // Data will be cached locally and synced when online
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
        
        firestore.firestoreSettings = settings
    }
}

