package com.ssbmax.shared.ui.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.result_back_to_home

/**
 * KMP port of `app/.../ui/components/result/ResultActionButtons.kt`, unchanged.
 */
@Composable
fun ResultActionButtons(
    primaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    primaryLabel: String = stringResource(Res.string.result_back_to_home),
    secondaryAction: (() -> Unit)? = null,
    secondaryLabel: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (secondaryAction != null && secondaryLabel != null) {
            OutlinedButton(onClick = secondaryAction, modifier = Modifier.fillMaxWidth()) {
                Text(text = secondaryLabel)
            }
        }
        Button(onClick = primaryAction, modifier = Modifier.fillMaxWidth()) {
            Text(text = primaryLabel)
        }
    }
}
