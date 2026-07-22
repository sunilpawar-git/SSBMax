package com.ssbmax.shared.ui.gto.gd.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gd_back_cd
import ssbmax.shared.generated.resources.gd_review_title
import ssbmax.shared.generated.resources.gd_submit_test
import ssbmax.shared.generated.resources.gd_topic_label
import ssbmax.shared.generated.resources.gd_your_response
import ssbmax.shared.generated.resources.gto_char_count_label

/**
 * KMP port of `app/.../ui/tests/gto/gd/GDTestPhases.kt`'s `ReviewPhase`,
 * unchanged behavior/layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GDReviewPhase(
    topic: String,
    response: String,
    charCount: Int,
    isSubmitting: Boolean,
    onBackToDiscussion: () -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.gd_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackToDiscussion, enabled = !isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.gd_back_cd))
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth(), enabled = !isSubmitting) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(stringResource(Res.string.gd_submit_test))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(Res.string.gd_topic_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(topic, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(Res.string.gd_your_response), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(Res.string.gto_char_count_label, charCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(response, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
