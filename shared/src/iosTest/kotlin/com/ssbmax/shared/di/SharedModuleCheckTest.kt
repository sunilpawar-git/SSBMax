package com.ssbmax.shared.di

import com.ssbmax.shared.platform.util.UninstalledAnalyticsTracker
import com.ssbmax.shared.platform.util.UninstalledCrashReporter
import org.koin.test.check.checkModules
import org.koin.test.KoinTest
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Phase 0d (KMP-convergence plan): pins that every constructor dependency in
 * `sharedModule` resolves against the iOS `platformModule` actual -- this,
 * not `commonTest`, because Koin's `androidContext()` extension used by the
 * Android actual needs a real/stubbed `Context` in the container before
 * resolution (see [PlatformModuleCheckTest] in `app/src/test`, the other
 * half of this check), which `commonTest` can't provide.
 *
 * RAN FOR THE FIRST TIME 2026-08-04, and failed exactly as this doc's earlier
 * revision predicted it would. `checkModules` really instantiates every
 * definition, so it reaches `UpgradeViewModel` -> a GitLive repository ->
 * live `FIRAuth`, and dies with "The default FirebaseApp instance must be
 * configured". The other 783 tests in this suite passed on that same run.
 *
 * It is @Ignore'd rather than fixed, deliberately -- see the annotation on
 * the test below for why each available fix was rejected.
 */
class SharedModuleCheckTest : KoinTest {

    /**
     * Checks the exact module pair `ensureKoinStarted()` starts, not
     * `sharedModule` alone. `CrashReporter`/`AnalyticsTracker` are no longer
     * bound by iOS's `platformModule` -- Swift owns them, and
     * [iosObservabilityModule] carries them in (see its doc). Consumers of
     * both interfaces live in `sharedModule` (ViewModels, CheckTestEligibilityUseCase),
     * so this fails loudly if the two modules ever stop composing into a
     * complete graph -- which is the failure mode the split introduces and
     * the only reason it's worth pinning.
     */
    /**
     * IGNORED -- needs a live `FirebaseApp`, because `checkModules` really
     * constructs `UpgradeViewModel` -> GitLive -> `FIRAuth`.
     * (`kotlin.test.Ignore` takes no reason argument, unlike JUnit's, hence
     * this comment.)
     *
     * Rejected fixes, so they aren't re-attempted:
     * - Configuring real Firebase would make a DI-wiring test depend on a
     *   `GoogleService-Info.plist` credential -- no other test here does.
     * - Android's counterpart `PlatformModuleCheckTest` `mockkStatic`s
     *   `FirebaseApp.getInstance()`; that cannot port, since MockK has no
     *   KMP/Native artifact and Obj-C class methods aren't mockable from
     *   Kotlin/Native.
     * - Narrowing to Firebase-free modules would drop the one assertion
     *   actually worth having.
     *
     * RE-ENABLE IN MOVE 2 (iOS CocoaPods->SPM convergence): once the
     * `GitLive*` repositories move to a leaf module, `:shared`'s graph
     * contains no Firebase and this passes for the right reason -- no mock,
     * no credential.
     */
    @Test
    @Ignore
    fun `the module pair ensureKoinStarted loads resolves every dependency`() {
        checkModules {
            modules(
                sharedModule,
                iosObservabilityModule(UninstalledCrashReporter(), UninstalledAnalyticsTracker())
            )
        }
    }
}
