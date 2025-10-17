package com.ssbmax.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Initializer
 * Configures Firebase services for optimal performance
 */
@Singleton
class FirebaseInitializer @Inject constructor() {

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
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        
        firestore.firestoreSettings = settings
    }
}

