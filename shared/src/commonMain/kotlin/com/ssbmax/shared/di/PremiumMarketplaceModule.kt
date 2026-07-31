package com.ssbmax.shared.di

import com.ssbmax.shared.presentation.marketplace.MarketplaceViewModel
import com.ssbmax.shared.presentation.premium.UpgradeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Premium Upgrade + Marketplace vertical. Split out of the former monolithic
 * `SharedModule.kt` — see [repositoryModule]'s doc comment for why.
 *
 * [UpgradeViewModel] is the KMP port of the Android LIVE upgrade screen
 * (`com.ssbmax.ui.premium`, route "premium/upgrade") — reuses
 * [profileSettingsModule]'s already-bound `GetSubscriptionTierUseCase`
 * instead of duplicating the Android original's inline `UserProfileRepository`
 * + `SubscriptionType` mapping (see [UpgradeViewModel]'s own class doc). The
 * Android app's sibling `com.ssbmax.ui.upgrade`/`com.ssbmax.ui.payment`
 * packages are dead code (zero nav call sites, confirmed by grep + git log)
 * and were deliberately not ported or bound here.
 *
 * [MarketplaceViewModel] has no repository dependency — backed by
 * `com.ssbmax.shared.presentation.marketplace.MarketplaceMockData` on both
 * platforms, same as the Android original (no `MarketplaceRepository` exists
 * on either side).
 */
val premiumMarketplaceModule = module {
    viewModelOf(::UpgradeViewModel)
    viewModelOf(::MarketplaceViewModel)
}
