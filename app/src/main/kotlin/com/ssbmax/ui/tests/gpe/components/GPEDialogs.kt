package com.ssbmax.ui.tests.gpe.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * GPE Test Dialogs
 */

@Composable
fun GPEExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_test_title)) },
        text = { Text(stringResource(R.string.exit_test_message_save_progress)) },
        confirmButton = {
            TextButton(onClick = onExit) {
                Text(stringResource(R.string.exit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.continue_test))
            }
        }
    )
}

@Composable
fun GPESubmitDialog(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.submit_test_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.submit_test_warning))
                Text(
                    stringResource(R.string.gpe_submit_review_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
