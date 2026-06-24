package com.ssbmax.ui.tests.tat.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * TAT Review Phase
 * Review current story before moving to next picture
 */
@Composable
fun TATReviewPhase(
    story: String,
    charactersCount: Int,
    sequenceNumber: Int,
    isLastQuestion: Boolean,
    onEdit: () -> Unit,
    onConfirm: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.tat_review_story_title, sequenceNumber),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.tat_review_character_count, charactersCount),
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider()
                    Text(
                        story,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.edit))
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (isLastQuestion) R.string.button_submit_test else R.string.dialog_confirm
                        )
                    )
                }
            }
        }
    }
}
