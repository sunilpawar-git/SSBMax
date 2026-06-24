package com.ssbmax.ui.tests.ppdt.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * PPDT Test Dialogs
 * Extracted from PPDTTestScreen.kt to improve maintainability
 */

@Composable
fun PPDTProfileRequiredDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ppdt_profile_required_title)) },
        text = { Text(stringResource(R.string.ppdt_profile_required_message)) },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text(stringResource(R.string.ppdt_profile_go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ppdt_profile_required_cancel))
            }
        }
    )
}

@Composable
fun PPDTExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit Test?") },
        text = { Text("Your progress will be saved. You can continue later from where you left off.") },
        confirmButton = {
            TextButton(onClick = onExit) {
                Text("Exit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue Test")
            }
        }
    )
}

@Composable
fun PPDTSubmitDialog(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Test?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Once submitted, you cannot make changes.")
                Text(
                    "Your story will be reviewed by your instructor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
