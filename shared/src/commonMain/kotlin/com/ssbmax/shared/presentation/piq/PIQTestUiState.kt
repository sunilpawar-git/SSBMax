package com.ssbmax.shared.presentation.piq

import com.ssbmax.shared.domain.model.ExtraCurricularActivity
import com.ssbmax.shared.domain.model.PIQPage
import com.ssbmax.shared.domain.model.PreviousInterview
import com.ssbmax.shared.domain.model.Sibling
import com.ssbmax.shared.domain.model.SportsParticipation
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.WorkExperience
import com.ssbmax.shared.presentation.common.TestError

/**
 * KMP port of `app/.../ui/tests/piq/PIQTestViewModel.kt`'s `PIQUiState`.
 * Field-for-field match with the Android original.
 *
 * PIQ is structurally distinct from every other Phase 5 vertical ported so
 * far (OIR/PPDT/TAT/WAT/SRT/SDT): it is not a timed psychological test with a
 * question sequence, it's an untimed ~90-field personal-information form with
 * free navigation between two pages, an explicit review step, and
 * autosave-as-draft. There is no [com.ssbmax.shared.domain.model.scoring.AnalysisStatus]/OLQ
 * result -- the Android original computes a synchronous mock
 * [com.ssbmax.shared.domain.model.PIQAIScore] completeness/quality estimate inline
 * at submit time (no Gemini call, no WorkManager analysis job), ported as-is.
 */
data class PIQTestUiState(
    val testId: String = "piq_standard",
    val currentPage: PIQPage = PIQPage.PAGE_1,
    val answers: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val lastSavedAt: Long? = null,
    val lastModifiedAt: Long = 0L,
    val showReviewScreen: Boolean = false,
    val submissionComplete: Boolean = false,
    val submissionId: String? = null,
    val error: TestError? = null,
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = "",
    // Complex structures (dynamic lists not yet wired to a "+Add" UI -- same
    // gap as the Android original, which only exposes fixed elderSibling1/2
    // and youngerSibling1/2 slots via the flat `answers` map; these lists
    // stay empty in practice until a future session adds dynamic-list UI)
    val sportsParticipation: List<SportsParticipation> = emptyList(),
    val extraCurricularActivities: List<ExtraCurricularActivity> = emptyList(),
    val previousInterviews: List<PreviousInterview> = emptyList(),
    val workExperience: List<WorkExperience> = emptyList(),
    val siblings: List<Sibling> = emptyList()
) {
    /** All fields optional -- lenient form, matches Android original. */
    val canSubmit: Boolean get() = true
}

/**
 * Selection Board options for the PIQ dropdown. Matches
 * `app/.../ui/tests/piq/PIQTestViewModel.kt`'s `SELECTION_BOARD_OPTIONS` verbatim.
 */
val PIQ_SELECTION_BOARD_OPTIONS = listOf(
    "Army (Prayagraj)",
    "Army (Bhopal)",
    "Army (Bangalore)",
    "Army (Jalandhar)",
    "Navy (Coimbatore)",
    "Navy (Visakhapatnam)",
    "Navy (Kolkata)",
    "Navy (Bhopal)",
    "Navy (Bengaluru)",
    "Air Force (Dehradun)",
    "Air Force (Mysore)",
    "Air Force (Gandhinagar)",
    "Air Force (Varanasi)",
    "Air Force (Guwahati)",
    "Coast Guard (Noida)"
)
