package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.CloudStudyMaterial

/**
 * Topic content as resolved for display, plus which tier it came from.
 *
 * Extracted from `GitLiveStudyContentRepository.kt` by Move 2 of the iOS
 * CocoaPods->SPM convergence. Declaring it inside a Firebase repository
 * implementation was a layering inversion: `TopicViewModel` (presentation, in
 * `:shared`) consumes it via an `is TopicContentData` check on what
 * `StudyContentRepository.getTopicContent()` emits, so the type is part of
 * that repository's effective contract and cannot live below it in
 * `:data-firebase`.
 *
 * The `is`-check at the call site is itself worth revisiting -- a repository
 * contract typed loosely enough to need a runtime type test is the reason
 * this was easy to misplace -- but that is a behavior change, out of scope
 * for a module extraction.
 */
data class TopicContentData(
    val title: String,
    val introduction: String,
    val materials: List<CloudStudyMaterial>,
    val source: ContentSource
)

/** Which tier [TopicContentData] was resolved from. */
enum class ContentSource {
    CLOUD,
    LOCAL
}
