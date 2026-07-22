package com.ssbmax.shared.ui.components.result

import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.domain.validation.SSBRecommendationUIModel

/**
 * KMP port of `app/.../ui/components/result/UnifiedResultUiState.kt`,
 * unchanged. Common contract every OLQ-based test result ViewModel's UiState
 * implements so it can drive [UnifiedOLQResultTemplate]. PPDT is the first
 * KMP consumer (this session); the Android original is shared by PPDT, TAT,
 * WAT, SRT, SDT -- this port keeps that same shape ready for when those
 * verticals are ported.
 */
interface UnifiedResultUiState {
    val isLoading: Boolean
    val error: String?
    val analysisStatus: AnalysisStatus
    val olqResult: OLQAnalysisResult?
    val ssbRecommendation: SSBRecommendationUIModel?

    val isAnalyzing: Boolean
        get() = analysisStatus == AnalysisStatus.ANALYZING

    val isCompleted: Boolean
        get() = analysisStatus == AnalysisStatus.COMPLETED

    val isFailed: Boolean
        get() = analysisStatus == AnalysisStatus.FAILED

    val isPending: Boolean
        get() = analysisStatus == AnalysisStatus.PENDING_ANALYSIS

    val hasError: Boolean
        get() = error != null

    val hasResults: Boolean
        get() = olqResult != null

    val showResults: Boolean
        get() = isCompleted && hasResults
}
