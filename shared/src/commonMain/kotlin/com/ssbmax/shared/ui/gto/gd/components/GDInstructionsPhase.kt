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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gd_back_cd
import ssbmax.shared.generated.resources.gd_instruction_chars_desc
import ssbmax.shared.generated.resources.gd_instruction_chars_title
import ssbmax.shared.generated.resources.gd_instruction_noise_desc
import ssbmax.shared.generated.resources.gd_instruction_noise_title
import ssbmax.shared.generated.resources.gd_instruction_time_desc
import ssbmax.shared.generated.resources.gd_instruction_time_title
import ssbmax.shared.generated.resources.gd_instruction_topic_desc
import ssbmax.shared.generated.resources.gd_instruction_topic_title
import ssbmax.shared.generated.resources.gd_instructions_headline
import ssbmax.shared.generated.resources.gd_noise_warning
import ssbmax.shared.generated.resources.gd_start_test
import ssbmax.shared.generated.resources.gd_title

/**
 * KMP port of `app/.../ui/tests/gto/gd/GDTestPhases.kt`'s `InstructionsPhase`
 * + `InstructionItem`, unchanged behavior/layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GDInstructionsPhase(onStart: () -> Unit, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.gd_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.gd_back_cd))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Groups, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(Res.string.gd_instructions_headline), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InstructionItem(Icons.Default.Topic, stringResource(Res.string.gd_instruction_topic_title), stringResource(Res.string.gd_instruction_topic_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    InstructionItem(Icons.Default.Timer, stringResource(Res.string.gd_instruction_time_title), stringResource(Res.string.gd_instruction_time_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    InstructionItem(Icons.Default.Edit, stringResource(Res.string.gd_instruction_chars_title), stringResource(Res.string.gd_instruction_chars_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    InstructionItem(Icons.AutoMirrored.Filled.VolumeUp, stringResource(Res.string.gd_instruction_noise_title), stringResource(Res.string.gd_instruction_noise_desc))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(Res.string.gd_noise_warning), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(Res.string.gd_start_test), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
internal fun InstructionItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
