package com.ssbmax.ui.home.student.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * Empty state when no tests completed
 * Shows a friendly prompt to start the SSB journey
 * 
 * @param modifier Modifier for the card
 * @param onStartTestClick Callback when user taps "Start First Test" button
 */
@Composable
fun EmptyDashboardState(
    modifier: Modifier = Modifier,
    onStartTestClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_empty_emoji),
                style = MaterialTheme.typography.displayLarge
            )
            
            Text(
                text = stringResource(R.string.dashboard_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.dashboard_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onStartTestClick) {
                Text(stringResource(R.string.dashboard_empty_button))
            }
        }
    }
}

