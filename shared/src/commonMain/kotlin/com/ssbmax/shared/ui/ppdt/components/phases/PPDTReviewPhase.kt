package com.ssbmax.shared.ui.ppdt.components.phases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import ssbmax.shared.generated.resources.ppdt_review_character_count
import ssbmax.shared.generated.resources.ppdt_review_edit_button
import ssbmax.shared.generated.resources.ppdt_review_story_label
import ssbmax.shared.generated.resources.ppdt_review_title

/**
 * KMP port of `app/.../ui/tests/ppdt/components/phases/PPDTReviewPhase.kt`,
 * unchanged behavior.
 */
@Composable
fun PPDTReviewPhase(
    imageUrl: String,
    story: String,
    charactersCount: Int,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(Res.string.ppdt_review_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Text(text = stringResource(Res.string.ppdt_review_story_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = story, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(Res.string.ppdt_review_character_count, charactersCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.ppdt_review_edit_button))
        }
    }
}
