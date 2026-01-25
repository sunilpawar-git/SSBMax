package com.ssbmax.ui.tests.ppdt.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.ui.components.TimerProgressBar
import com.ssbmax.ui.components.TimerThresholds

/**
 * PPDT Writing Phase
 * Story composition with character count tracking
 * 
 * Layout: TimerProgressBar fixed at bottom, always visible above keyboard
 */
private const val PPDT_WRITING_TIME_SECONDS = 240 // 4 minutes

@Composable
fun PPDTWritingPhase(
    story: String,
    onStoryChange: (String) -> Unit,
    charactersCount: Int,
    minCharacters: Int,
    maxCharacters: Int,
    timeRemainingSeconds: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // Push content up when keyboard opens
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Write your story based on the image you saw",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Include: What led to this? What's happening? What will be the outcome?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = story,
                onValueChange = {
                    if (it.length <= maxCharacters) {
                        onStoryChange(it)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 400.dp), // Compact initial, expands with content
                placeholder = { Text("Start writing your story here...") },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Minimum: $minCharacters characters",
                            color = if (charactersCount >= minCharacters) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = "$charactersCount / $maxCharacters",
                            color = when {
                                charactersCount >= maxCharacters -> MaterialTheme.colorScheme.error
                                charactersCount >= minCharacters -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                minLines = 5,
                maxLines = 15
            )
        }

        // Timer progress bar - FIXED at bottom, always visible above keyboard
        TimerProgressBar(
            timeRemainingSeconds = timeRemainingSeconds,
            totalTimeSeconds = PPDT_WRITING_TIME_SECONDS,
            lowTimeThresholdSeconds = TimerThresholds.STANDARD_TEST,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)
        )
    }
}
