package com.ssbmax.shared.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.firestore

internal const val GTO_COLLECTION_SUBMISSIONS = "submissions"
internal const val GTO_COLLECTION_RESULTS = "gto_results"
internal const val GTO_COLLECTION_USER_PROGRESS = "user_progress"
internal const val GTO_FIELD_USER_ID = "userId"
internal const val GTO_FIELD_TEST_TYPE = "testType"
internal const val GTO_FIELD_STATUS = "status"
internal const val GTO_FIELD_SUBMITTED_AT = "submittedAt"

/**
 * Shared Firestore collection handles for the GTO repository cluster. Extracted out of the former
 * single `GitLiveGTORepository` god-class (300-line-file limit) so the outer repository and its
 * [GitLiveGTOSubmissionDelegate]/[GitLiveGTOProgressDelegate]/[GitLiveGTOResultsDelegate] delegates
 * all share one Firestore connection ("submissions"/"gto_results"/"user_progress", same as the
 * Android original) instead of each opening their own. Pure structural split — no behavior change
 * from the original merged class.
 */
internal class GitLiveGTOCollections {
    val submissions: CollectionReference = Firebase.firestore.collection(GTO_COLLECTION_SUBMISSIONS)
    val results: CollectionReference = Firebase.firestore.collection(GTO_COLLECTION_RESULTS)
    val progress: CollectionReference = Firebase.firestore.collection(GTO_COLLECTION_USER_PROGRESS)
}
