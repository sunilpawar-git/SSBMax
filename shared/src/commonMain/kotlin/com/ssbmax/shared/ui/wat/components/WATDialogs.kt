package com.ssbmax.shared.ui.wat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.wat_continue_test
import ssbmax.shared.generated.resources.wat_exit
import ssbmax.shared.generated.resources.wat_exit_message_1
import ssbmax.shared.generated.resources.wat_exit_message_2
import ssbmax.shared.generated.resources.wat_exit_title

/**
 * KMP port of the exit-confirmation `AlertDialog` inlined in
 * `app/.../ui/tests/wat/components/WATInProgressView.kt`. Extracted to its
 * own composable, matching TAT/PPDT's dialog-extraction precedent (their
 * Android originals also inlined this dialog, ported the same way).
 *
 * WAT has no submit-confirmation dialog to port -- the Android original
 * auto-submits when the last word's response is saved
 * (`completeTest()` -> `submitTest()`, no user confirmation step), unlike
 * TAT's explicit `TATSubmitDialog`.
 */
@Composable
fun WATExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.wat_exit_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.wat_exit_message_1))
                Text(stringResource(Res.string.wat_exit_message_2))
            }
        },
        confirmButton = { TextButton(onClick = onExit) { Text(stringResource(Res.string.wat_exit)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.wat_continue_test)) } }
    )
}
