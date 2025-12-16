package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore Submission Repository
 * Manages test submissions (TAT, WAT, SRT, etc.) in Firestore
 */
@Singleton
class FirestoreSubmissionRepository @Inject constructor() : SubmissionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")

    companion object {
        private const val TAG = "FirestoreSubmission"
        
        // Field names
        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_ID = "testId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_GRADED_BY_INSTRUCTOR_ID = "gradedByInstructorId"
        private const val FIELD_GRADING_TIMESTAMP = "gradingTimestamp"
        private const val FIELD_BATCH_ID = "batchId"
        private const val FIELD_DATA = "data" // Contains the actual submission data
    }

    /**
     * Submit TAT test
     */
    override suspend fun submitTAT(submission: TATSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.TAT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit TAT: ${e.message}", e))
        }
    }

    /**
     * Submit WAT test
     */
    override suspend fun submitWAT(submission: WATSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.WAT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit WAT: ${e.message}", e))
        }
    }

    /**
     * Submit SRT test
     */
    override suspend fun submitSRT(submission: SRTSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.SRT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit SRT: ${e.message}", e))
        }
    }

    /**
     * Submit SDT test
     */
    override suspend fun submitSDT(submission: SDTSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore SDT: Preparing submission for Firestore...")
            Log.d(TAG, "   Document ID: ${submission.id}")
            Log.d(TAG, "   User ID: ${submission.userId}")
            Log.d(TAG, "   Test ID: ${submission.testId}")
            Log.d(TAG, "   Responses: ${submission.responses.size}")
            Log.d(TAG, "   Status: ${submission.status.name}")
            
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.SD.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
            )

            Log.d(TAG, "☁️ Firestore SDT: Writing to collection 'submissions' at path: submissions/${submission.id}")
            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Firestore SDT: Successfully written to Firestore!")
            Log.d(TAG, "   Collection: submissions")
            Log.d(TAG, "   Document: ${submission.id}")
            Result.success(submission.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore SDT: Failed to write to Firestore - ${e.message}", e)
            Result.failure(Exception("Failed to submit SDT: ${e.message}", e))
        }
    }

    /**
     * Submit PPDT test
     */
    override suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.submissionId,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.questionId,
                FIELD_TEST_TYPE to TestType.PPDT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
            )

            submissionsCollection.document(submission.submissionId)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.submissionId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit PPDT: ${e.message}", e))
        }
    }

    /**
     * Submit OIR test
     */
    override suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.OIR.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit OIR: ${e.message}", e))
        }
    }

    /**
     * Submit PIQ test
     */
    override suspend fun submitPIQ(submission: PIQSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore PIQ: Preparing submission for Firestore...")
            Log.d(TAG, "   Document ID: ${submission.id}")
            Log.d(TAG, "   User ID: ${submission.userId}")
            Log.d(TAG, "   Test ID: ${submission.testId}")
            Log.d(TAG, "   Status: ${submission.status.name}")
            
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.PIQ.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toMap()
            )

            Log.d(TAG, "☁️ Firestore PIQ: Writing to collection 'submissions' at path: submissions/${submission.id}")
            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Firestore PIQ: Successfully written to Firestore!")
            Log.d(TAG, "   Collection: submissions")
            Log.d(TAG, "   Document: ${submission.id}")
            Result.success(submission.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore PIQ: Failed to write to Firestore - ${e.message}", e)
            Result.failure(Exception("Failed to submit PIQ: ${e.message}", e))
        }
    }

    /**
     * Submit GPE test
     */
    override suspend fun submitGPE(
        submission: com.ssbmax.core.domain.model.gto.GTOSubmission.GPESubmission, 
        batchId: String?
    ): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.GTO_GPE.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to mapOf(
                    "imageUrl" to submission.imageUrl,
                    "scenario" to submission.scenario,
                    "solution" to submission.solution,
                    "plan" to submission.plan,
                    "characterCount" to submission.characterCount,
                    "olqScores" to submission.olqScores.mapKeys { it.key.name }.mapValues { entry ->
                        mapOf(
                            "score" to entry.value.score,
                            "confidence" to entry.value.confidence,
                            "reasoning" to entry.value.reasoning
                        )
                    }
                )
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit GPE: ${e.message}", e))
        }
    }
    
    /**
     * Submit GD test
     */
    override suspend fun submitGD(
        submission: com.ssbmax.core.domain.model.gto.GTOSubmission.GDSubmission, 
        batchId: String?
    ): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.GTO_GD.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to mapOf(
                    "topic" to submission.topic,
                    "response" to submission.response,
                    "wordCount" to submission.wordCount,
                    "olqScores" to submission.olqScores.mapKeys { it.key.name }.mapValues { entry ->
                        mapOf(
                            "score" to entry.value.score,
                            "confidence" to entry.value.confidence,
                            "reasoning" to entry.value.reasoning
                        )
                    }
                )
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit GD: ${e.message}", e))
        }
    }

    /**
     * Submit Lecturette test
     */
    override suspend fun submitLecturette(
        submission: com.ssbmax.core.domain.model.gto.GTOSubmission.LecturetteSubmission, 
        batchId: String?
    ): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.GTO_LECTURETTE.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to null,
                FIELD_GRADING_TIMESTAMP to null,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to mapOf(
                    "topicChoices" to submission.topicChoices,
                    "selectedTopic" to submission.selectedTopic,
                    "speechTranscript" to submission.speechTranscript,
                    "wordCount" to submission.wordCount,
                     "olqScores" to submission.olqScores.mapKeys { it.key.name }.mapValues { entry ->
                        mapOf(
                            "score" to entry.value.score,
                            "confidence" to entry.value.confidence,
                            "reasoning" to entry.value.reasoning
                        )
                    }
                )
            )

            submissionsCollection.document(submission.id)
                .set(submissionMap, SetOptions.merge())
                .await()

            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit Lecturette: ${e.message}", e))
        }
    }

    /**
     * Get submission by ID
     */
    override suspend fun getSubmission(submissionId: String): Result<Map<String, Any>?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()

            if (!document.exists()) {
                return Result.success(null)
            }

            Result.success(document.data)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get submission: ${e.message}", e))
        }
    }

    /**
     * Get user's submissions
     */
    override suspend fun getUserSubmissions(
        userId: String,
        limit: Int
    ): Result<List<Map<String, Any>>> {
        return try {
            val querySnapshot = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val submissions = querySnapshot.documents.mapNotNull { it.data }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get user submissions: ${e.message}", e))
        }
    }

    /**
     * Get user's submissions by test type
     */
    override suspend fun getUserSubmissionsByTestType(
        userId: String,
        testType: TestType,
        limit: Int
    ): Result<List<Map<String, Any>>> {
        return try {
            val querySnapshot = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, testType.name)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val submissions = querySnapshot.documents.mapNotNull { it.data }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get submissions by type: ${e.message}", e))
        }
    }

    /**
     * Observe submission changes in real-time
     */
    override fun observeSubmission(submissionId: String): Flow<Map<String, Any>?> = callbackFlow {
        val registration = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.data)
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Observe user's submissions in real-time
     */
    override fun observeUserSubmissions(userId: String, limit: Int): Flow<List<Map<String, Any>>> = callbackFlow {
        val registration = submissionsCollection
            .whereEqualTo(FIELD_USER_ID, userId)
            .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val submissions = querySnapshot.documents.mapNotNull { it.data }
                    trySend(submissions)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Update submission status
     */
    override suspend fun updateSubmissionStatus(
        submissionId: String,
        status: SubmissionStatus
    ): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId)
                .update(FIELD_STATUS, status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update status: ${e.message}", e))
        }
    }

    /**
     * Update submission with instructor grading
     */
    suspend fun updateWithInstructorGrading(
        submissionId: String,
        instructorId: String,
        status: SubmissionStatus = SubmissionStatus.GRADED
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                FIELD_STATUS to status.name,
                FIELD_GRADED_BY_INSTRUCTOR_ID to instructorId,
                FIELD_GRADING_TIMESTAMP to System.currentTimeMillis()
            )

            submissionsCollection.document(submissionId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update grading: ${e.message}", e))
        }
    }

    /**
     * Get pending submissions for instructor
     */
    suspend fun getPendingSubmissionsForInstructor(
        batchId: String? = null,
        limit: Int = 100
    ): Result<List<Map<String, Any>>> {
        return try {
            var query: Query = submissionsCollection
                .whereEqualTo(FIELD_STATUS, SubmissionStatus.SUBMITTED_PENDING_REVIEW.name)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.ASCENDING)

            if (batchId != null) {
                query = query.whereEqualTo(FIELD_BATCH_ID, batchId)
            }

            val querySnapshot = query.limit(limit.toLong()).get().await()
            val submissions = querySnapshot.documents.mapNotNull { it.data }
            Result.success(submissions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get pending submissions: ${e.message}", e))
        }
    }

    /**
     * Delete submission
     */
    suspend fun deleteSubmission(submissionId: String): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete submission: ${e.message}", e))
        }
    }

    /**
     * Get latest PIQ submission for user
     */
    override suspend fun getLatestPIQSubmission(userId: String): Result<PIQSubmission?> {
        return try {
            val query = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, "PIQ")
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val data = doc.get("data") as? Map<*, *>

                if (data == null) {
                    Log.w(TAG, "PIQ submission found but data field is null")
                    Result.success(null)
                } else {
                    val submission = parsePIQSubmission(data)
                    Result.success(submission)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest PIQ submission", e)
            Result.failure(e)
        }
    }

    /**
     * Get latest OIR submission for user
     */
    override suspend fun getLatestOIRSubmission(userId: String): Result<OIRSubmission?> {
        return try {
            val query = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, "OIR")
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val data = doc.get("data") as? Map<*, *>

                if (data == null) {
                    Log.w(TAG, "OIR submission found but data field is null")
                    Result.success(null)
                } else {
                    val submission = parseOIRSubmission(data)
                    Result.success(submission)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest OIR submission", e)
            Result.failure(e)
        }
    }

    /**
     * Get latest PPDT submission for user
     */
    override suspend fun getLatestPPDTSubmission(userId: String): Result<PPDTSubmission?> {
        return try {
            val query = submissionsCollection
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_TEST_TYPE, "PPDT")
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
                .limit(1)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val data = doc.get("data") as? Map<*, *>

                if (data == null) {
                    Log.w(TAG, "PPDT submission found but data field is null")
                    Result.success(null)
                } else {
                    val submission = parsePPDTSubmission(data)
                    Result.success(submission)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest PPDT submission", e)
            Result.failure(e)
        }
    }

    /**
     * Parse PIQ submission from Firestore map
     */
    @Suppress("UNCHECKED_CAST")
    private fun parsePIQSubmission(data: Map<*, *>): PIQSubmission {
        // Helper to safely get list of maps
        fun getListOfMaps(key: String): List<Map<*, *>> {
            return (data[key] as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
        }

        // Parse siblings
        val siblings = getListOfMaps("siblings").map {
            Sibling(
                id = it["id"] as? String ?: "",
                name = it["name"] as? String ?: "",
                age = it["age"] as? String ?: "",
                occupation = it["occupation"] as? String ?: "",
                education = it["education"] as? String ?: "",
                income = it["income"] as? String ?: ""
            )
        }

        // Parse education levels
        fun parseEducation(key: String, level: String): Education {
            val eduMap = data[key] as? Map<*, *> ?: emptyMap<String, Any>()
            return Education(
                level = level,
                institution = eduMap["institution"] as? String ?: "",
                board = eduMap["board"] as? String ?: "",
                stream = eduMap["stream"] as? String ?: "",
                year = eduMap["year"] as? String ?: "",
                percentage = eduMap["percentage"] as? String ?: "",
                cgpa = eduMap["cgpa"] as? String ?: "",
                mediumOfInstruction = eduMap["mediumOfInstruction"] as? String ?: "",
                boarderDayScholar = eduMap["boarderDayScholar"] as? String ?: "",
                outstandingAchievement = eduMap["outstandingAchievement"] as? String ?: ""
            )
        }

        // Parse sports participation
        val sportsParticipation = getListOfMaps("sportsParticipation").map {
            SportsParticipation(
                id = it["id"] as? String ?: "",
                sport = it["sport"] as? String ?: "",
                period = it["period"] as? String ?: "",
                representedInstitution = it["representedInstitution"] as? String ?: "",
                outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
            )
        }

        // Parse extra-curricular activities
        val extraCurricularActivities = getListOfMaps("extraCurricularActivities").map {
            ExtraCurricularActivity(
                id = it["id"] as? String ?: "",
                activityName = it["activityName"] as? String ?: "",
                duration = it["duration"] as? String ?: "",
                outstandingAchievement = it["outstandingAchievement"] as? String ?: ""
            )
        }

        // Parse work experience
        val workExperience = getListOfMaps("workExperience").map {
            WorkExperience(
                id = it["id"] as? String ?: "",
                company = it["company"] as? String ?: "",
                role = it["role"] as? String ?: "",
                duration = it["duration"] as? String ?: "",
                description = it["description"] as? String ?: ""
            )
        }

        // Parse NCC training
        val nccTrainingMap = data["nccTraining"] as? Map<*, *> ?: emptyMap<String, Any>()
        val nccTraining = NCCTraining(
            hasTraining = nccTrainingMap["hasTraining"] as? Boolean ?: false,
            totalTraining = nccTrainingMap["totalTraining"] as? String ?: "",
            wing = nccTrainingMap["wing"] as? String ?: "",
            division = nccTrainingMap["division"] as? String ?: "",
            certificateObtained = nccTrainingMap["certificateObtained"] as? String ?: ""
        )

        // Parse previous interviews
        val previousInterviews = getListOfMaps("previousInterviews").map {
            PreviousInterview(
                id = it["id"] as? String ?: "",
                typeOfEntry = it["typeOfEntry"] as? String ?: "",
                ssbNumber = it["ssbNumber"] as? String ?: "",
                ssbPlace = it["ssbPlace"] as? String ?: "",
                date = it["date"] as? String ?: "",
                chestNumber = it["chestNumber"] as? String ?: "",
                batchNumber = it["batchNumber"] as? String ?: ""
            )
        }

        // Parse AI score
        val aiScoreMap = data["aiPreliminaryScore"] as? Map<*, *>
        val aiPreliminaryScore = aiScoreMap?.let {
            PIQAIScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                personalInfoScore = (it["personalInfoScore"] as? Number)?.toFloat() ?: 0f,
                familyInfoScore = (it["familyInfoScore"] as? Number)?.toFloat() ?: 0f,
                motivationScore = (it["motivationScore"] as? Number)?.toFloat() ?: 0f,
                selfAssessmentScore = (it["selfAssessmentScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                completenessPercentage = (it["completenessPercentage"] as? Number)?.toInt() ?: 0,
                clarityScore = (it["clarityScore"] as? Number)?.toFloat() ?: 0f,
                consistencyScore = (it["consistencyScore"] as? Number)?.toFloat() ?: 0f,
                analysisTimestamp = (it["analysisTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        return PIQSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "piq_standard",
            oirNumber = data["oirNumber"] as? String ?: "",
            selectionBoard = data["selectionBoard"] as? String ?: "",
            batchNumber = data["batchNumber"] as? String ?: "",
            chestNumber = data["chestNumber"] as? String ?: "",
            upscRollNumber = data["upscRollNumber"] as? String ?: "",
            fullName = data["fullName"] as? String ?: "",
            dateOfBirth = data["dateOfBirth"] as? String ?: "",
            age = data["age"] as? String ?: "",
            gender = data["gender"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            email = data["email"] as? String ?: "",
            state = data["state"] as? String ?: "",
            district = data["district"] as? String ?: "",
            religion = data["religion"] as? String ?: "",
            scStObcStatus = data["scStObcStatus"] as? String ?: "",
            motherTongue = data["motherTongue"] as? String ?: "",
            maritalStatus = data["maritalStatus"] as? String ?: "",
            permanentAddress = data["permanentAddress"] as? String ?: "",
            presentAddress = data["presentAddress"] as? String ?: "",
            maximumResidence = data["maximumResidence"] as? String ?: "",
            maximumResidencePopulation = data["maximumResidencePopulation"] as? String ?: "",
            presentResidencePopulation = data["presentResidencePopulation"] as? String ?: "",
            permanentResidencePopulation = data["permanentResidencePopulation"] as? String ?: "",
            isDistrictHQ = data["isDistrictHQ"] as? Boolean ?: false,
            height = data["height"] as? String ?: "",
            weight = data["weight"] as? String ?: "",
            fatherName = data["fatherName"] as? String ?: "",
            fatherOccupation = data["fatherOccupation"] as? String ?: "",
            fatherEducation = data["fatherEducation"] as? String ?: "",
            fatherIncome = data["fatherIncome"] as? String ?: "",
            motherName = data["motherName"] as? String ?: "",
            motherOccupation = data["motherOccupation"] as? String ?: "",
            motherEducation = data["motherEducation"] as? String ?: "",
            parentsAlive = data["parentsAlive"] as? String ?: "",
            ageAtFatherDeath = data["ageAtFatherDeath"] as? String ?: "",
            ageAtMotherDeath = data["ageAtMotherDeath"] as? String ?: "",
            guardianName = data["guardianName"] as? String ?: "",
            guardianOccupation = data["guardianOccupation"] as? String ?: "",
            guardianEducation = data["guardianEducation"] as? String ?: "",
            guardianIncome = data["guardianIncome"] as? String ?: "",
            siblings = siblings,
            presentOccupation = data["presentOccupation"] as? String ?: "",
            personalMonthlyIncome = data["personalMonthlyIncome"] as? String ?: "",
            education10th = parseEducation("education10th", "10th"),
            education12th = parseEducation("education12th", "12th"),
            educationGraduation = parseEducation("educationGraduation", "Graduation"),
            educationPostGraduation = parseEducation("educationPostGraduation", "Post-Graduation"),
            hobbies = data["hobbies"] as? String ?: "",
            sports = data["sports"] as? String ?: "",
            sportsParticipation = sportsParticipation,
            extraCurricularActivities = extraCurricularActivities,
            positionsOfResponsibility = data["positionsOfResponsibility"] as? String ?: "",
            workExperience = workExperience,
            nccTraining = nccTraining,
            natureOfCommission = data["natureOfCommission"] as? String ?: "",
            choiceOfService = data["choiceOfService"] as? String ?: "",
            chancesAvailed = data["chancesAvailed"] as? String ?: "",
            previousInterviews = previousInterviews,
            whyDefenseForces = data["whyDefenseForces"] as? String ?: "",
            strengths = data["strengths"] as? String ?: "",
            weaknesses = data["weaknesses"] as? String ?: "",
            status = SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED"),
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            lastModifiedAt = (data["lastModifiedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            aiPreliminaryScore = aiPreliminaryScore
        )
    }

    /**
     * Parse OIR submission from Firestore map
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseOIRSubmission(data: Map<*, *>): OIRSubmission {
        val testResultMap = data["testResult"] as? Map<*, *> ?: emptyMap<String, Any>()

        // Parse category scores
        val categoryScoresMap = testResultMap["categoryScores"] as? Map<*, *> ?: emptyMap<String, Any>()
        val categoryScores = categoryScoresMap.mapNotNull { (key, value) ->
            val categoryName = key as? String ?: return@mapNotNull null
            val scoreMap = value as? Map<*, *> ?: return@mapNotNull null

            val category = try {
                OIRQuestionType.valueOf(categoryName)
            } catch (e: Exception) {
                return@mapNotNull null
            }

            category to CategoryScore(
                category = category,
                totalQuestions = (scoreMap["totalQuestions"] as? Number)?.toInt() ?: 0,
                correctAnswers = (scoreMap["correctAnswers"] as? Number)?.toInt() ?: 0,
                percentage = (scoreMap["percentage"] as? Number)?.toFloat() ?: 0f,
                averageTimeSeconds = (scoreMap["averageTimeSeconds"] as? Number)?.toInt() ?: 0
            )
        }.toMap()

        // Parse difficulty breakdown
        val difficultyBreakdownMap = testResultMap["difficultyBreakdown"] as? Map<*, *> ?: emptyMap<String, Any>()
        val difficultyBreakdown = difficultyBreakdownMap.mapNotNull { (key, value) ->
            val difficultyName = key as? String ?: return@mapNotNull null
            val scoreMap = value as? Map<*, *> ?: return@mapNotNull null

            val difficulty = try {
                QuestionDifficulty.valueOf(difficultyName)
            } catch (e: Exception) {
                return@mapNotNull null
            }

            difficulty to DifficultyScore(
                difficulty = difficulty,
                totalQuestions = (scoreMap["totalQuestions"] as? Number)?.toInt() ?: 0,
                correctAnswers = (scoreMap["correctAnswers"] as? Number)?.toInt() ?: 0,
                percentage = (scoreMap["percentage"] as? Number)?.toFloat() ?: 0f
            )
        }.toMap()

        // Parse answered questions (simplified - full parsing would be complex)
        val answeredQuestions = emptyList<OIRAnsweredQuestion>()

        val testResult = OIRTestResult(
            testId = testResultMap["testId"] as? String ?: "",
            sessionId = testResultMap["sessionId"] as? String ?: "",
            userId = testResultMap["userId"] as? String ?: "",
            totalQuestions = (testResultMap["totalQuestions"] as? Number)?.toInt() ?: 0,
            correctAnswers = (testResultMap["correctAnswers"] as? Number)?.toInt() ?: 0,
            incorrectAnswers = (testResultMap["incorrectAnswers"] as? Number)?.toInt() ?: 0,
            skippedQuestions = (testResultMap["skippedQuestions"] as? Number)?.toInt() ?: 0,
            totalTimeSeconds = (testResultMap["totalTimeSeconds"] as? Number)?.toInt() ?: 0,
            timeTakenSeconds = (testResultMap["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
            rawScore = (testResultMap["rawScore"] as? Number)?.toInt() ?: 0,
            percentageScore = (testResultMap["percentageScore"] as? Number)?.toFloat() ?: 0f,
            categoryScores = categoryScores,
            difficultyBreakdown = difficultyBreakdown,
            answeredQuestions = answeredQuestions,
            completedAt = (testResultMap["completedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )

        return OIRSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            testResult = testResult,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED"),
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong()
        )
    }

    /**
     * Parse PPDT submission from Firestore map
     */
    @Suppress("UNCHECKED_CAST")
    private fun parsePPDTSubmission(data: Map<*, *>): PPDTSubmission {
        // Parse AI score
        val aiScoreMap = data["aiPreliminaryScore"] as? Map<*, *>
        val aiPreliminaryScore = aiScoreMap?.let {
            PPDTAIScore(
                perceptionScore = (it["perceptionScore"] as? Number)?.toFloat() ?: 0f,
                imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                narrationScore = (it["narrationScore"] as? Number)?.toFloat() ?: 0f,
                characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList()
            )
        }

        // Parse instructor review (if available)
        val instructorReviewMap = data["instructorReview"] as? Map<*, *>
        val instructorReview = instructorReviewMap?.let {
            val detailedScoresMap = it["detailedScores"] as? Map<*, *> ?: emptyMap<String, Any>()
            val detailedScores = PPDTDetailedScores(
                perception = (detailedScoresMap["perception"] as? Number)?.toFloat() ?: 0f,
                imagination = (detailedScoresMap["imagination"] as? Number)?.toFloat() ?: 0f,
                narration = (detailedScoresMap["narration"] as? Number)?.toFloat() ?: 0f,
                characterDepiction = (detailedScoresMap["characterDepiction"] as? Number)?.toFloat() ?: 0f,
                positivity = (detailedScoresMap["positivity"] as? Number)?.toFloat() ?: 0f
            )

            PPDTInstructorReview(
                reviewId = it["reviewId"] as? String ?: "",
                instructorId = it["instructorId"] as? String ?: "",
                instructorName = it["instructorName"] as? String ?: "",
                finalScore = (it["finalScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                detailedScores = detailedScores,
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false,
                reviewedAt = (it["reviewedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                timeSpentMinutes = (it["timeSpentMinutes"] as? Number)?.toInt() ?: 0
            )
        }

        return PPDTSubmission(
            submissionId = data["submissionId"] as? String ?: "",
            questionId = data["questionId"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            userName = data["userName"] as? String ?: "",
            userEmail = data["userEmail"] as? String ?: "",
            batchId = data["batchId"] as? String,
            story = data["story"] as? String ?: "",
            charactersCount = (data["charactersCount"] as? Number)?.toInt() ?: 0,
            viewingTimeTakenSeconds = (data["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
            writingTimeTakenMinutes = (data["writingTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED"),
            aiPreliminaryScore = aiPreliminaryScore,
            instructorReview = instructorReview
        )
    }

    // ===========================
    // TAT OLQ Analysis Methods
    // ===========================

    override suspend fun getTATSubmission(submissionId: String): Result<TATSubmission?> {
        return try {
            Log.d(TAG, "📖 Fetching TAT submission: $submissionId")
            val document = submissionsCollection.document(submissionId).get().await()

            if (!document.exists()) {
                Log.w(TAG, "⚠️ TAT submission not found: $submissionId")
                return Result.success(null)
            }

            val data = document.get(FIELD_DATA) as? Map<*, *>
            if (data == null) {
                Log.e(TAG, "❌ TAT submission data is null")
                return Result.success(null)
            }

            val submission = parseTATSubmission(data)
            Log.d(TAG, "✅ TAT submission fetched successfully")
            Result.success(submission)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch TAT submission", e)
            Result.failure(Exception("Failed to fetch TAT submission: ${e.message}", e))
        }
    }

    override suspend fun updateTATAnalysisStatus(
        submissionId: String,
        status: com.ssbmax.core.domain.model.scoring.AnalysisStatus
    ): Result<Unit> {
        return try {
            Log.d(TAG, "📝 Updating TAT analysis status: $submissionId -> $status")
            submissionsCollection.document(submissionId)
                .update("data.analysisStatus", status.name)
                .await()
            Log.d(TAG, "✅ TAT analysis status updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update TAT analysis status", e)
            Result.failure(Exception("Failed to update TAT analysis status: ${e.message}", e))
        }
    }

    override suspend fun updateTATOLQResult(
        submissionId: String,
        olqResult: com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
    ): Result<Unit> {
        return try {
            Log.d(TAG, "📝 Updating TAT OLQ result: $submissionId")
            val olqResultMap = mapOf(
                "submissionId" to olqResult.submissionId,
                "testType" to olqResult.testType.name,
                "olqScores" to olqResult.olqScores.mapKeys { it.key.name }.mapValues { (_, score) ->
                    mapOf(
                        "score" to score.score,
                        "confidence" to score.confidence,
                        "reasoning" to score.reasoning
                    )
                },
                "overallScore" to olqResult.overallScore,
                "overallRating" to olqResult.overallRating,
                "strengths" to olqResult.strengths,
                "weaknesses" to olqResult.weaknesses,
                "recommendations" to olqResult.recommendations,
                "analyzedAt" to olqResult.analyzedAt,
                "aiConfidence" to olqResult.aiConfidence
            )

            submissionsCollection.document(submissionId)
                .update(
                    "data.olqResult", olqResultMap,
                    "data.analysisStatus", com.ssbmax.core.domain.model.scoring.AnalysisStatus.COMPLETED.name
                )
                .await()
            Log.d(TAG, "✅ TAT OLQ result updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update TAT OLQ result", e)
            Result.failure(Exception("Failed to update TAT OLQ result: ${e.message}", e))
        }
    }

    override fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> = callbackFlow {
        Log.d(TAG, "👀 Observing TAT submission: $submissionId")
        val listener = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error observing TAT submission", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                    val submission = if (data != null) parseTATSubmission(data) else null
                    trySend(submission)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to parse TAT submission", e)
                }
            }
        awaitClose { listener.remove() }
    }

    // WAT, SRT, SDT methods follow same pattern
    override suspend fun getWATSubmission(submissionId: String): Result<WATSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(parseWATSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch WAT submission: ${e.message}", e))
        }
    }

    override suspend fun updateWATAnalysisStatus(submissionId: String, status: com.ssbmax.core.domain.model.scoring.AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).update("data.analysisStatus", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update WAT status: ${e.message}", e))
        }
    }

    override suspend fun updateWATOLQResult(submissionId: String, olqResult: com.ssbmax.core.domain.model.scoring.OLQAnalysisResult): Result<Unit> {
        return updateOLQResult(submissionId, olqResult)
    }

    override fun observeWATSubmission(submissionId: String): Flow<WATSubmission?> = callbackFlow {
        val listener = submissionsCollection.document(submissionId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) { trySend(null); return@addSnapshotListener }
            try {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                trySend(if (data != null) parseWATSubmission(data) else null)
            } catch (e: Exception) { }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getSRTSubmission(submissionId: String): Result<SRTSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(parseSRTSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch SRT submission: ${e.message}", e))
        }
    }

    override suspend fun updateSRTAnalysisStatus(submissionId: String, status: com.ssbmax.core.domain.model.scoring.AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).update("data.analysisStatus", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update SRT status: ${e.message}", e))
        }
    }

    override suspend fun updateSRTOLQResult(submissionId: String, olqResult: com.ssbmax.core.domain.model.scoring.OLQAnalysisResult): Result<Unit> {
        return updateOLQResult(submissionId, olqResult)
    }

    override fun observeSRTSubmission(submissionId: String): Flow<SRTSubmission?> = callbackFlow {
        val listener = submissionsCollection.document(submissionId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) { trySend(null); return@addSnapshotListener }
            try {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                trySend(if (data != null) parseSRTSubmission(data) else null)
            } catch (e: Exception) { }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getSDTSubmission(submissionId: String): Result<SDTSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(parseSDTSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch SDT submission: ${e.message}", e))
        }
    }

    override suspend fun updateSDTAnalysisStatus(submissionId: String, status: com.ssbmax.core.domain.model.scoring.AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).update("data.analysisStatus", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update SDT status: ${e.message}", e))
        }
    }

    override suspend fun updateSDTOLQResult(submissionId: String, olqResult: com.ssbmax.core.domain.model.scoring.OLQAnalysisResult): Result<Unit> {
        return updateOLQResult(submissionId, olqResult)
    }

    override fun observeSDTSubmission(submissionId: String): Flow<SDTSubmission?> = callbackFlow {
        val listener = submissionsCollection.document(submissionId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) { trySend(null); return@addSnapshotListener }
            try {
                val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                trySend(if (data != null) parseSDTSubmission(data) else null)
            } catch (e: Exception) { }
        }
        awaitClose { listener.remove() }
    }

    private suspend fun updateOLQResult(submissionId: String, olqResult: com.ssbmax.core.domain.model.scoring.OLQAnalysisResult): Result<Unit> {
        return try {
            val olqResultMap = mapOf(
                "submissionId" to olqResult.submissionId,
                "testType" to olqResult.testType.name,
                "olqScores" to olqResult.olqScores.mapKeys { it.key.name }.mapValues { (_, score) ->
                    mapOf("score" to score.score, "confidence" to score.confidence, "reasoning" to score.reasoning)
                },
                "overallScore" to olqResult.overallScore,
                "overallRating" to olqResult.overallRating,
                "strengths" to olqResult.strengths,
                "weaknesses" to olqResult.weaknesses,
                "recommendations" to olqResult.recommendations,
                "analyzedAt" to olqResult.analyzedAt,
                "aiConfidence" to olqResult.aiConfidence
            )
            submissionsCollection.document(submissionId)
                .update("data.olqResult", olqResultMap, "data.analysisStatus", com.ssbmax.core.domain.model.scoring.AnalysisStatus.COMPLETED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update OLQ result: ${e.message}", e))
        }
    }

    // ===========================
    // Parsing Helper Methods
    // ===========================

    /**
     * Parse TAT submission from Firestore data map
     */
    private fun parseTATSubmission(data: Map<*, *>): TATSubmission {
        // Parse stories
        val storiesList = data["stories"] as? List<*> ?: emptyList<Any>()
        val stories = storiesList.mapNotNull { storyData ->
            (storyData as? Map<*, *>)?.let {
                TATStoryResponse(
                    questionId = it["questionId"] as? String ?: "",
                    story = it["story"] as? String ?: "",
                    charactersCount = (it["charactersCount"] as? Number)?.toInt() ?: 0,
                    viewingTimeTakenSeconds = (it["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    writingTimeTakenSeconds = (it["writingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
        }

        // Parse AI score if present
        val aiScoreMap = data["aiPreliminaryScore"] as? Map<*, *>
        val aiPreliminaryScore = aiScoreMap?.let {
            // Parse story-wise analysis
            val storyAnalysisList = it["storyWiseAnalysis"] as? List<*> ?: emptyList<Any>()
            val storyWiseAnalysis = storyAnalysisList.mapNotNull { analysisData ->
                (analysisData as? Map<*, *>)?.let { analysis ->
                    StoryAnalysis(
                        questionId = analysis["questionId"] as? String ?: "",
                        sequenceNumber = (analysis["sequenceNumber"] as? Number)?.toInt() ?: 0,
                        score = (analysis["score"] as? Number)?.toFloat() ?: 0f,
                        themes = (analysis["themes"] as? List<*>)?.mapNotNull { t -> t as? String } ?: emptyList(),
                        sentimentScore = (analysis["sentimentScore"] as? Number)?.toFloat() ?: 0f,
                        keyInsights = (analysis["keyInsights"] as? List<*>)?.mapNotNull { k -> k as? String } ?: emptyList()
                    )
                }
            }

            TATAIScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                thematicPerceptionScore = (it["thematicPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                emotionalToneScore = (it["emotionalToneScore"] as? Number)?.toFloat() ?: 0f,
                narrativeStructureScore = (it["narrativeStructureScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String,
                storyWiseAnalysis = storyWiseAnalysis,
                strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { a -> a as? String } ?: emptyList()
            )
        }

        // Parse instructor score if present
        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            TATInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                thematicPerceptionScore = (it["thematicPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                emotionalToneScore = (it["emotionalToneScore"] as? Number)?.toFloat() ?: 0f,
                narrativeStructureScore = (it["narrativeStructureScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                storyWiseComments = (it["storyWiseComments"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    (k as? String)?.let { key -> key to (v as? String ?: "") }
                }?.toMap() ?: emptyMap(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        // Parse OLQ result if present
        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)

        // Parse analysis status
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.valueOf(analysisStatusStr)
            } else {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
            }
        } catch (e: Exception) {
            com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
        }

        return TATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            stories = stories,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try {
                SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW")
            } catch (e: Exception) {
                SubmissionStatus.SUBMITTED_PENDING_REVIEW
            },
            aiPreliminaryScore = aiPreliminaryScore,
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    /**
     * Parse WAT submission from Firestore data map
     */
    private fun parseWATSubmission(data: Map<*, *>): WATSubmission {
        // Parse responses
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                WATWordResponse(
                    wordId = it["wordId"] as? String ?: "",
                    word = it["word"] as? String ?: "",
                    response = it["response"] as? String ?: "",
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        // Parse AI score if present
        val aiScoreMap = data["aiPreliminaryScore"] as? Map<*, *>
        val aiPreliminaryScore = aiScoreMap?.let {
            WATAIScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                creativityScore = (it["creativityScore"] as? Number)?.toFloat() ?: 0f,
                speedScore = (it["speedScore"] as? Number)?.toFloat() ?: 0f,
                relevanceScore = (it["relevanceScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String,
                positiveWords = (it["positiveWords"] as? Number)?.toInt() ?: 0,
                negativeWords = (it["negativeWords"] as? Number)?.toInt() ?: 0,
                neutralWords = (it["neutralWords"] as? Number)?.toInt() ?: 0,
                uniqueResponsesCount = (it["uniqueResponsesCount"] as? Number)?.toInt() ?: 0,
                repeatedPatterns = (it["repeatedPatterns"] as? List<*>)?.mapNotNull { p -> p as? String } ?: emptyList(),
                strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { a -> a as? String } ?: emptyList()
            )
        }

        // Parse instructor score if present
        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            WATInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                creativityScore = (it["creativityScore"] as? Number)?.toFloat() ?: 0f,
                speedScore = (it["speedScore"] as? Number)?.toFloat() ?: 0f,
                relevanceScore = (it["relevanceScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                notableResponses = (it["notableResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        // Parse OLQ result if present
        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)

        // Parse analysis status
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.valueOf(analysisStatusStr)
            } else {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
            }
        } catch (e: Exception) {
            com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
        }

        return WATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try {
                SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW")
            } catch (e: Exception) {
                SubmissionStatus.SUBMITTED_PENDING_REVIEW
            },
            aiPreliminaryScore = aiPreliminaryScore,
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    /**
     * Parse SRT submission from Firestore data map
     */
    private fun parseSRTSubmission(data: Map<*, *>): SRTSubmission {
        // Parse responses
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                SRTSituationResponse(
                    situationId = it["situationId"] as? String ?: "",
                    situation = it["situation"] as? String ?: "",
                    response = it["response"] as? String ?: "",
                    charactersCount = (it["charactersCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        // Parse AI score if present
        val aiScoreMap = data["aiPreliminaryScore"] as? Map<*, *>
        val aiPreliminaryScore = aiScoreMap?.let {
            // Parse category-wise scores
            val categoryWiseScoresMap = it["categoryWiseScores"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val categoryWiseScores = categoryWiseScoresMap.mapNotNull { (k, v) ->
                try {
                    val category = SRTCategory.valueOf(k as? String ?: "GENERAL")
                    val score = (v as? Number)?.toFloat() ?: 0f
                    category to score
                } catch (e: Exception) {
                    null
                }
            }.toMap()

            // Parse response quality
            val responseQualityStr = it["responseQuality"] as? String
            val responseQuality = try {
                if (responseQualityStr != null) {
                    ResponseQuality.valueOf(responseQualityStr)
                } else {
                    ResponseQuality.AVERAGE
                }
            } catch (e: Exception) {
                ResponseQuality.AVERAGE
            }

            SRTAIScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
                decisionMakingScore = (it["decisionMakingScore"] as? Number)?.toFloat() ?: 0f,
                practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
                initiativeScore = (it["initiativeScore"] as? Number)?.toFloat() ?: 0f,
                socialResponsibilityScore = (it["socialResponsibilityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String,
                categoryWiseScores = categoryWiseScores,
                positiveTraits = (it["positiveTraits"] as? List<*>)?.mapNotNull { t -> t as? String } ?: emptyList(),
                concerningPatterns = (it["concerningPatterns"] as? List<*>)?.mapNotNull { c -> c as? String } ?: emptyList(),
                responseQuality = responseQuality,
                strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { a -> a as? String } ?: emptyList()
            )
        }

        // Parse instructor score if present
        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            // Parse category-wise comments
            val categoryWiseCommentsMap = it["categoryWiseComments"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val categoryWiseComments = categoryWiseCommentsMap.mapNotNull { (k, v) ->
                try {
                    val category = SRTCategory.valueOf(k as? String ?: "GENERAL")
                    val comment = v as? String ?: ""
                    category to comment
                } catch (e: Exception) {
                    null
                }
            }.toMap()

            SRTInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
                decisionMakingScore = (it["decisionMakingScore"] as? Number)?.toFloat() ?: 0f,
                practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
                initiativeScore = (it["initiativeScore"] as? Number)?.toFloat() ?: 0f,
                socialResponsibilityScore = (it["socialResponsibilityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                categoryWiseComments = categoryWiseComments,
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                exemplaryResponses = (it["exemplaryResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        // Parse OLQ result if present
        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)

        // Parse analysis status
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.valueOf(analysisStatusStr)
            } else {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
            }
        } catch (e: Exception) {
            com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
        }

        return SRTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try {
                SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW")
            } catch (e: Exception) {
                SubmissionStatus.SUBMITTED_PENDING_REVIEW
            },
            aiPreliminaryScore = aiPreliminaryScore,
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    /**
     * Parse SDT submission from Firestore data map
     */
    private fun parseSDTSubmission(data: Map<*, *>): SDTSubmission {
        // Parse responses
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                SDTQuestionResponse(
                    questionId = it["questionId"] as? String ?: "",
                    question = it["question"] as? String ?: "",
                    answer = it["answer"] as? String ?: "",
                    wordCount = (it["wordCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        // Parse AI score if present
        val aiScoreMap = data["aiPreliminaryScore"] as? Map<*, *>
        val aiPreliminaryScore = aiScoreMap?.let {
            // Parse question-wise analysis
            val questionAnalysisList = it["questionWiseAnalysis"] as? List<*> ?: emptyList<Any>()
            val questionWiseAnalysis = questionAnalysisList.mapNotNull { analysisData ->
                (analysisData as? Map<*, *>)?.let { analysis ->
                    QuestionAnalysis(
                        questionId = analysis["questionId"] as? String ?: "",
                        sequenceNumber = (analysis["sequenceNumber"] as? Number)?.toInt() ?: 0,
                        score = (analysis["score"] as? Number)?.toFloat() ?: 0f,
                        themes = (analysis["themes"] as? List<*>)?.mapNotNull { t -> t as? String } ?: emptyList(),
                        sentimentScore = (analysis["sentimentScore"] as? Number)?.toFloat() ?: 0f,
                        keyInsights = (analysis["keyInsights"] as? List<*>)?.mapNotNull { k -> k as? String } ?: emptyList()
                    )
                }
            }

            // Parse response quality
            val responseQualityStr = it["responseQuality"] as? String
            val responseQuality = try {
                if (responseQualityStr != null) {
                    ResponseQuality.valueOf(responseQualityStr)
                } else {
                    ResponseQuality.AVERAGE
                }
            } catch (e: Exception) {
                ResponseQuality.AVERAGE
            }

            SDTAIScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                selfAwarenessScore = (it["selfAwarenessScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                socialPerceptionScore = (it["socialPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                introspectionScore = (it["introspectionScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String,
                positiveTraits = (it["positiveTraits"] as? List<*>)?.mapNotNull { t -> t as? String } ?: emptyList(),
                concerningPatterns = (it["concerningPatterns"] as? List<*>)?.mapNotNull { c -> c as? String } ?: emptyList(),
                responseQuality = responseQuality,
                strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { a -> a as? String } ?: emptyList(),
                questionWiseAnalysis = questionWiseAnalysis
            )
        }

        // Parse instructor score if present
        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            SDTInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                selfAwarenessScore = (it["selfAwarenessScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                socialPerceptionScore = (it["socialPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                introspectionScore = (it["introspectionScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                exemplaryResponses = (it["exemplaryResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        // Parse OLQ result if present
        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)

        // Parse analysis status
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.valueOf(analysisStatusStr)
            } else {
                com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
            }
        } catch (e: Exception) {
            com.ssbmax.core.domain.model.scoring.AnalysisStatus.PENDING_ANALYSIS
        }

        return SDTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try {
                SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW")
            } catch (e: Exception) {
                SubmissionStatus.SUBMITTED_PENDING_REVIEW
            },
            aiPreliminaryScore = aiPreliminaryScore,
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    /**
     * Parse OLQ analysis result from Firestore data map
     */
    private fun parseOLQResult(data: Map<*, *>?): com.ssbmax.core.domain.model.scoring.OLQAnalysisResult? {
        if (data == null) return null

        return try {
            // Parse test type
            val testTypeStr = data["testType"] as? String ?: return null
            val testType = TestType.valueOf(testTypeStr)

            // Parse OLQ scores
            val olqScoresMap = data["olqScores"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val olqScores = olqScoresMap.mapNotNull { (k, v) ->
                try {
                    val olq = com.ssbmax.core.domain.model.interview.OLQ.valueOf(k as? String ?: "")
                    val scoreMap = v as? Map<*, *> ?: return@mapNotNull null
                    val score = com.ssbmax.core.domain.model.interview.OLQScore(
                        score = (scoreMap["score"] as? Number)?.toInt() ?: 5,
                        confidence = (scoreMap["confidence"] as? Number)?.toInt() ?: 0,
                        reasoning = scoreMap["reasoning"] as? String ?: ""
                    )
                    olq to score
                } catch (e: Exception) {
                    null
                }
            }.toMap()

            com.ssbmax.core.domain.model.scoring.OLQAnalysisResult(
                submissionId = data["submissionId"] as? String ?: "",
                testType = testType,
                olqScores = olqScores,
                overallScore = (data["overallScore"] as? Number)?.toFloat() ?: 5f,
                overallRating = data["overallRating"] as? String ?: "",
                strengths = (data["strengths"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                weaknesses = (data["weaknesses"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                recommendations = (data["recommendations"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                analyzedAt = (data["analyzedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                aiConfidence = (data["aiConfidence"] as? Number)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // ===========================
    // Archival Methods
    // ===========================
    
    /**
     * Archive submissions older than the specified timestamp
     * Moves data to archived_submissions collection and deletes from main collection
     * 
     * @param beforeTimestamp Unix timestamp - submissions before this will be archived
     * @return Number of submissions successfully archived
     */
    override suspend fun archiveOldSubmissions(beforeTimestamp: Long): Result<Int> {
        return try {
            Log.d(TAG, "Starting archival for submissions before $beforeTimestamp")
            
            // Query all submissions older than timestamp using collection group
            val submissionsSnapshot = firestore.collectionGroup("submissions")
                .whereLessThan("submittedAt", beforeTimestamp)
                .get()
                .await()
            
            if (submissionsSnapshot.isEmpty) {
                Log.d(TAG, "No submissions to archive")
                return Result.success(0)
            }
            
            var archivedCount = 0
            val failedArchives = mutableListOf<String>()
            
            // Archive each submission
            submissionsSnapshot.documents.forEach { doc ->
                try {
                    // Get submission data
                    val submissionData = doc.data ?: emptyMap()
                    val submissionId = submissionData["id"] as? String ?: doc.id
                    
                    // Move to archived_submissions collection
                    firestore.collection("archived_submissions")
                        .document(submissionId)
                        .set(submissionData)
                        .await()
                    
                    // Delete from original collection
                    doc.reference.delete().await()
                    
                    archivedCount++
                    
                    if (archivedCount % 10 == 0) {
                        Log.d(TAG, "Archived $archivedCount submissions so far...")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to archive submission ${doc.id}", e)
                    failedArchives.add(doc.id)
                }
            }
            
            Log.d(TAG, "✅ Archival complete: $archivedCount archived, ${failedArchives.size} failed")
            
            if (failedArchives.isNotEmpty()) {
                Log.w(TAG, "Failed to archive: ${failedArchives.joinToString()}")
            }
            
            Result.success(archivedCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Archival failed", e)
            Result.failure(e)
        }
    }
}

/**
 * Extension functions to convert domain models to maps for Firestore
 */
private fun OIRSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "testResult" to mapOf(
            "testId" to testResult.testId,
            "sessionId" to testResult.sessionId,
            "userId" to testResult.userId,
            "totalQuestions" to testResult.totalQuestions,
            "correctAnswers" to testResult.correctAnswers,
            "incorrectAnswers" to testResult.incorrectAnswers,
            "skippedQuestions" to testResult.skippedQuestions,
            "totalTimeSeconds" to testResult.totalTimeSeconds,
            "timeTakenSeconds" to testResult.timeTakenSeconds,
            "rawScore" to testResult.rawScore,
            "percentageScore" to testResult.percentageScore,
            "categoryScores" to testResult.categoryScores.entries.associate { (category, score) ->
                category.name to mapOf(
                    "category" to score.category.name,
                    "totalQuestions" to score.totalQuestions,
                    "correctAnswers" to score.correctAnswers,
                    "percentage" to score.percentage,
                    "averageTimeSeconds" to score.averageTimeSeconds
                )
            },
            "difficultyBreakdown" to testResult.difficultyBreakdown.entries.associate { (difficulty, score) ->
                difficulty.name to mapOf(
                    "difficulty" to score.difficulty.name,
                    "totalQuestions" to score.totalQuestions,
                    "correctAnswers" to score.correctAnswers,
                    "percentage" to score.percentage
                )
            },
            "answeredQuestions" to testResult.answeredQuestions.map { aq ->
                mapOf(
                    "questionId" to aq.question.id,
                    "questionNumber" to aq.question.questionNumber,
                    "questionType" to aq.question.type.name,
                    "questionText" to aq.question.questionText,
                    "difficulty" to aq.question.difficulty.name,
                    "correctAnswerId" to aq.question.correctAnswerId,
                    "selectedOptionId" to aq.userAnswer.selectedOptionId,
                    "isCorrect" to aq.isCorrect,
                    "timeTakenSeconds" to aq.userAnswer.timeTakenSeconds,
                    "skipped" to aq.userAnswer.skipped
                )
            },
            "completedAt" to testResult.completedAt,
            "passed" to testResult.passed,
            "grade" to testResult.grade.name
        ),
        "submittedAt" to submittedAt,
        "status" to status.name,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun PPDTSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "submissionId" to submissionId,
        "questionId" to questionId,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "batchId" to batchId,
        "story" to story,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "writingTimeTakenMinutes" to writingTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.let {
            mapOf(
                "perceptionScore" to it.perceptionScore,
                "imaginationScore" to it.imaginationScore,
                "narrationScore" to it.narrationScore,
                "characterDepictionScore" to it.characterDepictionScore,
                "positivityScore" to it.positivityScore,
                "overallScore" to it.overallScore,
                "feedback" to it.feedback,
                "strengths" to it.strengths,
                "areasForImprovement" to it.areasForImprovement
            )
        },
        "instructorReview" to instructorReview?.let {
            mapOf(
                "reviewId" to it.reviewId,
                "instructorId" to it.instructorId,
                "instructorName" to it.instructorName,
                "finalScore" to it.finalScore,
                "feedback" to it.feedback,
                "detailedScores" to mapOf(
                    "perception" to it.detailedScores.perception,
                    "imagination" to it.detailedScores.imagination,
                    "narration" to it.detailedScores.narration,
                    "characterDepiction" to it.detailedScores.characterDepiction,
                    "positivity" to it.detailedScores.positivity
                ),
                "agreedWithAI" to it.agreedWithAI,
                "reviewedAt" to it.reviewedAt,
                "timeSpentMinutes" to it.timeSpentMinutes
            )
        }
    )
}

private fun TATSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "stories" to stories.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun TATStoryResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "story" to story,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "writingTimeTakenSeconds" to writingTimeTakenSeconds,
        "submittedAt" to submittedAt
    )
}

private fun TATAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "thematicPerceptionScore" to thematicPerceptionScore,
        "imaginationScore" to imaginationScore,
        "characterDepictionScore" to characterDepictionScore,
        "emotionalToneScore" to emotionalToneScore,
        "narrativeStructureScore" to narrativeStructureScore,
        "feedback" to feedback,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement
    )
}

private fun TATInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "thematicPerceptionScore" to thematicPerceptionScore,
        "imaginationScore" to imaginationScore,
        "characterDepictionScore" to characterDepictionScore,
        "emotionalToneScore" to emotionalToneScore,
        "narrativeStructureScore" to narrativeStructureScore,
        "feedback" to feedback,
        "storyWiseComments" to storyWiseComments,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

private fun WATSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun WATWordResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "wordId" to wordId,
        "word" to word,
        "response" to response,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

private fun WATAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "positivityScore" to positivityScore,
        "creativityScore" to creativityScore,
        "speedScore" to speedScore,
        "relevanceScore" to relevanceScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "feedback" to feedback,
        "positiveWords" to positiveWords,
        "negativeWords" to negativeWords,
        "neutralWords" to neutralWords,
        "uniqueResponsesCount" to uniqueResponsesCount,
        "repeatedPatterns" to repeatedPatterns,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement
    )
}

private fun WATInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "positivityScore" to positivityScore,
        "creativityScore" to creativityScore,
        "speedScore" to speedScore,
        "relevanceScore" to relevanceScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "feedback" to feedback,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt
    )
}

private fun SRTSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun SRTSituationResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "situationId" to situationId,
        "situation" to situation,
        "response" to response,
        "charactersCount" to charactersCount,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

private fun SRTAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "leadershipScore" to leadershipScore,
        "decisionMakingScore" to decisionMakingScore,
        "practicalityScore" to practicalityScore,
        "initiativeScore" to initiativeScore,
        "socialResponsibilityScore" to socialResponsibilityScore,
        "feedback" to feedback,
        "categoryWiseScores" to categoryWiseScores.mapKeys { it.key.name },
        "positiveTraits" to positiveTraits,
        "concerningPatterns" to concerningPatterns,
        "responseQuality" to responseQuality.name,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement
    )
}

private fun SRTInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "leadershipScore" to leadershipScore,
        "decisionMakingScore" to decisionMakingScore,
        "practicalityScore" to practicalityScore,
        "initiativeScore" to initiativeScore,
        "socialResponsibilityScore" to socialResponsibilityScore,
        "feedback" to feedback,
        "categoryWiseComments" to categoryWiseComments.mapKeys { it.key.name },
        "flaggedResponses" to flaggedResponses,
        "exemplaryResponses" to exemplaryResponses,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

// ==================== SDT Mapping Functions ====================

private fun SDTSubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "testId" to testId,
        "responses" to responses.map { it.toMap() },
        "totalTimeTakenMinutes" to totalTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.toMap(),
        "instructorScore" to instructorScore?.toMap(),
        "gradedByInstructorId" to gradedByInstructorId,
        "gradingTimestamp" to gradingTimestamp
    )
}

private fun SDTQuestionResponse.toMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "question" to question,
        "answer" to answer,
        "wordCount" to wordCount,
        "timeTakenSeconds" to timeTakenSeconds,
        "submittedAt" to submittedAt,
        "isSkipped" to isSkipped
    )
}

private fun SDTAIScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "selfAwarenessScore" to selfAwarenessScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "socialPerceptionScore" to socialPerceptionScore,
        "introspectionScore" to introspectionScore,
        "feedback" to feedback,
        "positiveTraits" to positiveTraits,
        "concerningPatterns" to concerningPatterns,
        "responseQuality" to responseQuality.name,
        "strengths" to strengths,
        "areasForImprovement" to areasForImprovement,
        "questionWiseAnalysis" to questionWiseAnalysis.map { it.toMap() }
    )
}

private fun QuestionAnalysis.toMap(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "sequenceNumber" to sequenceNumber,
        "score" to score,
        "themes" to themes,
        "sentimentScore" to sentimentScore,
        "keyInsights" to keyInsights
    )
}

private fun SDTInstructorScore.toMap(): Map<String, Any?> {
    return mapOf(
        "overallScore" to overallScore,
        "selfAwarenessScore" to selfAwarenessScore,
        "emotionalMaturityScore" to emotionalMaturityScore,
        "socialPerceptionScore" to socialPerceptionScore,
        "introspectionScore" to introspectionScore,
        "feedback" to feedback,
        "flaggedResponses" to flaggedResponses,
        "exemplaryResponses" to exemplaryResponses,
        "gradedByInstructorId" to gradedByInstructorId,
        "gradedByInstructorName" to gradedByInstructorName,
        "gradedAt" to gradedAt,
        "agreedWithAI" to agreedWithAI
    )
}

private fun GPESubmission.toMap(): Map<String, Any?> {
    return mapOf(
        "submissionId" to submissionId,
        "questionId" to questionId,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "batchId" to batchId,
        "planningResponse" to planningResponse,
        "charactersCount" to charactersCount,
        "viewingTimeTakenSeconds" to viewingTimeTakenSeconds,
        "planningTimeTakenMinutes" to planningTimeTakenMinutes,
        "submittedAt" to submittedAt,
        "status" to status.name,
        "aiPreliminaryScore" to aiPreliminaryScore?.let {
            mapOf(
                "situationAnalysisScore" to it.situationAnalysisScore,
                "planningQualityScore" to it.planningQualityScore,
                "leadershipScore" to it.leadershipScore,
                "resourceUtilizationScore" to it.resourceUtilizationScore,
                "practicalityScore" to it.practicalityScore,
                "overallScore" to it.overallScore,
                "feedback" to it.feedback,
                "strengths" to it.strengths,
                "areasForImprovement" to it.areasForImprovement
            )
        },
        "instructorReview" to instructorReview?.let {
            mapOf(
                "reviewId" to it.reviewId,
                "instructorId" to it.instructorId,
                "instructorName" to it.instructorName,
                "finalScore" to it.finalScore,
                "feedback" to it.feedback,
                "detailedScores" to mapOf(
                    "situationAnalysis" to it.detailedScores.situationAnalysis,
                    "planningQuality" to it.detailedScores.planningQuality,
                    "leadership" to it.detailedScores.leadership,
                    "resourceUtilization" to it.detailedScores.resourceUtilization,
                    "practicality" to it.detailedScores.practicality
                ),
                "agreedWithAI" to it.agreedWithAI,
                "reviewedAt" to it.reviewedAt,
                "timeSpentMinutes" to it.timeSpentMinutes
            )
        }
    )
}

