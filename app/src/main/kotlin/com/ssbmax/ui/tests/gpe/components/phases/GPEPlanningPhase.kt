package com.ssbmax.ui.tests.gpe.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import com.ssbmax.ui.components.TimerProgressBar
import com.ssbmax.ui.components.TimerThresholds

private const val GPE_PLANNING_TIME_SECONDS = 1740 // 29 minutes

/**
 * GPE Planning Phase
 * User writes their planning response (29 minutes)
 * 
 * Layout: TimerProgressBar fixed at bottom, always visible above keyboard
 */
@Composable
fun GPEPlanningPhase(
    planningResponse: String,
    onPlanningResponseChange: (String) -> Unit,
    charactersCount: Int,
    minCharacters: Int,
    maxCharacters: Int,
    timeRemainingSeconds: Int,
    scenario: String,
    resources: List<String>,
    imageUrl: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // Push content up when keyboard opens
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Map Image Card
            if (imageUrl.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp), // Reduced height for better keyboard visibility
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.gpe_test_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        error = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Filled.BrokenImage)
                    )
                }
            }

            // Scenario reminder card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.gpe_scenario_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = scenario,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    if (resources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.gpe_resources_available),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        resources.forEach { resource ->
                            Text(
                                text = "• $resource",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Planning response text field - compact height that expands
            OutlinedTextField(
                value = planningResponse,
                onValueChange = onPlanningResponseChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 350.dp), // Compact initial, expands with content
                label = { Text(stringResource(R.string.gpe_planning_response_label)) },
                placeholder = { Text(stringResource(R.string.gpe_planning_response_placeholder)) },
                supportingText = {
                    val remaining = maxCharacters - charactersCount
                    val color = when {
                        charactersCount < minCharacters -> MaterialTheme.colorScheme.error
                        remaining < 100 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = stringResource(
                            R.string.character_count_format,
                            charactersCount,
                            minCharacters,
                            maxCharacters
                        ),
                        color = color
                    )
                },
                isError = charactersCount > maxCharacters,
                maxLines = 15
            )

            // Guidelines card
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
                        text = stringResource(R.string.gpe_planning_guidelines),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.gpe_guideline_1),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.gpe_guideline_2),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.gpe_guideline_3),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Timer progress bar - FIXED at bottom, always visible above keyboard
        TimerProgressBar(
            timeRemainingSeconds = timeRemainingSeconds,
            totalTimeSeconds = GPE_PLANNING_TIME_SECONDS,
            lowTimeThresholdSeconds = TimerThresholds.LONG_TEST,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
