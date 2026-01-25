package com.ssbmax.ui.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * Standard action buttons for result screens.
 *
 * @param primaryAction Callback for the primary button (typically navigate home)
 * @param primaryLabel Label for the primary button, defaults to "Back to Home"
 * @param secondaryAction Optional callback for secondary button
 * @param secondaryLabel Label for secondary button, required if secondaryAction is provided
 */
@Composable
fun ResultActionButtons(
    primaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    primaryLabel: String = stringResource(R.string.result_back_to_home),
    secondaryAction: (() -> Unit)? = null,
    secondaryLabel: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (secondaryAction != null && secondaryLabel != null) {
            OutlinedButton(
                onClick = secondaryAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = secondaryLabel)
            }
        }

        Button(
            onClick = primaryAction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = primaryLabel)
        }
    }
}
