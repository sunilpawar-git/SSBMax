package com.ssbmax.shared.ui.tat.components.phases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.tat_edit
import ssbmax.shared.generated.resources.tat_review_character_count
import ssbmax.shared.generated.resources.tat_review_story_title
import ssbmax.shared.generated.resources.tat_button_confirm
import ssbmax.shared.generated.resources.tat_button_submit_test

/**
 * KMP port of `app/.../ui/tests/tat/components/phases/TATReviewPhase.kt`,
 * unchanged behavior. TAT-specific difference from PPDT's single review
 * screen: this phase repeats once per story (12x), and its confirm button
 * label switches to "Submit Test" only on the last story.
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
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(Res.string.tat_review_story_title, sequenceNumber),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )
                    Text(stringResource(Res.string.tat_review_character_count, charactersCount), style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Text(story, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.tat_edit))
                }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (isLastQuestion) Res.string.tat_button_submit_test else Res.string.tat_button_confirm))
                }
            }
        }
    }
}
