package com.ssbmax.ui.tests.tat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * TAT Test Dialogs
 * Extracted from TATTestScreen.kt to improve maintainability
 */

@Composable
fun TATExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit Test?") },
        text = { Text("Your progress will be lost. Are you sure you want to exit?") },
        confirmButton = {
            TextButton(onClick = onExit) {
                Text("Exit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TATSubmitDialog(
    completedStories: Int,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Test?") },
        text = {
            Column {
                Text("You have completed $completedStories/12 stories.")
                if (completedStories < 12) {
                    Text(
                        "You can submit now or complete remaining stories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
