package com.ssbmax.core.data.di

import com.ssbmax.core.data.analytics.AnalyticsManager
import com.ssbmax.core.data.remote.CommonSubmissionRepository
import com.ssbmax.core.data.remote.FirebaseAuthService
import com.ssbmax.core.data.remote.FirebaseInitializer
import com.ssbmax.core.data.remote.FirestoreUserRepository
import com.ssbmax.core.data.remote.GTOSubmissionRepository
import com.ssbmax.core.data.remote.OIRPersonalSubmissionDataSource
import com.ssbmax.core.data.remote.PIQPersonalSubmissionDataSource
import com.ssbmax.core.data.remote.PPDTPersonalSubmissionDataSource
import com.ssbmax.core.data.remote.PersonalTestSubmissionRepository
import com.ssbmax.core.data.remote.PsychTestSubmissionRepository
import com.ssbmax.core.data.remote.SDTSubmissionRepository
import com.ssbmax.core.data.remote.SRTSubmissionRepository
import com.ssbmax.core.data.remote.SubmissionArchiveRepository
import com.ssbmax.core.data.remote.TATSubmissionRepository
import com.ssbmax.core.data.remote.WATSubmissionRepository
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Plain `core:data` classes that Hilt previously auto-wired from their
 * `@Inject`-constructor without needing an explicit `@Provides`/`@Binds`
 * (mostly Firestore-backed repositories/data sources and Room-cache
 * managers with no separate domain interface). Koin has no implicit
 * constructor scanning, so each needs one explicit binding here instead.
 * All were `@Singleton`-scoped in Hilt (either directly or via the
 * `@Binds`/`@Provides` call site that consumed them).
 */
val coreDataInjectablesModule = module {
    singleOf(::AnalyticsManager)
    singleOf(::CommonSubmissionRepository)
    singleOf(::DifficultyProgressionManager)
    singleOf(::FirebaseAuthService)
    singleOf(::FirebaseInitializer)
    singleOf(::FirestoreUserRepository)
    singleOf(::GTOSubmissionRepository)
    singleOf(::OIRPersonalSubmissionDataSource)
    singleOf(::PIQPersonalSubmissionDataSource)
    singleOf(::PPDTPersonalSubmissionDataSource)
    singleOf(::PersonalTestSubmissionRepository)
    singleOf(::PsychTestSubmissionRepository)
    singleOf(::SDTSubmissionRepository)
    singleOf(::SRTSubmissionRepository)
    singleOf(::SubmissionArchiveRepository)
    singleOf(::TATSubmissionRepository)
    singleOf(::WATSubmissionRepository)
}
