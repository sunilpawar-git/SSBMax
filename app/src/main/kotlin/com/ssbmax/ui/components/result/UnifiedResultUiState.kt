package com.ssbmax.ui.components.result

import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel

/**
 * Unified interface for OLQ-based test result UI states.
 *
 * This interface defines the common contract that all test result ViewModels
 * must implement to work with the unified result template.
 *
 * Implementing classes should expose these properties via StateFlow for proper
 * Compose state observation.
 */
interface UnifiedResultUiState {
    /** Whether the result data is currently being loaded */
    val isLoading: Boolean

    /** Error message if loading or analysis failed, null otherwise */
    val error: String?

    /** Current status of the OLQ analysis */
    val analysisStatus: AnalysisStatus

    /** OLQ analysis results when completed, null otherwise */
    val olqResult: OLQAnalysisResult?

    /** SSB recommendation based on OLQ scores, null if not computed */
    val ssbRecommendation: SSBRecommendationUIModel?

    // Computed properties with default implementations

    /** True when analysis is currently in progress */
    val isAnalyzing: Boolean
        get() = analysisStatus == AnalysisStatus.ANALYZING

    /** True when analysis has completed successfully */
    val isCompleted: Boolean
        get() = analysisStatus == AnalysisStatus.COMPLETED

    /** True when analysis has failed */
    val isFailed: Boolean
        get() = analysisStatus == AnalysisStatus.FAILED

    /** True when analysis is pending (not yet started) */
    val isPending: Boolean
        get() = analysisStatus == AnalysisStatus.PENDING_ANALYSIS

    /** True when there is an error to display */
    val hasError: Boolean
        get() = error != null

    /** True when OLQ results are available */
    val hasResults: Boolean
        get() = olqResult != null

    /** True when results should be displayed (completed with results) */
    val showResults: Boolean
        get() = isCompleted && hasResults
}
