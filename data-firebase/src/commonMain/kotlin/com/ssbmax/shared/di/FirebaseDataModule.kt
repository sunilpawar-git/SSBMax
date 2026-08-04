package com.ssbmax.shared.di

import org.koin.dsl.module

/**
 * The Firebase-backed half of the DI graph: everything `sharedModule` declares
 * an interface for but deliberately no longer binds.
 *
 * Move 2 of the iOS CocoaPods->SPM convergence extracted these bindings (and
 * the ~8,100 lines of `GitLive*` implementations behind them) out of `:shared`
 * so that `:shared`'s klib graph carries no Firebase. That is what lets
 * `:shared:iosSimulatorArm64Test` link without CocoaPods -- see
 * `data-firebase/build.gradle.kts` for the verified linker facts.
 *
 * Pairs with [sharedModule]: neither is a complete graph alone. Every
 * composition root loads both -- `app`'s `KoinModules.kt` and iOS's
 * `ensureKoinStarted()`. Same shape as `iosObservabilityModule` from Move 1,
 * where Swift likewise supplies what Kotlin cannot construct.
 */
val firebaseDataModule = module {
    includes(repositoryModule)
}
