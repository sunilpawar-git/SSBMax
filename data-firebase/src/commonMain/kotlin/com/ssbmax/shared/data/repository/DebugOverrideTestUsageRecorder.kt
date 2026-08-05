package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import com.ssbmax.shared.platform.settings.DeveloperSettings

/**
 * Debug-only decorator that no-ops [recordTestUsage] while a [DeveloperSettings] tier override is
 * active, instead of delegating to the real Firestore increment.
 *
 * Every submit path calls [TestUsageRecorder.recordTestUsage] unconditionally (TAT/PPDT/GTO and 5
 * more). Without this no-op, dev testing under an override would permanently inflate the real
 * monthly counters -- there's no in-app reset, so a dev session spent testing Force Premium would
 * leave the account limit-locked under Follow Real afterward, making the real path untestable.
 *
 * [DeveloperSettings] is read live on every call, matching [DebugOverrideSubscriptionRepository]'s
 * same live-read requirement.
 */
class DebugOverrideTestUsageRecorder(
    private val delegate: TestUsageRecorder,
    private val developerSettings: DeveloperSettings
) : TestUsageRecorder {

    override suspend fun recordTestUsage(testType: TestType, userId: String, submissionId: String?) {
        if (developerSettings.getOverride() != SubscriptionOverride.FOLLOW_REAL) {
            return
        }
        delegate.recordTestUsage(testType, userId, submissionId)
    }
}
