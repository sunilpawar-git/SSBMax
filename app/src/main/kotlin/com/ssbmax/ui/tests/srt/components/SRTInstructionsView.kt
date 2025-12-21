package com.ssbmax.ui.tests.srt.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * SRT Test Instructions Screen
 * Displays test overview, instructions, tips, and start button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SRTInstructionsView(
    onStart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.srt_test)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SRTIntroCard() }
            item { SRTInstructionsCard() }
            item { SRTTipsCard() }
            
            item {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_start_test))
                }
            }
        }
    }
}

@Composable
private fun SRTIntroCard() {
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
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Text(
                stringResource(R.string.srt_full_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.srt_subtitle),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SRTInstructionsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.srt_instructions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            InstructionItem(
                icon = Icons.Default.FormatListNumbered,
                text = stringResource(R.string.srt_instruction_1)
            )
            InstructionItem(
                icon = Icons.Default.Edit,
                text = stringResource(R.string.srt_instruction_2)
            )
            InstructionItem(
                icon = Icons.Default.Timer,
                text = stringResource(R.string.srt_instruction_3)
            )
            InstructionItem(
                icon = Icons.Default.CheckCircle,
                text = stringResource(R.string.srt_instruction_4)
            )
        }
    }
}

@Composable
private fun SRTTipsCard() {
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
                    stringResource(R.string.srt_tips_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(stringResource(R.string.srt_tip_1))
            Text(stringResource(R.string.srt_tip_2))
            Text(stringResource(R.string.srt_tip_3))
            Text(stringResource(R.string.srt_tip_4))
            Text(stringResource(R.string.srt_tip_5))
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
