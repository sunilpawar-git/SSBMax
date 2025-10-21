package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.repository.BookmarkRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of BookmarkRepository
 */
@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BookmarkRepository {
    
    private companion object {
        const val BOOKMARKS_COLLECTION = "bookmarks"
        const val BOOKMARKS_FIELD = "materialIds"
    }
    
    override fun getBookmarkedMaterials(userId: String): Flow<List<String>> = callbackFlow {
        val docRef = firestore.collection(BOOKMARKS_COLLECTION)
            .document(userId)
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val bookmarks = snapshot?.get(BOOKMARKS_FIELD) as? List<*>
            val materialIds = bookmarks?.filterIsInstance<String>() ?: emptyList()
            trySend(materialIds)
        }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun toggleBookmark(
        userId: String, 
        materialId: String
    ) {
        try {
            val docRef = firestore.collection(BOOKMARKS_COLLECTION)
                .document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentBookmarks = (snapshot.get(BOOKMARKS_FIELD) as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList() 
                    ?: mutableListOf()
                
                if (currentBookmarks.contains(materialId)) {
                    currentBookmarks.remove(materialId)
                } else {
                    currentBookmarks.add(materialId)
                }
                
                transaction.set(docRef, mapOf(BOOKMARKS_FIELD to currentBookmarks))
            }.await()
        } catch (e: Exception) {
            // Log error but don't throw - bookmark toggle is non-critical
            android.util.Log.e("BookmarkRepository", "Failed to toggle bookmark", e)
        }
    }
    
    override fun isBookmarked(
        userId: String, 
        materialId: String
    ): Flow<Boolean> = callbackFlow {
        val docRef = firestore.collection(BOOKMARKS_COLLECTION)
            .document(userId)
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(false)
                return@addSnapshotListener
            }
            
            val bookmarks = snapshot?.get(BOOKMARKS_FIELD) as? List<*>
            val materialIds = bookmarks?.filterIsInstance<String>() ?: emptyList()
            trySend(materialIds.contains(materialId))
        }
        
        awaitClose { listener.remove() }
    }
}

