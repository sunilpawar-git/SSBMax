package com.ssbmax.ui.tests.oir.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * Top bar for OIR test screen showing question progress and countdown timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OIRTestTopBar(
    questionNumber: Int,
    totalQuestions: Int,
    timeRemainingSeconds: Int,
    onExitClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.oir_question_format, questionNumber, totalQuestions),
                    style = MaterialTheme.typography.titleMedium
                )
                OIRTimerChip(timeRemainingSeconds = timeRemainingSeconds)
            }
        },
        actions = {
            IconButton(onClick = onExitClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.oir_exit_test)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Countdown timer chip. Turns red when under 5 minutes remain.
 */
@Composable
internal fun OIRTimerChip(timeRemainingSeconds: Int) {
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val isLowTime = timeRemainingSeconds < 300

    AssistChip(
        onClick = { },
        label = {
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
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
            }
        )
    )
}
