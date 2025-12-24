package com.ssbmax.ui.tests.tat.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * TAT Instructions Phase
 * Shows test instructions before starting
 */
@Composable
fun TATInstructionsPhase(onStart: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "TAT - Thematic Apperception Test",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Write creative stories based on ambiguous pictures",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    InstructionItem(
                        icon = Icons.Default.Image,
                        text = "You will see 12 pictures, one at a time"
                    )
                    InstructionItem(
                        icon = Icons.Default.Timer,
                        text = "View each picture for 30 seconds"
                    )
                    InstructionItem(
                        icon = Icons.Default.Edit,
                        text = "Write a story for each picture in 4 minutes"
                    )
                    InstructionItem(
                        icon = Icons.Default.Description,
                        text = "Write 50-1500 characters per story"
                    )
                    InstructionItem(
                        icon = Icons.Default.Psychology,
                        text = "Be creative, positive, and describe characters"
                    )
                }
            }
        }

        item {
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
                        Icon(Icons.Default.Lightbulb, null)
                        Text(
                            "Tips for Success",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("• Create complete stories with beginning, middle, and end")
                    Text("• Show leadership qualities and positive outlook")
                    Text("• Describe characters' emotions and motivations")
                    Text("• Focus on action and resolution")
                }
            }
        }

        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Test")
            }
        }
    }
}

@Composable
private fun InstructionItem(
    icon: ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
