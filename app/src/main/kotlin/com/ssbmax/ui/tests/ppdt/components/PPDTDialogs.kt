package com.ssbmax.ui.tests.ppdt.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * PPDT Test Dialogs
 * Extracted from PPDTTestScreen.kt to improve maintainability
 */

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
