package com.ssbmax.shared.ui.ppdt.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.ppdt_exit_confirm
import ssbmax.shared.generated.resources.ppdt_exit_continue
import ssbmax.shared.generated.resources.ppdt_exit_message
import ssbmax.shared.generated.resources.ppdt_exit_title
import ssbmax.shared.generated.resources.ppdt_profile_go_to_settings
import ssbmax.shared.generated.resources.ppdt_profile_required_cancel
import ssbmax.shared.generated.resources.ppdt_profile_required_message
import ssbmax.shared.generated.resources.ppdt_profile_required_title
import ssbmax.shared.generated.resources.ppdt_submit_cancel
import ssbmax.shared.generated.resources.ppdt_submit_confirm
import ssbmax.shared.generated.resources.ppdt_submit_message
import ssbmax.shared.generated.resources.ppdt_submit_note
import ssbmax.shared.generated.resources.ppdt_submit_title

/**
 * KMP port of `app/.../ui/tests/ppdt/components/PPDTDialogs.kt`. The Android
 * original hardcoded the exit/submit dialog strings (a pre-existing "zero
 * hardcoded strings" lint-rule violation) -- ported here as real
 * composeResources entries instead of carrying the violation forward, same
 * as every other ported dialog this phase.
 */
@Composable
fun PPDTProfileRequiredDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.ppdt_profile_required_title)) },
        text = { Text(stringResource(Res.string.ppdt_profile_required_message)) },
        confirmButton = { Button(onClick = onGoToSettings) { Text(stringResource(Res.string.ppdt_profile_go_to_settings)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ppdt_profile_required_cancel)) } }
    )
}

@Composable
fun PPDTExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.ppdt_exit_title)) },
        text = { Text(stringResource(Res.string.ppdt_exit_message)) },
        confirmButton = { TextButton(onClick = onExit) { Text(stringResource(Res.string.ppdt_exit_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ppdt_exit_continue)) } }
    )
}

@Composable
fun PPDTSubmitDialog(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.ppdt_submit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.ppdt_submit_message))
                Text(
                    stringResource(Res.string.ppdt_submit_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = onSubmit) { Text(stringResource(Res.string.ppdt_submit_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ppdt_submit_cancel)) } }
    )
}
