package com.ssbmax.shared.presentation.interviewstart

import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.PrerequisiteCheckResult
import kotlinx.datetime.Instant

/**
 * KMP port of `app/.../ui/interview/start/StartInterviewUiState.kt`. Field-for-field
 * equivalent (`java.time.Instant` -> `kotlinx.datetime.Instant`, same as every other
 * ported UiState in this phase).
 */
data class StartInterviewUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val isGeneratingQuestions: Boolean = false,
    val prerequisiteResult: PrerequisiteCheckResult? = null,
    val isEligible: Boolean = false,
    val sessionId: String? = null,
    val error: String? = null,
    val isSessionCreated: Boolean = false,
    val pastResults: List<InterviewResult> = emptyList(),
    val pendingSessions: List<PendingInterviewSession> = emptyList(),
    val isLoadingHistory: Boolean = false
) {
    fun canStartInterview(): Boolean = isEligible && !isLoading && !isGeneratingQuestions && error == null

    fun getFailureReasons(): List<String> = prerequisiteResult?.failureReasons ?: emptyList()

    fun hasPastInterviews(): Boolean = pastResults.isNotEmpty() || pendingSessions.isNotEmpty()
}

/**
 * Represents an interview session that's still being analyzed.
 *
 * Carried forward verbatim from the Android original, including the same
 * pre-existing gap: `pendingSessions` is never populated by
 * [StartInterviewViewModel.loadInterviewHistory] (only `pastResults` is),
 * and `StartInterviewScreen`'s `MainContent` never renders either
 * `pastResults` or `pendingSessions` -- the history-loading call happens on
 * every screen load but its result is dead as far as this screen's own UI is
 * concerned. Not a regression introduced by this port; ported faithfully
 * rather than silently dropped or "fixed" out of scope.
 */
data class PendingInterviewSession(
    val sessionId: String,
    val status: InterviewStatus,
    val startedAt: Instant,
    val questionCount: Int
)
