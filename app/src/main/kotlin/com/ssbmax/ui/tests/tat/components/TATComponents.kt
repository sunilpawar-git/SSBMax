package com.ssbmax.ui.tests.tat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.TATPhase

/**
 * TAT Shared UI Components
 * Extracted from TATTestScreen.kt to improve maintainability
 */

@Composable
fun TATBottomBar(
    phase: TATPhase,
    canMoveNext: Boolean,
    canMovePrevious: Boolean,
    canSubmit: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (canMovePrevious) {
                OutlinedButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Previous")
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (canSubmit) {
                Button(onClick = onSubmit) {
                    Text("Submit Test")
                }
            } else {
                // Show Next button always, but disabled if criteria not met (e.g. min characters)
                Button(
                    onClick = onNext,
                    enabled = canMoveNext
                ) {
                    Text("Next")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}
