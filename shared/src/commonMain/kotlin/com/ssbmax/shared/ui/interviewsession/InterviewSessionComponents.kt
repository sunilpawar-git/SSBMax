package com.ssbmax.shared.ui.interviewsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.interview_audio_muted
import ssbmax.shared.generated.resources.interview_button_complete
import ssbmax.shared.generated.resources.interview_button_submit
import ssbmax.shared.generated.resources.interview_cd_speaking
import ssbmax.shared.generated.resources.interview_cd_unmute_tts
import ssbmax.shared.generated.resources.interview_char_count
import ssbmax.shared.generated.resources.interview_interviewer_speaking
import ssbmax.shared.generated.resources.interview_question_label
import ssbmax.shared.generated.resources.interview_response_hint
import ssbmax.shared.generated.resources.interview_response_placeholder

/**
 * Sub-components for [InterviewSessionScreen], split out to keep both files
 * under this plan's 300-line Quality Limit.
 */
@Composable
internal fun QuestionCard(questionText: String, isSpeaking: Boolean, isMuted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(Res.string.interview_question_label), style = MaterialTheme.typography.labelMedium)
                if (isMuted) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = stringResource(Res.string.interview_cd_unmute_tts),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.interview_audio_muted),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (isSpeaking) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(Res.string.interview_cd_speaking),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(Res.string.interview_interviewer_speaking),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(text = questionText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun ResponseInputCard(responseText: String, onResponseChange: (String) -> Unit, enabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.interview_response_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = responseText,
                onValueChange = onResponseChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
                enabled = enabled,
                placeholder = { Text(stringResource(Res.string.interview_response_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                maxLines = 10,
                supportingText = {
                    Text(text = stringResource(Res.string.interview_char_count, responseText.length), style = MaterialTheme.typography.labelSmall)
                }
            )
        }
    }
}

@Composable
internal fun SubmitButton(isSubmitting: Boolean, canSubmit: Boolean, hasMoreQuestions: Boolean, onSubmit: () -> Unit) {
    Button(onClick = onSubmit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
        if (isSubmitting) {
            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(stringResource(if (hasMoreQuestions) Res.string.interview_button_submit else Res.string.interview_button_complete))
        }
    }
}
