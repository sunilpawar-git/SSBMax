package com.ssbmax.ui.tests.wat.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.R

/**
 * WAT In-Progress View
 * Timer is displayed in the static WATHeader (always visible at top)
 */

@Composable
fun WATInProgressView(
    word: String,
    wordNumber: Int,
    totalWords: Int,
    timeRemaining: Int,
    response: String,
    onResponseChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    showExitDialog: Boolean,
    onShowExitDialog: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background with gradient based on time remaining
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = when {
                timeRemaining <= 5 -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top bar with progress and exit - PINNED at top
                WATHeader(
                    wordNumber = wordNumber,
                    totalWords = totalWords,
                    timeRemaining = timeRemaining,
                    onShowExitDialog = onShowExitDialog
                )

                // Main Content - Scrollable
                WATActiveContent(
                    word = word,
                    response = response,
                    onResponseChange = onResponseChange,
                    onSubmit = onSubmit,
                    onSkip = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Take remaining space
                )
            }
        }
    }
    
    // Exit dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text(stringResource(R.string.wat_exit_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.wat_exit_message_1))
                    Text(stringResource(R.string.wat_exit_message_2))
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text(stringResource(R.string.wat_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) {
                    Text(stringResource(R.string.wat_continue_test))
                }
            }
        )
    }
}

@Composable
private fun WATHeader(
    wordNumber: Int,
    totalWords: Int,
    timeRemaining: Int,
    onShowExitDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onShowExitDialog) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.wat_exit))
        }

        Text(
            stringResource(R.string.wat_progress_format, wordNumber, totalWords),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Timer
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (timeRemaining <= 5) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Text(
                stringResource(R.string.wat_timer_format, timeRemaining),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun WATActiveContent(
    word: String,
    response: String,
    onResponseChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))
        
        // Word display (Large, centered)
        AnimatedContent(
            targetState = word,
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            },
            label = "word_animation"
        ) { currentWord ->
            Text(
                text = currentWord,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 36.sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        }
        
        // Response input
        OutlinedTextField(
            value = response,
            onValueChange = onResponseChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.wat_response_placeholder)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.wat_skip))
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                enabled = response.isNotBlank()
            ) {
                Text(stringResource(R.string.wat_submit))
            }
        }
        
        Spacer(Modifier.height(16.dp))
    }
}
