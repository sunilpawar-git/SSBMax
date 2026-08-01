package com.ssbmax.di

import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.workers.TATAnalysisPipelineOrchestrator
import com.ssbmax.workers.TATAnalysisWorkPlanner
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * App-layer plain classes that Hilt previously auto-wired from their
 * `@Inject`-constructor without needing an explicit `@Provides`/`@Binds`.
 * Koin has no implicit constructor scanning, so each needs one explicit
 * binding here instead. All were `@Singleton`-scoped in Hilt.
 *
 * KMP-convergence Phase 6a: `AdminContentManager`/`BillingRepository`/
 * `GTOSequentialAccessManager` bindings removed -- each was only ever
 * consumed by `app/ui` screens/ViewModels, deleted this phase (confirmed by
 * grep against the surviving `app/workers`/`app/notifications` before
 * removing). [TATAnalysisPipelineOrchestrator]/[TATAnalysisWorkPlanner] stay
 * bound -- `app/workers/WorkManagerSubmissionAnalysisTrigger` genuinely
 * needs them (moved from `app/ui/tests/tat/` alongside this trim, see their
 * own class docs).
 */
val appInjectablesModule = module {
    singleOf(::NotificationHelper)
    // WhiteNoisePlayer: provided by shared's platformModule (Phase 4 shim).
    singleOf(::TATAnalysisPipelineOrchestrator)
    singleOf(::TATAnalysisWorkPlanner)
}
