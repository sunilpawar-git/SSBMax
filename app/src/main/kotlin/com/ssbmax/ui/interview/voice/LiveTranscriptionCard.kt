package com.ssbmax.ui.interview.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * Live Transcription Card (Phase 2)
 *
 * Displays real-time speech-to-text transcription with:
 * - Status indicator (listening, processing, completed, error)
 * - Live partial transcription while recording
 * - Editable final transcription
 * - Color-coded based on state
 */
@Composable
fun LiveTranscriptionCard(
    state: TranscriptionState,
    liveTranscription: String,
    finalTranscription: String,
    transcriptionError: String?,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                TranscriptionState.LISTENING -> MaterialTheme.colorScheme.primaryContainer
                TranscriptionState.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                TranscriptionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TranscriptionStatusRow(state)

            if (transcriptionError != null) {
                Text(
                    text = transcriptionError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            TranscriptionTextField(
                state = state,
                liveTranscription = liveTranscription,
                finalTranscription = finalTranscription,
                onEdit = onEdit
            )
        }
    }
}

@Composable
private fun TranscriptionStatusRow(state: TranscriptionState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TranscriptionStateIcon(state)
        Text(
            text = when (state) {
                TranscriptionState.LISTENING -> stringResource(R.string.listening)
                TranscriptionState.PROCESSING -> stringResource(R.string.processing_speech)
                TranscriptionState.COMPLETED -> stringResource(R.string.transcription_complete)
                TranscriptionState.ERROR -> stringResource(R.string.transcription_error)
                else -> stringResource(R.string.ready_to_record)
            },
            style = MaterialTheme.typography.labelMedium,
            color = when (state) {
                TranscriptionState.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun TranscriptionStateIcon(state: TranscriptionState) {
    when (state) {
        TranscriptionState.LISTENING -> Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        TranscriptionState.PROCESSING -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        TranscriptionState.COMPLETED -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        TranscriptionState.ERROR -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        else -> Icon(
            imageVector = Icons.Default.MicNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TranscriptionTextField(
    state: TranscriptionState,
    liveTranscription: String,
    finalTranscription: String,
    onEdit: (String) -> Unit
) {
    val displayText = when (state) {
        TranscriptionState.LISTENING -> liveTranscription
        else -> finalTranscription
    }

    if (displayText.isNotBlank() || state == TranscriptionState.COMPLETED || state == TranscriptionState.ERROR) {
        OutlinedTextField(
            value = displayText,
            onValueChange = onEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.your_response)) },
            minLines = 4,
            maxLines = 8,
            enabled = state == TranscriptionState.COMPLETED || state == TranscriptionState.ERROR,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}




