package com.ssbmax.ui.tests.ppdt.components.phases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * PPDT Writing Phase
 * Story composition with character count tracking
 * 
 * Timer is displayed in the static TopAppBar header (TimerChip)
 * Layout: Scrollable column that allows content to scroll behind header when keyboard appears
 */

@Composable
fun PPDTWritingPhase(
    story: String,
    onStoryChange: (String) -> Unit,
    charactersCount: Int,
    minCharacters: Int,
    maxCharacters: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Make entire column scrollable
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .imePadding(), // Handle keyboard insets at the end
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Instructions card - will scroll up behind header when keyboard appears
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

        // Text field - fills remaining space efficiently
        OutlinedTextField(
            value = story,
            onValueChange = {
                if (it.length <= maxCharacters) {
                    onStoryChange(it)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 300.dp), // Larger minimum height
            placeholder = { Text("Start writing your story here...") },
            maxLines = Int.MAX_VALUE, // Allow multi-line expansion
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
            }
        )
        
        // Bottom spacer for better scroll behavior
        Spacer(modifier = Modifier.height(8.dp))
    }
}
