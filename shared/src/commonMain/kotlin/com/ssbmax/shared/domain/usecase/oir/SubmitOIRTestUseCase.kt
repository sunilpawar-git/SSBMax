package com.ssbmax.shared.domain.usecase.oir

import kotlinx.datetime.Clock

import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase

/**
 * Orchestrates the full OIR test-submission pipeline in a single, testable use case.
 *
 * Steps (run in strict order — any failure short-circuits remaining steps):
 *  1. Calculate score from the completed session.
 *  2. Record test usage against the user's subscription quota.
 *  3. Invalidate the OLQ dashboard cache so the new score appears immediately.
 *  4. Persist the submission to Firestore and obtain the submission ID.
 *  5. Mark the test session as ended in Firestore.
 *
 * Returns `Result<String>` — the submission ID on success.
 */
class SubmitOIRTestUseCase constructor(
    private val scoreCalculator: OIRTestScoreCalculator,
    private val usageRecorder: TestUsageRecorder,
    private val dashboardUseCase: GetOLQDashboardUseCase,
    private val submissionRepository: SubmissionRepository,
    private val testSessionRepository: TestSessionRepository,
    private val testContentRepository: TestContentRepository
) {

    suspend operator fun invoke(session: OIRTestSession): Result<String> {
        return runCatching {
            // Step 1: Calculate score
            val result = scoreCalculator.calculate(session)

            // Step 2: Record subscription usage — failure propagates (blocks steps 3+)
            usageRecorder.recordTestUsage(TestType.OIR, session.userId)

            // Step 3: Invalidate OLQ dashboard cache
            dashboardUseCase.invalidateCache(session.userId)

            // Step 4: Persist submission — use session ID as the document ID so the
            // result screen can load it without an extra Firestore lookup
            val submission = OIRSubmission(
                id          = session.sessionId,
                userId      = session.userId,
                testId      = session.testId,
                testResult  = result,
                submittedAt = Clock.System.now().toEpochMilliseconds(),
                status      = SubmissionStatus.SUBMITTED_PENDING_REVIEW
            )
            submissionRepository.submitOIR(submission, null).getOrThrow()

            // Step 5: End the test session
            testSessionRepository.endTestSession(session.sessionId)

            // Step 6: Mark served questions as used (best-effort — never fails the submission)
            runCatching {
                testContentRepository.markOIRQuestionsUsed(session.questions.map { it.id })
            }

            // Return the submission ID
            submission.id
        }
    }
}
