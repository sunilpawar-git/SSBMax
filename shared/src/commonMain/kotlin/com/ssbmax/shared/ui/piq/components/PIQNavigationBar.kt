package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_previous

/**
 * KMP port of `app/.../ui/tests/piq/components/PIQNavigationBar.kt`.
 */
@Composable
fun PIQNavigationBar(
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    canGoBack: Boolean,
    nextButtonText: String
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (canGoBack) {
                OutlinedButton(onClick = onPreviousPage) {
                    Text(stringResource(Res.string.piq_previous))
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(onClick = onNextPage) {
                Text(nextButtonText)
            }
        }
    }
}
