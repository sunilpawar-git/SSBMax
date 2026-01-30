package com.ssbmax.ui.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel
import com.ssbmax.ui.components.SSBRecommendationBanner

/**
 * Unified template for all OLQ-based test result screens.
 *
 * This template provides a consistent layout and behavior across all result screens:
 * - Loading state handling
 * - Analysis status display (pending, analyzing, failed, completed)
 * - SSB Recommendation banner
 * - Overall score card
 * - Strengths, OLQ scores by category, weaknesses, recommendations
 * - Instructor review status
 * - Action buttons
 *
 * @param T The type of UI state, must implement UnifiedResultUiState
 * @param uiState The current UI state
 * @param testTitle The title to display in the top bar
 * @param submissionConfirmationContent Slot for test-specific submission confirmation card
 * @param testSpecificContent Optional slot for additional test-specific content
 * @param submissionStatus The submission status for showing instructor review card
 * @param showInstructorReview Whether to show the instructor review pending card
 * @param onNavigateHome Callback when user wants to navigate back to home
 * @param onRetry Optional callback for retry action when analysis fails
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : UnifiedResultUiState> UnifiedOLQResultTemplate(
    uiState: T,
    testTitle: String,
    submissionConfirmationContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    testSpecificContent: (@Composable (T) -> Unit)? = null,
    submissionStatus: SubmissionStatus? = null,
    showInstructorReview: Boolean = true,
    onNavigateHome: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(testTitle) })
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingContent(paddingValues = paddingValues)
            }

            uiState.hasError -> {
                ErrorContent(
                    errorMessage = uiState.error ?: stringResource(R.string.result_analysis_failed_description),
                    paddingValues = paddingValues,
                    onNavigateHome = onNavigateHome
                )
            }

            else -> {
                ResultContent(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    submissionConfirmationContent = submissionConfirmationContent,
                    testSpecificContent = testSpecificContent,
                    submissionStatus = submissionStatus,
                    showInstructorReview = showInstructorReview,
                    onNavigateHome = onNavigateHome,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    paddingValues: PaddingValues,
    onNavigateHome: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AnalysisFailedCard() }
        item { ResultActionButtons(primaryAction = onNavigateHome) }
    }
}

@Composable
private fun <T : UnifiedResultUiState> ResultContent(
    uiState: T,
    paddingValues: PaddingValues,
    submissionConfirmationContent: @Composable (T) -> Unit,
    testSpecificContent: (@Composable (T) -> Unit)?,
    submissionStatus: SubmissionStatus?,
    showInstructorReview: Boolean,
    onNavigateHome: () -> Unit,
    onRetry: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Submission confirmation card (test-specific)
        item { submissionConfirmationContent(uiState) }

        // Analysis status or results
        analysisStatusSection(uiState, onRetry)

        // Test-specific content slot
        testSpecificContent?.let {
            item { it(uiState) }
        }

        // Instructor review status
        if (showInstructorReview && submissionStatus == SubmissionStatus.SUBMITTED_PENDING_REVIEW) {
            item { InstructorReviewPendingCard() }
        }

        // Action buttons
        item { ResultActionButtons(primaryAction = onNavigateHome) }
    }
}

/**
 * Extension function to add analysis status section to LazyListScope.
 */
private fun <T : UnifiedResultUiState> LazyListScope.analysisStatusSection(
    uiState: T,
    onRetry: (() -> Unit)?
) {
    when {
        uiState.isPending -> {
            item { PendingAnalysisCard() }
        }

        uiState.isAnalyzing -> {
            item { AnalyzingCard() }
        }

        uiState.isFailed -> {
            item { AnalysisFailedCard() }
        }

        uiState.showResults -> {
            val result = uiState.olqResult!!
            olqResultsSection(result, uiState.ssbRecommendation)
        }
    }
}

/**
 * Extension function to add complete OLQ results to LazyListScope.
 */
private fun LazyListScope.olqResultsSection(
    result: OLQAnalysisResult,
    ssbRecommendation: SSBRecommendationUIModel?
) {
    // SSB Recommendation Banner
    ssbRecommendation?.let { recommendation ->
        item { SSBRecommendationBanner(model = recommendation) }
    }

    // Overall Score Card
    item {
        OverallScoreCard(
            overallScore = result.overallScore,
            overallRating = result.overallRating,
            aiConfidence = result.aiConfidence
        )
    }

    // Strengths Section
    strengthsSection(result.strengths)

    // OLQ Scores by Category
    olqCategorySection(result.olqScores)

    // Weaknesses Section
    weaknessesSection(result.weaknesses)

    // Recommendations Section
    recommendationsSection(result.recommendations)
}
