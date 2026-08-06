package com.ssbmax.shared.ui.gto.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gto_error_ok
import ssbmax.shared.generated.resources.gto_error_title
import ssbmax.shared.generated.resources.gto_exit_cancel
import ssbmax.shared.generated.resources.gto_exit_confirm
import ssbmax.shared.generated.resources.gto_exit_message
import ssbmax.shared.generated.resources.gto_exit_title

/**
 * Shared dialogs for GD/Lecturette (and future GPE/PGT/etc.) -- extracted
 * once instead of duplicated per-vertical, matching TAT/WAT/SRT's own
 * dialog-extraction precedent (a dedicated Dialogs.kt per vertical). Ported
 * from the inline `AlertDialog` blocks in the Android originals'
 * `app/.../ui/tests/gto/{gd,lecturette}` test screens.
 */
@Composable
fun GTOExitDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.gto_exit_title)) },
        text = { Text(stringResource(Res.string.gto_exit_message)) },
        confirmButton = { TextButton(onClick = onExit) { Text(stringResource(Res.string.gto_exit_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.gto_exit_cancel)) } }
    )
}

@Composable
fun GTOErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.gto_error_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.gto_error_ok)) } }
    )
}
