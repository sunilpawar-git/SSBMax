package com.ssbmax.ui.tests.tat.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * TAT Writing Phase
 * Story composition with character count tracking and timer
 */
@Composable
fun TATWritingPhase(
    story: String,
    onStoryChange: (String) -> Unit,
    timeRemaining: Int,
    minCharacters: Int,
    maxCharacters: Int,
    charactersCount: Int,
    sequenceNumber: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show blank slide reminder for 12th picture
        if (sequenceNumber == 12) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Text(
                            "Blank Slide (Picture 12/12)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Use your imagination to create a story. There was no picture shown - write what you visualize in your mind.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (sequenceNumber == 12) "Write Your Imagination" else "Write Your Story",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$charactersCount / $maxCharacters characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            charactersCount < minCharacters -> MaterialTheme.colorScheme.error
                            charactersCount > maxCharacters -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, null)
                    Text(
                        "${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (timeRemaining <= 60) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = story,
            onValueChange = onStoryChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Write a complete story with characters, situation, action, and outcome...") },
            supportingText = {
                Text("Include: Who, What, Why, How the story unfolds, and What happens in the end")
            },
            isError = charactersCount > maxCharacters
        )

        // Full-width timer progress bar (matches PPDT implementation)
        LinearProgressIndicator(
            progress = { timeRemaining.toFloat() / 240f },
            modifier = Modifier.fillMaxWidth(),
            color = if (timeRemaining < 60) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}
