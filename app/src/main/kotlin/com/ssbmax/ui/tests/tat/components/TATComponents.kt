package com.ssbmax.ui.tests.tat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.TATPhase

/**
 * TAT Shared UI Components
 * Extracted from TATTestScreen.kt to improve maintainability
 */

@Composable
fun TATBottomBar(
    phase: TATPhase,
    canMoveNext: Boolean,
    canSubmit: Boolean,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (canSubmit) {
                Button(onClick = onSubmit) {
                    Text(stringResource(R.string.button_submit_test))
                }
            } else {
                Button(
                    onClick = onNext,
                    enabled = canMoveNext
                ) {
                    Text(stringResource(R.string.button_next))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}
