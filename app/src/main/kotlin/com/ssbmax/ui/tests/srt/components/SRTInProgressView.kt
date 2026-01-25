package com.ssbmax.ui.tests.srt.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * SRT Test In-Progress Screen
 * Timer is displayed in the static SRTHeader (always visible at top)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SRTInProgressView(
    situation: String,
    situationNumber: Int,
    totalSituations: Int,
    timeRemaining: Int,
    response: String,
    onResponseChange: (String) -> Unit,
    minChars: Int,
    maxChars: Int,
    canMoveNext: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    showExitDialog: Boolean,
    onShowExitDialog: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit
) {
    // Background with gradient based on time remaining/urgency
    val backgroundColor = if (timeRemaining <= 60) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SRTHeader(
                situationNumber = situationNumber,
                totalSituations = totalSituations,
                timeRemaining = timeRemaining,
                onShowExitDialog = onShowExitDialog
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()) // Allow scrolling to reach buttons
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                SRTSituationCard(situation = situation)
                
                SRTResponseInput(
                    response = response,
                    onResponseChange = onResponseChange,
                    minChars = minChars,
                    maxChars = maxChars
                )
                
                // Buttons moved here, immediately below input as requested
                SRTButtons(
                    canMoveNext = canMoveNext,
                    onNext = onNext,
                    onSkip = onSkip
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Exit dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text(stringResource(R.string.srt_exit_title)) },
            text = { Text(stringResource(R.string.srt_exit_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text(stringResource(R.string.srt_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) {
                    Text(stringResource(R.string.srt_continue))
                }
            }
        )
    }
}

@Composable
private fun SRTButtons(
    canMoveNext: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.srt_skip))
        }
        
        Button(
            onClick = onNext,
            enabled = canMoveNext,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.srt_next))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SRTHeader(
    situationNumber: Int,
    totalSituations: Int,
    timeRemaining: Int,
    onShowExitDialog: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.srt_situation_number, situationNumber, totalSituations),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onShowExitDialog) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
            }
        },
        actions = {
            // Timer Display
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (timeRemaining <= 60) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = formatTime(timeRemaining),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (timeRemaining <= 60) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
        }
    )
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@Composable
private fun SRTSituationCard(situation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.srt_situation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                situation,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SRTResponseInput(
    response: String,
    onResponseChange: (String) -> Unit,
    minChars: Int,
    maxChars: Int
) {
    OutlinedTextField(
        value = response,
        onValueChange = onResponseChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.srt_your_response)) },
        placeholder = { Text(stringResource(R.string.srt_response_placeholder)) },
        supportingText = {
            Text(
                stringResource(R.string.srt_char_count, response.length, maxChars),
                color = when {
                    response.length < minChars -> MaterialTheme.colorScheme.error
                    response.length > maxChars -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        isError = response.length > maxChars,
        maxLines = 8
    )
}
