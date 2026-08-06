package com.ssbmax.shared.ui.piq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.PIQPage
import com.ssbmax.shared.presentation.piq.PIQTestViewModel
import com.ssbmax.shared.presentation.piq.PIQ_SELECTION_BOARD_OPTIONS
import com.ssbmax.shared.ui.common.TestLimitReachedDialog
import com.ssbmax.shared.ui.common.testErrorMessage
import com.ssbmax.shared.ui.piq.components.PIQNavigationBar
import com.ssbmax.shared.ui.piq.components.PIQPage1EducationFields
import com.ssbmax.shared.ui.piq.components.PIQPage1FamilyFields
import com.ssbmax.shared.ui.piq.components.PIQPage1PersonalFields
import com.ssbmax.shared.ui.piq.components.PIQPage2Fields
import com.ssbmax.shared.ui.piq.components.PIQPrivacyWarningBanner
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_back
import ssbmax.shared.generated.resources.piq_career_additional_title
import ssbmax.shared.generated.resources.piq_error
import ssbmax.shared.generated.resources.piq_next
import ssbmax.shared.generated.resources.piq_ok
import ssbmax.shared.generated.resources.piq_page_title
import ssbmax.shared.generated.resources.piq_personal_family_title
import ssbmax.shared.generated.resources.piq_review
import ssbmax.shared.generated.resources.piq_saved

/**
 * KMP port of `app/.../ui/tests/piq/PIQTestScreen.kt`. Uses
 * `koinViewModel<PIQTestViewModel>()` -- `viewModelScope` is cancelled
 * automatically in `onCleared`, no manual `DisposableEffect`/`close()` needed.
 *
 * Untimed 2-page form with free navigation, not a phase state machine --
 * [PIQTestViewModel.initialize] loads the OIR-number autofill and starts
 * autosave; [uiState.showReviewScreen] toggles to [PIQReviewScreen] instead of
 * a `phase` enum. Split into per-section field composables
 * ([PIQPage1PersonalFields]/[PIQPage1FamilyFields]/[PIQPage1EducationFields]/[PIQPage2Fields])
 * to keep this file and each of them under the 300-line limit -- the Android
 * original's single `PIQTestScreen.kt` is 845 lines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIQTestScreen(
    testId: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToResult: (String) -> Unit = {},
    viewModel: PIQTestViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(testId) {
        viewModel.initialize(testId)
    }

    LaunchedEffect(uiState.submissionComplete) {
        if (uiState.submissionComplete) {
            uiState.submissionId?.let { onNavigateToResult(it) } ?: onNavigateBack()
        }
    }

    if (uiState.isLimitReached) {
        TestLimitReachedDialog(
            tier = uiState.subscriptionTier,
            testsLimit = uiState.testsLimit,
            testsUsed = uiState.testsUsed,
            resetsAt = uiState.resetsAt,
            onUpgrade = { onNavigateBack() },
            onDismiss = onNavigateBack
        )
        return
    }

    if (uiState.showReviewScreen) {
        PIQReviewScreen(
            answers = uiState.answers,
            onEdit = { page -> viewModel.editPage(page) },
            onSubmit = { viewModel.submitTest() },
            onBack = { viewModel.navigateToPage(PIQPage.PAGE_2) }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.piq_page_title, uiState.currentPage.displayName)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.piq_back))
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    } else if (uiState.lastSavedAt != null) {
                        Text(
                            stringResource(Res.string.piq_saved),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            PIQNavigationBar(
                onPreviousPage = {
                    if (uiState.currentPage == PIQPage.PAGE_2) viewModel.navigateToPage(PIQPage.PAGE_1)
                },
                onNextPage = {
                    if (uiState.currentPage == PIQPage.PAGE_1) {
                        viewModel.navigateToPage(PIQPage.PAGE_2)
                    } else {
                        viewModel.goToReview()
                    }
                },
                canGoBack = uiState.currentPage == PIQPage.PAGE_2,
                nextButtonText = if (uiState.currentPage == PIQPage.PAGE_2) stringResource(Res.string.piq_review) else stringResource(Res.string.piq_next)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PIQPrivacyWarningBanner()
                when (uiState.currentPage) {
                    PIQPage.PAGE_1 -> {
                        Text(stringResource(Res.string.piq_personal_family_title), style = MaterialTheme.typography.headlineSmall)
                        PIQPage1PersonalFields(uiState.answers, PIQ_SELECTION_BOARD_OPTIONS, viewModel::updateField)
                        PIQPage1FamilyFields(uiState.answers, viewModel::updateField)
                        PIQPage1EducationFields(uiState.answers, viewModel::updateField)
                    }
                    PIQPage.PAGE_2 -> {
                        Text(stringResource(Res.string.piq_career_additional_title), style = MaterialTheme.typography.headlineSmall)
                        PIQPage2Fields(uiState.answers, viewModel::updateField)
                    }
                }
            }
        }

        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(stringResource(Res.string.piq_error)) },
                text = { Text(testErrorMessage(error)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(Res.string.piq_ok))
                    }
                }
            )
        }
    }
}
