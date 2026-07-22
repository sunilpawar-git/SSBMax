package com.ssbmax.shared.ui.sdt.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.sdt_action_exit
import ssbmax.shared.generated.resources.sdt_action_submit
import ssbmax.shared.generated.resources.sdt_cancel
import ssbmax.shared.generated.resources.sdt_exit_message
import ssbmax.shared.generated.resources.sdt_exit_title
import ssbmax.shared.generated.resources.sdt_submit_message
import ssbmax.shared.generated.resources.sdt_submit_title
import ssbmax.shared.generated.resources.sdt_submit_warning

/**
 * KMP port of the exit-confirmation `AlertDialog` inlined in
 * `app/.../ui/tests/sdt/SDTTestScreen.kt`'s `QuestionInProgressView`.
 * Extracted to its own composable, matching TAT/WAT/SRT/PPDT's
 * dialog-extraction precedent.
 */
@Composable
fun SDTExitDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sdt_exit_title)) },
        text = { Text(stringResource(Res.string.sdt_exit_message)) },
        confirmButton = {
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(Res.string.sdt_action_exit))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sdt_cancel)) } }
    )
}

/**
 * KMP port of the submit-confirmation `AlertDialog` inlined in
 * `app/.../ui/tests/sdt/SDTTestScreen.kt`. Warns when fewer than 4 valid
 * responses have been given (the Android original's `< 4` check, hardcoded
 * to SDT's fixed question count).
 */
@Composable
fun SDTSubmitDialog(
    validResponseCount: Int,
    totalQuestions: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sdt_submit_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.sdt_submit_message, validResponseCount, totalQuestions))
                if (validResponseCount < totalQuestions) {
                    Text(
                        stringResource(Res.string.sdt_submit_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.sdt_action_submit)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sdt_cancel)) } }
    )
}
