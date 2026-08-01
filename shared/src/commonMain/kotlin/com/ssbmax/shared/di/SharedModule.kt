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
val sharedModule = module {
    includes(
        coreInfraModule,
        repositoryModule,
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
