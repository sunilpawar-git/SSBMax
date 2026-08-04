package com.ssbmax.shared.di

import org.koin.dsl.module

/**
 * Root Koin module for `shared` — Phase 0 wired one vertical slice (auth + OIR
 * result); Phase 2 added the full repository/Ktor-Gemini graph; Phase 5 ported
 * 55 of 61 live Compose screens' ViewModels/use cases across many sessions.
 *
 * Split into per-vertical modules ([coreInfraModule], [repositoryModule],
 * [authHomeModule], [testTakingModule], [gtoModule], [interviewModule],
 * [profileSettingsModule], [resultsSubmissionsModule], [studyContentModule],
 * [premiumMarketplaceModule]) — this file had grown to 565 lines across
 * Phase 5's many sessions, 4x the repo's 300-line Quality Limit, mirroring
 * exactly what happened to `SSBMaxNavHost.kt` before its own exit-sweep split
 * (`AuthGraph.kt` etc.). Pure structural move, zero behavior change; see each
 * sub-module's own doc comment for its scope and the per-vertical rationale
 * (shadow-bound repositories, "existed but unbound" use cases, async-analysis
 * seam consequences) previously carried in this file's single class doc.
 *
 * `SSBMaxNavHost` (built from these verticals) is the production nav graph on
 * both platforms since the KMP-convergence plan's Phase 5 cutover.
 */
/**
 * Move 2 (iOS CocoaPods->SPM convergence): `repositoryModule` is deliberately
 * NOT included here anymore. It binds the Firebase-backed implementations and
 * now lives in `:data-firebase`, above this module -- keeping `:shared` free
 * of Firebase is what lets its Kotlin/Native test binaries link without
 * CocoaPods (see data-firebase/build.gradle.kts for the linker facts).
 *
 * Consequence: `sharedModule` alone is an INCOMPLETE graph. Every composition
 * root must load `firebaseDataModule` alongside it -- `app`'s KoinModules.kt
 * and iOS's `ensureKoinStarted()`.
 *
 * KNOWN COVERAGE GAP, recorded rather than papered over: that pairing is
 * verified only on Android, by `app/src/test/.../PlatformModuleCheckTest.kt`,
 * which composes both modules and really instantiates every definition. The
 * iOS equivalent (`SharedModuleCheckTest` in `shared/src/iosTest`) was deleted
 * in Move 2 because it can no longer exist anywhere useful: a full-graph check
 * needs `firebaseDataModule`, `:shared` sits below `:data-firebase` and cannot
 * see it, and `:data-firebase` deliberately has no iOS test source set (an
 * `iosTest` there would drag GitLive's `-framework Firebase*` linker opts back
 * into a Kotlin/Native test executable and bring CocoaPods back with them).
 * So iOS's `platformModule` actual is exercised only by the app actually
 * launching, not by any test. Closing this would mean either a Kotlin/Native
 * mocking story for Obj-C statics (none exists) or an instrumented iOS test
 * target.
 */
val sharedModule = module {
    includes(
        coreInfraModule,
        authHomeModule,
        testTakingModule,
        gtoModule,
        interviewModule,
        profileSettingsModule,
        resultsSubmissionsModule,
        studyContentModule,
        premiumMarketplaceModule
    )
}
