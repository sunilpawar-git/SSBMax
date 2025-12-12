package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.utils.ErrorLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

/**
 * Background worker for analyzing GTO test submissions using Gemini AI
 *
 * This worker:
 * 1. Fetches submission from Firestore
 * 2. Analyzes response with Gemini AI for OLQ scores
 * 3. Updates submission with OLQ scores
 * 4. Marks submission as completed
 * 5. Sends push notification when complete
 *
 * The user is free to navigate away while this runs in the background.
 */
@HiltWorker
class GTOAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val gtoRepository: GTORepository,
    private val aiService: AIService,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "GTOAnalysisWorker"
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val API_CALL_DELAY_MS = 500L // Delay between API calls
        private const val FALLBACK_CONFIDENCE = 30 // Low confidence for fallback scores
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID)
        if (submissionId.isNullOrBlank()) {
            Log.e(TAG, "❌ No submission ID provided")
            ErrorLogger.log(
                "GTO analysis worker started without submission ID",
                emptyMap(),
                ErrorLogger.Severity.ERROR
            )
            return Result.failure()
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 Starting GTO background analysis for submission: $submissionId")
        ErrorLogger.log(
            "GTO analysis worker started",
            mapOf("submissionId" to submissionId, "attempt" to runAttemptCount.toString()),
            ErrorLogger.Severity.INFO
        )

        return try {
            // 1. Get submission
            val submissionResult = gtoRepository.getSubmission(submissionId)
            val submission = submissionResult.getOrNull()
            if (submission == null) {
                Log.e(TAG, "❌ Submission not found: $submissionId")
                return Result.failure()
            }

            Log.d(TAG, "   Step 1: Submission found - ${submission.testType}")

            // Verify submission is in PENDING_ANALYSIS state
            if (submission.status != GTOSubmissionStatus.PENDING_ANALYSIS) {
                Log.w(TAG, "⚠️ Submission not in PENDING_ANALYSIS state: ${submission.status}")
                // Don't fail, just skip - might be already processed
                return Result.success()
            }

            // 2. Update status to ANALYZING
            gtoRepository.updateSubmissionStatus(submissionId, GTOSubmissionStatus.ANALYZING)
            Log.d(TAG, "   Step 2: Status updated to ANALYZING")

            // 3. Analyze with AI
            Log.d(TAG, "   Step 3: Analyzing with AI...")
            val analysisResult = analyzeSubmissionWithRetry(submission)

            if (analysisResult != null) {
                // Update submission with OLQ scores
                gtoRepository.updateSubmissionOLQScores(submissionId, analysisResult)
                Log.d(TAG, "   ✅ Analysis complete and saved")

                // Send notification
                val duration = (System.currentTimeMillis() - startTime) / 1000
                Log.d(TAG, "✅ GTO analysis completed in ${duration}s")
                
                notificationHelper.showGTOAnalysisCompleteNotification(
                    submissionId,
                    submission.testType.displayName
                )

                ErrorLogger.log(
                    "GTO analysis worker completed successfully",
                    mapOf(
                        "submissionId" to submissionId,
                        "testType" to submission.testType.name,
                        "duration" to duration.toString()
                    ),
                    ErrorLogger.Severity.INFO
                )

                Result.success()
            } else {
                Log.e(TAG, "   ❌ AI analysis failed after retries")

                // Use fallback scores
                val fallbackScores = generateFallbackOLQScores()
                gtoRepository.updateSubmissionOLQScores(submissionId, fallbackScores)

                Log.w(TAG, "⚠️ Using fallback scores due to AI failure")
                notificationHelper.showGTOAnalysisCompleteNotification(
                    submissionId,
                    submission.testType.displayName
                )

                ErrorLogger.log(
                    "GTO analysis worker completed with fallback scores",
                    mapOf(
                        "submissionId" to submissionId,
                        "testType" to submission.testType.name
                    ),
                    ErrorLogger.Severity.WARNING
                )

                Result.success()
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "GTO analysis worker failed")
            Log.e(TAG, "❌ Worker failed", e)

            // Mark as failed
            try {
                gtoRepository.updateSubmissionStatus(submissionId!!, GTOSubmissionStatus.FAILED)
            } catch (updateError: Exception) {
                ErrorLogger.log(updateError, "Failed to update submission status to FAILED")
            }

            // Retry up to 3 times
            return if (runAttemptCount < 3) {
                Log.d(TAG, "⚠️ Retrying (attempt ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                Log.e(TAG, "❌ Max retries reached, failing")
                Result.failure()
            }
        }
    }

    /**
     * Analyze submission with retry logic
     */
    private suspend fun analyzeSubmissionWithRetry(
        submission: GTOSubmission,
        maxRetries: Int = 3
    ): Map<OLQ, OLQScore>? {
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "      AI attempt ${attempt + 1}/$maxRetries...")

                val prompt = generateAnalysisPrompt(submission)
                val olqScores = analyzeWithGemini(prompt, submission.testType)

                if (olqScores != null && olqScores.size == 15) {
                    Log.d(TAG, "      ✅ AI analysis successful")
                    return olqScores
                }

                Log.w(TAG, "      ⚠️ AI returned incomplete scores (${olqScores?.size ?: 0}/15)")
            } catch (e: Exception) {
                ErrorLogger.log(e, "AI analysis attempt ${attempt + 1} failed")
                Log.e(TAG, "      ❌ AI attempt ${attempt + 1} failed", e)
            }

            if (attempt < maxRetries - 1) {
                val backoffDelay = API_CALL_DELAY_MS * (attempt + 1) * 2
                Log.d(TAG, "      Waiting ${backoffDelay}ms before retry...")
                delay(backoffDelay)
            }
        }

        return null
    }

    /**
     * Generate Gemini prompt based on submission type
     */
    private fun generateAnalysisPrompt(submission: GTOSubmission): String {
        return when (submission) {
            is GTOSubmission.GDSubmission -> generateGDPrompt(submission)
            is GTOSubmission.GPESubmission -> generateGPEPrompt(submission)
            is GTOSubmission.LecturetteSubmission -> generateLecturettePrompt(submission)
            is GTOSubmission.PGTSubmission -> generatePGTPrompt(submission)
            is GTOSubmission.HGTSubmission -> generateHGTPrompt(submission)
            is GTOSubmission.GORSubmission -> generateGORPrompt(submission)
            is GTOSubmission.IOSubmission -> generateIOPrompt(submission)
            is GTOSubmission.CTSubmission -> generateCTPrompt(submission)
        }
    }

    private fun generateGDPrompt(submission: GTOSubmission.GDSubmission): String {
        return """
You are analyzing a Group Discussion response for SSB GTO assessment.

**Topic**: ${submission.topic}
**Candidate Response**: 
${submission.response}

**Word Count**: ${submission.wordCount}
**Time Spent**: ${submission.timeSpent} seconds

Evaluate the candidate's response against ALL 15 Officer-Like Qualities (OLQs):

1. **Effective Intelligence**: Clarity, logic, analytical thinking
2. **Reasoning Ability**: Problem-solving approach
3. **Organizing Ability**: Structure of arguments
4. **Power of Expression**: Articulation, communication clarity
5. **Social Adjustment**: Respect for diverse views
6. **Cooperation**: Collaborative tone
7. **Sense of Responsibility**: Accountability in arguments
8. **Initiative**: Leadership potential in discussion
9. **Self Confidence**: Conviction in opinions
10. **Speed of Decision**: Decisiveness in stance
11. **Ability to Influence Group**: Persuasiveness
12. **Liveliness**: Energy and enthusiasm
13. **Determination**: Firmness in viewpoint
14. **Courage**: Willingness to take bold positions
15. **Stamina**: Sustained quality throughout

**SSB Scoring Convention (CRITICAL)**:
- Scale: 1-10 where LOWER is BETTER
- 1-3: Exceptional (rare, outstanding)
- 4: Excellent (top tier)
- 5: Very Good (best common score)
- 6: Good (above average)
- 7: Average (typical performance)
- 8: Below Average (lowest acceptable)
- 9-10: Poor (usually rejected)

Return JSON format:
{
  "olqScores": {
    "EFFECTIVE_INTELLIGENCE": {"score": 5, "confidence": 80, "reasoning": "Clear analytical thinking demonstrated"},
    "REASONING_ABILITY": {"score": 6, "confidence": 75, "reasoning": "Logical arguments presented"},
    ... (all 15 OLQs)
  }
}
        """.trimIndent()
    }

    private fun generateGPEPrompt(submission: GTOSubmission.GPESubmission): String {
        return """
You are analyzing a Group Planning Exercise response for SSB GTO assessment.

**Scenario**: ${submission.scenario}
**Candidate's Plan**: 
${submission.plan}

**Character Count**: ${submission.characterCount}
**Time Spent**: ${submission.timeSpent} seconds

Evaluate the candidate's tactical planning against ALL 15 OLQs, focusing on:
- Reasoning Ability: Logical approach to problem
- Organizing Ability: Resource allocation and sequencing
- Speed of Decision: Decisiveness in planning
- Initiative: Leadership in solution design
- (Include all 15 OLQs as in GD prompt)

Return JSON with all 15 OLQ scores in the same format.
        """.trimIndent()
    }

    private fun generateLecturettePrompt(submission: GTOSubmission.LecturetteSubmission): String {
        return """
You are analyzing a Lecturette (3-minute speech) for SSB GTO assessment.

**Topic Chosen**: ${submission.selectedTopic}
**Available Topics**: ${submission.topicChoices.joinToString(", ")}
**Speech Transcript**: 
${submission.speechTranscript}

**Word Count**: ${submission.wordCount}
**Time Spent**: ${submission.timeSpent} seconds

Evaluate the candidate's speech against ALL 15 OLQs, focusing on:
- Power of Expression: Fluency, articulation
- Self Confidence: Conviction and poise
- Ability to Influence Group: Persuasiveness
- Liveliness: Energy and enthusiasm
- (Include all 15 OLQs as in GD prompt)

Return JSON with all 15 OLQ scores in the same format.
        """.trimIndent()
    }

    private fun generatePGTPrompt(submission: GTOSubmission.PGTSubmission): String {
        val solutionsText = submission.solutions.joinToString("\n\n") { solution ->
            "**Obstacle ${solution.obstacleId}**: ${solution.solutionText}"
        }

        return """
You are analyzing a Progressive Group Task response for SSB GTO assessment.

**Obstacles**: ${submission.obstacles.size} progressive challenges
**Candidate's Solutions**: 
$solutionsText

**Time Spent**: ${submission.timeSpent} seconds

Evaluate the candidate's problem-solving across progressively difficult obstacles against ALL 15 OLQs.

Return JSON with all 15 OLQ scores in the same format.
        """.trimIndent()
    }

    private fun generateHGTPrompt(submission: GTOSubmission.HGTSubmission): String {
        return """
You are analyzing a Half Group Task response for SSB GTO assessment.

**Obstacle**: ${submission.obstacle.name} - ${submission.obstacle.description}
**Solution**: ${submission.solution.solutionText}
**Leadership Decisions**: ${submission.leadershipDecisions}

**Time Spent**: ${submission.timeSpent} seconds

Focus on leadership qualities: Initiative, Organizing Ability, Ability to Influence Group.
Evaluate ALL 15 OLQs.

Return JSON with all 15 OLQ scores in the same format.
        """.trimIndent()
    }

    private fun generateGORPrompt(submission: GTOSubmission.GORSubmission): String {
        return """
You are analyzing a Group Obstacle Race response for SSB GTO assessment.

**Obstacles**: ${submission.obstacles.size} team obstacles
**Coordination Strategy**: 
${submission.coordinationStrategy}

**Time Spent**: ${submission.timeSpent} seconds

Focus on teamwork: Cooperation, Social Adjustment, Organizing Ability, Stamina.
Evaluate ALL 15 OLQs.

Return JSON with all 15 OLQ scores in the same format.
        """.trimIndent()
    }

    private fun generateIOPrompt(submission: GTOSubmission.IOSubmission): String {
        return """
You are analyzing Individual Obstacles response for SSB GTO assessment.

**Obstacles**: ${submission.obstacles.size} individual challenges
**Overall Approach**: 
${submission.approach}

**Time Spent**: ${submission.timeSpent} seconds

Focus on individual qualities: Courage, Determination, Self Confidence, Stamina.
Evaluate ALL 15 OLQs.

Return JSON with all 15 OLQ scores in the same format.
        """.trimIndent()
    }

    private fun generateCTPrompt(submission: GTOSubmission.CTSubmission): String {
        return """
You are analyzing a Command Task response for SSB GTO assessment.

**Scenario**: ${submission.scenario}
**Obstacle**: ${submission.obstacle.name}
**Command Decisions**: ${submission.commandDecisions}
**Resource Allocation**: ${submission.resourceAllocation}

**Time Spent**: ${submission.timeSpent} seconds

Focus on command qualities: Initiative, Organizing Ability, Speed of Decision, Courage.
Evaluate ALL 15 OLQs.

Return JSON with all 15 OLQ scores in the same format.
        """.trimIndent()
    }

    /**
     * Analyze with Gemini (placeholder - will be implemented with actual AI service)
     */
    private suspend fun analyzeWithGemini(
        prompt: String,
        testType: GTOTestType
    ): Map<OLQ, OLQScore>? {
        return try {
            // TODO: Implement actual Gemini API call via AIService
            // For now, return null to use fallback scores
            // 
            // val response = aiService.analyzeGTOResponse(prompt)
            // return parseOLQScores(response)
            
            null
        } catch (e: Exception) {
            ErrorLogger.log(e, "Gemini API call failed for $testType")
            null
        }
    }

    /**
     * Generate fallback OLQ scores (neutral 6/10 for all)
     */
    private fun generateFallbackOLQScores(): Map<OLQ, OLQScore> {
        return OLQ.entries.associateWith { olq ->
            OLQScore(
                score = 6, // Neutral "Good" score
                confidence = FALLBACK_CONFIDENCE,
                reasoning = "AI analysis unavailable - neutral score assigned. Please retake test for accurate assessment."
            )
        }
    }
}
