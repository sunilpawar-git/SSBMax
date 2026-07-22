package com.ssbmax.shared.ui.tat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.tat_cancel
import ssbmax.shared.generated.resources.tat_exit_confirm
import ssbmax.shared.generated.resources.tat_exit_message
import ssbmax.shared.generated.resources.tat_exit_title
import ssbmax.shared.generated.resources.tat_profile_go_to_settings
import ssbmax.shared.generated.resources.tat_profile_required_cancel
import ssbmax.shared.generated.resources.tat_profile_required_message
import ssbmax.shared.generated.resources.tat_profile_required_title
import ssbmax.shared.generated.resources.tat_submit
import ssbmax.shared.generated.resources.tat_submit_incomplete_message
import ssbmax.shared.generated.resources.tat_submit_progress
import ssbmax.shared.generated.resources.tat_submit_title

/**
 * KMP port of `app/.../ui/tests/tat/components/TATDialogs.kt`. The Android
 * original hardcoded these dialog strings -- ported here as real
 * composeResources entries instead of carrying the violation forward, same
 * as every other ported dialog this phase (PPDT's `PPDTDialogs.kt` set the
 * precedent).
 */
@Composable
fun TATProfileRequiredDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.tat_profile_required_title)) },
        text = { Text(stringResource(Res.string.tat_profile_required_message)) },
        confirmButton = { Button(onClick = onGoToSettings) { Text(stringResource(Res.string.tat_profile_go_to_settings)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.tat_profile_required_cancel)) } }
    )
}

@Composable
fun TATExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.tat_exit_title)) },
        text = { Text(stringResource(Res.string.tat_exit_message)) },
        confirmButton = { TextButton(onClick = onExit) { Text(stringResource(Res.string.tat_exit_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.tat_cancel)) } }
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
        title = { Text(stringResource(Res.string.tat_submit_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.tat_submit_progress, completedStories, totalStories))
                if (completedStories < totalStories) {
                    Text(
                        stringResource(Res.string.tat_submit_incomplete_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onSubmit) { Text(stringResource(Res.string.tat_submit)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.tat_cancel)) } }
    )
}
