package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * GitLive-Firebase-backed port of the recording half of the Android `core:data`
 * `SubscriptionManager` (it implements the same [TestUsageRecorder] interface
 * that `SubmitOIRTestUseCase`/`SubmitPPDTTestUseCase` already depend on).
 *
 * Same Firestore path as the Android original and [GitLiveSubscriptionRepository]:
 * `users/{userId}/subscription/usage_{yyyy-MM}`.
 *
 * Deliberate deviation, documented not silent: the Android original wraps
 * this in a `firestore.runTransaction { }` for atomicity plus an idempotency
 * check against a `recordedSubmissions` list (so a retried submission never
 * double-counts). GitLive's `Transaction` API is not reified for arbitrary
 * document reads in this codebase's confirmed-working surface (see this
 * plan's Phase 2 note: "every repo needing atomicity uses proven
 * read-then-write instead, accepting a small non-atomic race window" —
 * the same precedent [GitLiveQuestionCacheRepository] and others already
 * follow). This recorder follows suit: read-then-write with
 * `FieldValue.increment`, no submission-idempotency de-dup. A double-submit
 * race (two calls for the same submission landing concurrently) would double
 * count usage — same small window every other GitLive repo in this codebase
 * already accepts, not a new gap introduced here.
 */
class GitLiveTestUsageRecorder : TestUsageRecorder {

    override suspend fun recordTestUsage(testType: TestType, userId: String, submissionId: String?) {
        val month = currentYearMonth()
        val docRef = Firebase.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection("subscription")
            .document("usage_$month")

        val fieldName = fieldNameFor(testType)
        val snapshot = docRef.get()

        if (!snapshot.exists) {
            // New document for the month: write via the typed DTO (matches
            // GitLiveSubscriptionRepository's own read-side use of the same
            // DTO) rather than a raw Map — GitLive has no public raw
            // Map<String, Any> encode path for `.set()` (see this plan's
            // Phase 2 note on FirestoreRawMapSerializer; a full second
            // serializer isn't warranted here when the typed DTO already
            // covers every field this document needs).
            docRef.set(SubscriptionUsageDto().withIncrementedField(fieldName))
        } else {
            docRef.update(fieldName to FieldValue.increment(1))
        }
    }

    private fun SubscriptionUsageDto.withIncrementedField(fieldName: String): SubscriptionUsageDto = when (fieldName) {
        "oirTestsUsed" -> copy(oirTestsUsed = 1)
        "tatTestsUsed" -> copy(tatTestsUsed = 1)
        "watTestsUsed" -> copy(watTestsUsed = 1)
        "srtTestsUsed" -> copy(srtTestsUsed = 1)
        "ppdtTestsUsed" -> copy(ppdtTestsUsed = 1)
        "piqTestsUsed" -> copy(piqTestsUsed = 1)
        "gtoTestsUsed" -> copy(gtoTestsUsed = 1)
        "interviewTestsUsed" -> copy(interviewTestsUsed = 1)
        "sdTestsUsed" -> copy(sdTestsUsed = 1)
        else -> this
    }

    private fun fieldNameFor(testType: TestType): String = when (testType) {
        TestType.OIR -> "oirTestsUsed"
        TestType.TAT -> "tatTestsUsed"
        TestType.WAT -> "watTestsUsed"
        TestType.SRT -> "srtTestsUsed"
        TestType.PPDT -> "ppdtTestsUsed"
        TestType.PIQ -> "piqTestsUsed"
        TestType.GTO_GD, TestType.GTO_GPE, TestType.GTO_PGT, TestType.GTO_GOR,
        TestType.GTO_HGT, TestType.GTO_LECTURETTE, TestType.GTO_IO, TestType.GTO_CT -> "gtoTestsUsed"
        TestType.IO -> "interviewTestsUsed"
        TestType.SD -> "sdTestsUsed"
    }

    private fun currentYearMonth(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
