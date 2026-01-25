package com.ssbmax.ui.tests.ppdt.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.PPDTPhase

/**
 * PPDT Shared UI Components
 * Extracted from PPDTTestScreen.kt to improve maintainability
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPDTTopBar(
    currentPhase: PPDTPhase,
    timeRemainingSeconds: Int,
    onExitClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "PPDT Test",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currentPhase.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            if (currentPhase in listOf(PPDTPhase.IMAGE_VIEWING, PPDTPhase.WRITING)) {
                TimerChip(timeRemainingSeconds = timeRemainingSeconds)
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = onExitClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Test"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun TimerChip(timeRemainingSeconds: Int) {
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val isLowTime = timeRemainingSeconds < 30

    AssistChip(
        onClick = { },
        label = {
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontWeight = FontWeight.Bold
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isLowTime) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
            labelColor = if (isLowTime) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            },
            leadingIconContentColor = if (isLowTime) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            }
        )
    )
}

@Composable
fun PPDTBottomBar(
    currentPhase: PPDTPhase,
    canProceed: Boolean,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            when (currentPhase) {
                PPDTPhase.IMAGE_VIEWING -> {
                    // Auto-proceeds, no button needed
                }
                PPDTPhase.WRITING -> {
                    Button(
                        onClick = onNext,
                        enabled = canProceed
                    ) {
                        Text("Review")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                PPDTPhase.REVIEW -> {
                    Button(
                        onClick = onSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Test")
                    }
                }
                else -> {}
            }
        }
    }
}
