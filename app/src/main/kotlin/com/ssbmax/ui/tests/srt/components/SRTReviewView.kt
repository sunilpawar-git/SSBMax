package com.ssbmax.ui.tests.srt.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.SRTSituationResponse

/**
 * SRT Test Review Screen
 * Displays all responses for review before final submission
 */
@Composable
fun SRTReviewView(
    responses: List<SRTSituationResponse>,
    totalSituations: Int,
    onEdit: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.srt_submit_test))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SRTReviewHeader(
                    validResponseCount = responses.filter { it.isValidResponse }.size,
                    totalSituations = totalSituations
                )
            }
            
            items(responses.size) { index ->
                val response = responses[index]
                ResponseReviewCard(
                    number = index + 1,
                    situation = response.situation,
                    response = response.response,
                    isSkipped = response.isSkipped,
                    onEdit = { onEdit(index) }
                )
            }
        }
    }
}

@Composable
private fun SRTReviewHeader(
    validResponseCount: Int,
    totalSituations: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                stringResource(R.string.srt_review_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.srt_review_completed, validResponseCount, totalSituations)
            )
        }
    }
}

@Composable
private fun ResponseReviewCard(
    number: Int,
    situation: String,
    response: String,
    isSkipped: Boolean,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.srt_situation_prefix, number),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (isSkipped) {
                    AssistChip(
                        onClick = { },
                        label = { Text(stringResource(R.string.srt_skipped)) },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
            }
            
            Text(
                situation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!isSkipped) {
                HorizontalDivider()
                Text(
                    response,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.srt_edit))
            }
        }
    }
}
