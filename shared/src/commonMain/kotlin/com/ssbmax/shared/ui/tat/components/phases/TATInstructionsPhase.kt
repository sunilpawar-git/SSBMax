package com.ssbmax.shared.ui.tat.components.phases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.tat_instruction_1
import ssbmax.shared.generated.resources.tat_instruction_2
import ssbmax.shared.generated.resources.tat_instruction_3
import ssbmax.shared.generated.resources.tat_instruction_4
import ssbmax.shared.generated.resources.tat_instruction_5
import ssbmax.shared.generated.resources.tat_instructions_subtitle
import ssbmax.shared.generated.resources.tat_instructions_title
import ssbmax.shared.generated.resources.tat_instructions_section_title
import ssbmax.shared.generated.resources.tat_start_test_button
import ssbmax.shared.generated.resources.tat_tips_title
import ssbmax.shared.generated.resources.tat_tip_1
import ssbmax.shared.generated.resources.tat_tip_2
import ssbmax.shared.generated.resources.tat_tip_3
import ssbmax.shared.generated.resources.tat_tip_4

/**
 * KMP port of `app/.../ui/tests/tat/components/phases/TATInstructionsPhase.kt`.
 * The Android original hardcoded every instruction/tip string -- ported here
 * as real composeResources entries, same as PPDT's own instructions phase.
 */
@Composable
fun TATInstructionsPhase(onStart: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(32.dp))
                    Text(stringResource(Res.string.tat_instructions_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.tat_instructions_subtitle), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.tat_instructions_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    InstructionItem(icon = Icons.Default.Image, text = stringResource(Res.string.tat_instruction_1))
                    InstructionItem(icon = Icons.Default.Timer, text = stringResource(Res.string.tat_instruction_2))
                    InstructionItem(icon = Icons.Default.Edit, text = stringResource(Res.string.tat_instruction_3))
                    InstructionItem(icon = Icons.Default.Description, text = stringResource(Res.string.tat_instruction_4))
                    InstructionItem(icon = Icons.Default.Psychology, text = stringResource(Res.string.tat_instruction_5))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Text(stringResource(Res.string.tat_tips_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Text(stringResource(Res.string.tat_tip_1))
                    Text(stringResource(Res.string.tat_tip_2))
                    Text(stringResource(Res.string.tat_tip_3))
                    Text(stringResource(Res.string.tat_tip_4))
                }
            }
        }

        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.tat_start_test_button))
            }
        }
    }
}

@Composable
private fun InstructionItem(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
