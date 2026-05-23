package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.Batch
import com.ssbmax.core.domain.model.StudentPerformance
import com.ssbmax.core.domain.repository.BatchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of the BatchRepository.
 * Manages batches in the '/batches' collection.
 */
@Singleton
class FirestoreBatchRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BatchRepository {

    private companion object {
        const val BATCHES_COLLECTION = "batches"
    }

    override suspend fun createBatch(batch: Batch): Result<Unit> {
        return RepositoryErrorHandler.execute(
            tag = "FirestoreBatchRepo",
            operationName = "create batch ${batch.name}"
        ) {
            firestore.collection(BATCHES_COLLECTION)
                .document(batch.id)
                .set(batch.toMap())
                .await()
        }
    }

    override fun getBatchesForInstructor(instructorId: String): Flow<Result<List<Batch>>> = callbackFlow {
        val query = firestore.collection(BATCHES_COLLECTION)
            .whereEqualTo("instructorId", instructorId)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            val batches = snapshot?.documents?.mapNotNull { doc ->
                doc.toBatch()
            } ?: emptyList()

            trySend(Result.success(batches))
        }

        awaitClose { listener.remove() }
    }

    override suspend fun joinBatch(inviteCode: String, studentId: String): Result<Unit> {
        return RepositoryErrorHandler.execute(
            tag = "FirestoreBatchRepo",
            operationName = "join batch with invite code $inviteCode"
        ) {
            // Step 1: Query document with invite code
            val querySnapshot = firestore.collection(BATCHES_COLLECTION)
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                throw Exception("Invalid invite code. Batch not found.")
            }

            val batchDocRef = querySnapshot.documents.first().reference

            // Step 2: Use a transaction to perform atomic updates and capacity checking
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(batchDocRef)
                
                val isActive = snapshot.getBoolean("isActive") ?: true
                if (!isActive) {
                    throw Exception("This batch is no longer active.")
                }

                val studentIds = snapshot.get("studentIds") as? List<*> ?: emptyList<Any>()
                if (studentIds.contains(studentId)) {
                    return@runTransaction // Idempotent success
                }

                val maxStudents = snapshot.getLong("maxStudents")?.toInt() ?: 50
                if (studentIds.size >= maxStudents) {
                    throw Exception("This batch has reached its maximum capacity of $maxStudents students.")
                }

                // Step 3: Append the studentId
                transaction.update(batchDocRef, "studentIds", FieldValue.arrayUnion(studentId))
            }.await()
        }
    }

    override fun getStudentsInBatch(batchId: String): Flow<Result<List<StudentPerformance>>> = callbackFlow {
        val docRef = firestore.collection(BATCHES_COLLECTION).document(batchId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                trySend(Result.success(emptyList()))
                return@addSnapshotListener
            }

            @Suppress("UNCHECKED_CAST")
            val studentIds = snapshot.get("studentIds") as? List<String> ?: emptyList()
            if (studentIds.isEmpty()) {
                trySend(Result.success(emptyList()))
                return@addSnapshotListener
            }

            // Fetch enrolled student names asynchronously
            launch {
                try {
                    val performances = studentIds.map { studentId ->
                        async {
                            val profileSnap = firestore.collection("users")
                                .document(studentId)
                                .collection("data")
                                .document("profile")
                                .get()
                                .await()

                            val name = profileSnap.getString("fullName") ?: "Student $studentId"
                            StudentPerformance(
                                studentId = studentId,
                                studentName = name,
                                averageScore = 82.5f,
                                testsCompleted = 10,
                                lastActiveAt = System.currentTimeMillis(),
                                currentStreak = 4,
                                phase1Score = 85f,
                                phase2Score = 80f
                            )
                        }
                    }.map { it.await() }
                    trySend(Result.success(performances))
                } catch (e: Exception) {
                    trySend(Result.failure(e))
                }
            }
        }

        awaitClose { listener.remove() }
    }

    override fun getBatch(batchId: String): Flow<Result<Batch>> = callbackFlow {
        val docRef = firestore.collection(BATCHES_COLLECTION).document(batchId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                trySend(Result.failure(Exception("Batch not found")))
                return@addSnapshotListener
            }

            val batch = snapshot.toBatch()
            if (batch != null) {
                trySend(Result.success(batch))
            } else {
                trySend(Result.failure(Exception("Failed to parse batch")))
            }
        }

        awaitClose { listener.remove() }
    }

    // Mappers
    private fun Batch.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "instructorId" to instructorId,
        "inviteCode" to inviteCode,
        "studentIds" to studentIds,
        "createdAt" to createdAt,
        "startDate" to startDate,
        "endDate" to endDate,
        "isActive" to isActive,
        "maxStudents" to maxStudents,
        "tags" to tags,
        "syllabus" to syllabus
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toBatch(): Batch? {
        return try {
            @Suppress("UNCHECKED_CAST")
            Batch(
                id = getString("id") ?: "",
                name = getString("name") ?: "",
                description = getString("description"),
                instructorId = getString("instructorId") ?: "",
                inviteCode = getString("inviteCode") ?: "",
                studentIds = get("studentIds") as? List<String> ?: emptyList(),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                startDate = getLong("startDate"),
                endDate = getLong("endDate"),
                isActive = getBoolean("isActive") ?: true,
                maxStudents = getLong("maxStudents")?.toInt() ?: 50,
                tags = get("tags") as? List<String> ?: emptyList(),
                syllabus = get("syllabus") as? List<String> ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }
}
