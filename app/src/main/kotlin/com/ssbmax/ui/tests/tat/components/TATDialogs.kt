package com.ssbmax.ui.tests.tat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * TAT Test Dialogs
 * Extracted from TATTestScreen.kt to improve maintainability
 */

@Composable
fun TATProfileRequiredDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tat_profile_required_title)) },
        text = { Text(stringResource(R.string.tat_profile_required_message)) },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text(stringResource(R.string.tat_profile_go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tat_profile_required_cancel))
            }
        }
    )
}

@Composable
fun TATExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tat_exit_title)) },
        text = { Text(stringResource(R.string.tat_exit_message)) },
        confirmButton = {
            TextButton(onClick = onExit) {
                Text(stringResource(R.string.tat_exit_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun TATSubmitDialog(
    completedStories: Int,
    totalStories: Int,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tat_submit_title)) },
        text = {
            Column {
                Text(stringResource(R.string.tat_submit_progress, completedStories, totalStories))
                if (completedStories < totalStories) {
                    Text(
                        stringResource(R.string.tat_submit_incomplete_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text(stringResource(R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
