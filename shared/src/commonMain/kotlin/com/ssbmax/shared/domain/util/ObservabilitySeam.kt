package com.ssbmax.shared.domain.util

/**
 * Bundles [logger] and [analyticsTracker] into one constructor parameter.
 * Used only by the handful of ViewModels (SDT/SRT/WAT) where adding
 * [AnalyticsTracker] as a 10th separate parameter crossed detekt's
 * `LongParameterList` threshold (Phase 7a) — every other ViewModel keeps
 * the two as plain flat parameters, same precedent as this plan's Phase 3b
 * fixing a `LongMethod` overage by extraction rather than a baseline
 * suppression.
 */
class ObservabilitySeam(
    val logger: DomainLogger,
    val analyticsTracker: AnalyticsTracker
)
