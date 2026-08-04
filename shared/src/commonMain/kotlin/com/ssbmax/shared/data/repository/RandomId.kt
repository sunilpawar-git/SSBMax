package com.ssbmax.shared.data.repository

/**
 * Random 128-bit hex identifier, used where a document/entity ID must be
 * generated client-side rather than assigned by Firestore.
 *
 * Extracted from `GitLiveQuestionCacheRepository.kt` by Move 2 of the iOS
 * CocoaPods->SPM convergence, which surfaced that it had never belonged
 * there: it is general-purpose, has nothing to do with question caching, and
 * is called from `InterviewFallbackQuestions` and `InterviewResultAggregation`
 * too -- files that stay in `:shared` while the GitLive repositories move to
 * `:data-firebase`.
 *
 * Public rather than `internal` for the same reason: Kotlin's `internal` is
 * module-scoped, so an `internal` helper cannot be shared across the new
 * module boundary.
 */
fun randomId(): String = kotlin.random.Random.nextBytes(16).joinToString("") { byte ->
    (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
}
