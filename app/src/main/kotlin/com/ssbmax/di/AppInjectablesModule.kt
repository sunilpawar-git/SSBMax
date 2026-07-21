package com.ssbmax.di

import com.ssbmax.admin.AdminContentManager
import com.ssbmax.billing.BillingRepository
import com.ssbmax.billing.SSBMaxBillingClient
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.ui.tests.gto.common.GTOSequentialAccessManager
import com.ssbmax.ui.tests.gto.common.GTOWhiteNoisePlayer
import com.ssbmax.ui.tests.tat.TATAnalysisPipelineOrchestrator
import com.ssbmax.ui.tests.tat.TATAnalysisWorkPlanner
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * App-layer plain classes that Hilt previously auto-wired from their
 * `@Inject`-constructor without needing an explicit `@Provides`/`@Binds`.
 * Koin has no implicit constructor scanning, so each needs one explicit
 * binding here instead. All were `@Singleton`-scoped in Hilt.
 */
val appInjectablesModule = module {
    singleOf(::AdminContentManager)
    singleOf(::BillingRepository)
    singleOf(::SSBMaxBillingClient)
    singleOf(::NotificationHelper)
    singleOf(::GTOSequentialAccessManager)
    singleOf(::GTOWhiteNoisePlayer)
    singleOf(::TATAnalysisPipelineOrchestrator)
    singleOf(::TATAnalysisWorkPlanner)
}
