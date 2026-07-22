package com.ssbmax.shared.domain.usecase.submission

import com.ssbmax.shared.domain.model.TATSubmission
import com.ssbmax.shared.domain.repository.SubmissionRepository

/**
 * Use case for submitting TAT test.
 *
 * Kept as a thin pass-through to [SubmissionRepository.submitTAT] --
 * NOT widened to build the [TATSubmission] itself (unlike
 * [com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestUseCase]'s
 * build-then-record-usage shape). A first attempt at that widening was
 * reverted: `app/.../ui/tests/tat/TATTestViewModel.kt` (the real, live
 * Android production TAT flow, untouched by this KMP migration) already
 * calls this exact `invoke(submission, batchId)` signature -- verified only
 * *after* hitting a real `:app:compileDebugKotlin` failure, which is the
 * regression-prevention lesson worth stating plainly: this file having zero
 * callers *within `shared`* does not mean zero callers overall, and grepping
 * only `shared/` before changing a `shared` use case's signature is not
 * sufficient when `app` also depends on `shared`.
 *
 * Consequence: [com.ssbmax.shared.presentation.tat.TATTestViewModel] (the
 * KMP port) builds its own [TATSubmission] and resolves `subscriptionType`/
 * records usage directly, the same way the Android original's ViewModel
 * does -- it does not get PPDT's thinner call site, and that is intentional
 * here, not a shortcut.
 */
class SubmitTATTestUseCase constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: TATSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitTAT(submission, batchId)
    }
}
