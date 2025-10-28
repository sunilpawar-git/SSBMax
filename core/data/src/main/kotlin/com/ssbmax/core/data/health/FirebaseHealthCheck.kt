package com.ssbmax.core.data.health

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase health check utility
 * Tests connectivity to Firestore and Cloud Storage before using cloud content
 * 
 * Usage:
 * ```
 * val health = healthCheck.checkHealth()
 * if (health.isFullyHealthy) {
 *     // Safe to use cloud content
 * } else {
 *     // Fallback to local content
 * }
 * ```
 */
@Singleton
class FirebaseHealthCheck @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    
    data class HealthStatus(
        val isFirestoreHealthy: Boolean,
        val isStorageHealthy: Boolean,
        val firestoreError: String? = null,
        val storageError: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val isFullyHealthy: Boolean
            get() = isFirestoreHealthy && isStorageHealthy
        
        val isPartiallyHealthy: Boolean
            get() = isFirestoreHealthy || isStorageHealthy
        
        fun toDisplayString(): String {
            return """
                Firebase Health Check
                =====================
                Firestore: ${if (isFirestoreHealthy) "✓ Healthy" else "✗ Failed"}
                ${firestoreError?.let { "  Error: $it" } ?: ""}
                
                Cloud Storage: ${if (isStorageHealthy) "✓ Healthy" else "✗ Failed"}
                ${storageError?.let { "  Error: $it" } ?: ""}
                
                Status: ${when {
                    isFullyHealthy -> "✓ All systems operational"
                    isPartiallyHealthy -> "⚠ Partial connectivity"
                    else -> "✗ All systems down - using local fallback"
                }}
            """.trimIndent()
        }
    }
    
    /**
     * Check health of all Firebase services
     * Safe to call from UI thread (uses suspend)
     */
    suspend fun checkHealth(): HealthStatus {
        var firestoreHealthy = false
        var storageHealthy = false
        var firestoreError: String? = null
        var storageError: String? = null
        
        // Test 1: Firestore connectivity
        try {
            firestore.collection("health_check")
                .document("test")
                .get()
                .await()
            firestoreHealthy = true
            Log.d(TAG, "✓ Firestore is healthy")
        } catch (e: Exception) {
            firestoreError = e.message ?: "Unknown error"
            Log.e(TAG, "✗ Firestore failed: ${e.message}")
        }
        
        // Test 2: Cloud Storage connectivity
        try {
            val ref = storage.reference.child("health_check/test.txt")
            ref.metadata.await()
            storageHealthy = true
            Log.d(TAG, "✓ Cloud Storage is healthy")
        } catch (e: Exception) {
            storageError = e.message ?: "Unknown error"
            Log.e(TAG, "✗ Cloud Storage failed: ${e.message}")
        }
        
        return HealthStatus(
            isFirestoreHealthy = firestoreHealthy,
            isStorageHealthy = storageHealthy,
            firestoreError = firestoreError,
            storageError = storageError
        )
    }
    
    /**
     * Verify Firestore offline persistence is enabled
     */
    suspend fun verifyOfflinePersistence(): Result<Boolean> {
        return try {
            val settings = firestore.firestoreSettings
            val isEnabled = settings.isPersistenceEnabled
            Log.d(TAG, "Offline persistence: ${if (isEnabled) "ENABLED" else "DISABLED"}")
            Result.success(isEnabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check persistence: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Test if we can read from Firestore (checks auth + network)
     */
    suspend fun canReadFromFirestore(): Boolean {
        return try {
            firestore.collection("topic_content")
                .limit(1)
                .get()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Cannot read from Firestore: ${e.message}")
            false
        }
    }
    
    /**
     * Quick health check (Firestore only, faster)
     */
    suspend fun quickCheck(): Boolean {
        return try {
            firestore.collection("health_check")
                .document("test")
                .get()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        private const val TAG = "FirebaseHealthCheck"
    }
}

