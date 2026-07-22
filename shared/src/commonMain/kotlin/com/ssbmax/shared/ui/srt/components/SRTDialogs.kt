package com.ssbmax.shared.ui.srt.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.srt_cancel
import ssbmax.shared.generated.resources.srt_continue
import ssbmax.shared.generated.resources.srt_exit
import ssbmax.shared.generated.resources.srt_exit_message
import ssbmax.shared.generated.resources.srt_exit_title
import ssbmax.shared.generated.resources.srt_submit
import ssbmax.shared.generated.resources.srt_submit_incomplete
import ssbmax.shared.generated.resources.srt_submit_message
import ssbmax.shared.generated.resources.srt_submit_title
import ssbmax.shared.generated.resources.srt_time_up_message
import ssbmax.shared.generated.resources.srt_time_up_title

/**
 * KMP port of the exit-confirmation `AlertDialog` inlined in
 * `app/.../ui/tests/srt/components/SRTInProgressView.kt`. Extracted to its
 * own composable, matching TAT/WAT/PPDT's dialog-extraction precedent.
 */
@Composable
fun SRTExitDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.srt_exit_title)) },
        text = { Text(stringResource(Res.string.srt_exit_message)) },
        confirmButton = { TextButton(onClick = onExit) { Text(stringResource(Res.string.srt_exit)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.srt_continue)) } }
    )
}

/**
 * KMP port of the submit-confirmation `AlertDialog` inlined in
 * `app/.../ui/tests/srt/SRTTestScreen.kt`. Unlike WAT (auto-submits, no
 * confirmation step), SRT's review phase ends with an explicit confirmation
 * that also warns about any pending (unattempted) situations.
 */
@Composable
fun SRTSubmitDialog(
    validResponseCount: Int,
    totalSituations: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.srt_submit_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.srt_submit_message, validResponseCount, totalSituations))
                if (validResponseCount < totalSituations) {
                    val pendingCount = totalSituations - validResponseCount
                    Text(
                        stringResource(Res.string.srt_submit_incomplete, pendingCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.srt_submit)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.srt_cancel)) } }
    )
}

/**
 * KMP port of the non-dismissible "Time's Up" `AlertDialog` inlined in
 * `app/.../ui/tests/srt/SRTTestScreen.kt`. Real product behavior, not
 * incidental: `dismissOnBackPress`/`dismissOnClickOutside` are both false so
 * the candidate cannot escape it during the 2s auto-submit grace period
 * ([com.ssbmax.shared.presentation.srt.SRTTestViewModel]'s `startTimer`).
 */
@Composable
fun SRTTimeUpDialog() {
    AlertDialog(
        onDismissRequest = { /* no-op: not dismissible */ },
        title = { Text(text = stringResource(Res.string.srt_time_up_title), color = MaterialTheme.colorScheme.error) },
        text = { Text(stringResource(Res.string.srt_time_up_message)) },
        confirmButton = {},
        dismissButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}
