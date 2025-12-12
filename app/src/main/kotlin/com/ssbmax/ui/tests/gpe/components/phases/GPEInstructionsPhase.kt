package com.ssbmax.ui.tests.gpe.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * GPE Instructions Phase
 * Shows test instructions before starting
 */
@Composable
fun GPEInstructionsPhase(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.gpe_instructions_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.gpe_instruction_1),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.gpe_instruction_2),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.gpe_instruction_3),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.gpe_instruction_4),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

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
                Text(
                    text = stringResource(R.string.test_format),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.gpe_format_viewing),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.gpe_format_planning),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.start_test))
        }
    }
}
