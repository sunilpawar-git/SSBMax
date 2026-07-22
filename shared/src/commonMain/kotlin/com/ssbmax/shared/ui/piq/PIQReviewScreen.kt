package com.ssbmax.shared.ui.piq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.PIQPage
import com.ssbmax.shared.ui.piq.components.PIQReviewCareerSection
import com.ssbmax.shared.ui.piq.components.PIQReviewPersonalSection
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_action_submit
import ssbmax.shared.generated.resources.piq_back
import ssbmax.shared.generated.resources.piq_cancel
import ssbmax.shared.generated.resources.piq_edit_cd
import ssbmax.shared.generated.resources.piq_review_action_submit
import ssbmax.shared.generated.resources.piq_review_dialog_message
import ssbmax.shared.generated.resources.piq_review_dialog_title
import ssbmax.shared.generated.resources.piq_review_section_career
import ssbmax.shared.generated.resources.piq_review_section_personal_family
import ssbmax.shared.generated.resources.piq_review_title

/**
 * KMP port of `app/.../ui/tests/piq/PIQReviewScreen.kt`. Delegates the two
 * page summaries to [PIQReviewPersonalSection]/[PIQReviewCareerSection]
 * (split out to respect the 300-line limit -- the Android original's single
 * file is 337 lines with both inlined).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIQReviewScreen(
    answers: Map<String, String>,
    onEdit: (PIQPage) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    var showSubmitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.piq_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.piq_back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, tonalElevation = 3.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { showSubmitDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.piq_review_action_submit))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReviewSection(title = stringResource(Res.string.piq_review_section_personal_family), onEdit = { onEdit(PIQPage.PAGE_1) }) {
                PIQReviewPersonalSection(answers)
            }
            ReviewSection(title = stringResource(Res.string.piq_review_section_career), onEdit = { onEdit(PIQPage.PAGE_2) }) {
                PIQReviewCareerSection(answers)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showSubmitDialog) {
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                title = { Text(stringResource(Res.string.piq_review_dialog_title)) },
                text = { Text(stringResource(Res.string.piq_review_dialog_message)) },
                confirmButton = {
                    Button(onClick = { showSubmitDialog = false; onSubmit() }) {
                        Text(stringResource(Res.string.piq_action_submit))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubmitDialog = false }) {
                        Text(stringResource(Res.string.piq_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ReviewSection(
    title: String,
    onEdit: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(Res.string.piq_edit_cd))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
