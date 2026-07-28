package com.ssbmax.di

import com.ssbmax.core.data.di.aiModule
import com.ssbmax.core.data.di.contentRepositoryModule
import com.ssbmax.core.data.di.coreDataInjectablesModule
import com.ssbmax.core.data.di.coroutineScopeModule
import com.ssbmax.core.data.di.databaseModule
import com.ssbmax.core.data.di.firebaseModule
import com.ssbmax.core.data.di.loggerModule
import com.ssbmax.core.data.di.repositoryModule
import com.ssbmax.shared.di.sharedModule

/**
 * All Koin modules the app needs, in one place for `startKoin()`
 * (see [com.ssbmax.SSBMaxApplication]). Mirrors the module set previously
 * wired via Hilt's `@InstallIn(SingletonComponent::class)` across
 * `core:data`'s 6 modules and `app`'s 11 modules — split here into
 * `core:data` modules (repositories/Room/Firebase/AI) and `app` modules
 * (use cases bridging :shared, platform singletons, ViewModels).
 *
 * `sharedModule` (the KMP `:shared` module's own Koin graph) is included
 * too — per this phase's scope, `app` still depends on both `:shared` and
 * `:core:data` side by side (not yet rewired to consume only `:shared`,
 * that's Phase 5 territory), so both graphs must be live at once.
 */
val appModules = listOf(
    // :shared (KMP)
    sharedModule,

    // :core:data
    databaseModule,
    repositoryModule,
    contentRepositoryModule,
    firebaseModule,
    loggerModule,
    coroutineScopeModule,
    aiModule,
    coreDataInjectablesModule,

    // :app
    authUseCaseModule,
    studyUseCaseModule,
    submissionUseCaseModule,
    subscriptionUseCaseModule,
    testUseCaseModule,
    clockModule,
    debugModule,
    gtoTestModule,
    imageModule,
    workManagerModule,
    appInjectablesModule,
    viewModelModule
)
