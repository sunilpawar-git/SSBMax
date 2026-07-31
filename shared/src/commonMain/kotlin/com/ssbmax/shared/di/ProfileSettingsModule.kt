package com.ssbmax.shared.di

import com.ssbmax.shared.domain.usecase.subscription.GetMonthlyUsageUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.presentation.profile.StudentProfileViewModel
import com.ssbmax.shared.presentation.profile.UserProfileViewModel
import com.ssbmax.shared.presentation.settings.SettingsViewModel
import com.ssbmax.shared.presentation.settings.SubscriptionManagementViewModel
import com.ssbmax.shared.presentation.settings.notifications.NotificationSettingsViewModel
import com.ssbmax.shared.presentation.settings.theme.ThemeSettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Profile + Settings vertical. `UserProfileRepository`/`AuthRepository`/
 * `TestProgressRepository`/`NotificationRepository` and
 * `ObserveCurrentUserUseCase` come from [repositoryModule]/[authHomeModule].
 * [AppThemeSettings] is bound in `platformModule` (Phase 4 platform-shims
 * item), not here. Split out of the former monolithic `SharedModule.kt` —
 * see [repositoryModule]'s doc comment for why.
 *
 * [GetSubscriptionTierUseCase]/[GetMonthlyUsageUseCase] existed in
 * `core:domain`/`shared` before Phase 5's Settings session but were never
 * bound in this Koin graph until then. None of these screens is swapped into
 * the live Android app's nav graph — Settings/Profile were judged less
 * monetization-adjacent than the test verticals but still deferred pending
 * their own review pass.
 */
val profileSettingsModule = module {
    viewModelOf(::UserProfileViewModel)
    viewModelOf(::StudentProfileViewModel)

    factoryOf(::GetSubscriptionTierUseCase)
    factoryOf(::GetMonthlyUsageUseCase)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ThemeSettingsViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::SubscriptionManagementViewModel)
}
