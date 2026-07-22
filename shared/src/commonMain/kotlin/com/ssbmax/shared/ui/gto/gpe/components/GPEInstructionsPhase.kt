package com.ssbmax.shared.ui.gto.gpe.components

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
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.ui.gto.gd.components.InstructionItem
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gpe_back_cd
import ssbmax.shared.generated.resources.gpe_instruction_response_desc
import ssbmax.shared.generated.resources.gpe_instruction_response_title
import ssbmax.shared.generated.resources.gpe_instruction_review_desc
import ssbmax.shared.generated.resources.gpe_instruction_review_title
import ssbmax.shared.generated.resources.gpe_instruction_scenario_desc
import ssbmax.shared.generated.resources.gpe_instruction_scenario_title
import ssbmax.shared.generated.resources.gpe_instruction_time_desc
import ssbmax.shared.generated.resources.gpe_instruction_time_title
import ssbmax.shared.generated.resources.gpe_instructions_headline
import ssbmax.shared.generated.resources.gpe_instructions_subtitle
import ssbmax.shared.generated.resources.gpe_start_test
import ssbmax.shared.generated.resources.gpe_tip
import ssbmax.shared.generated.resources.gpe_title

/**
 * KMP port of `app/.../ui/tests/gpe/components/phases/GPEInstructionsPhase.kt`.
 * Reuses [com.ssbmax.shared.ui.gto.gd.components.InstructionItem] and the
 * Scaffold-per-phase shape established by
 * [com.ssbmax.shared.ui.gto.lecturette.components.LecturetteInstructionsPhase],
 * rather than the Android original's single-Scaffold-at-screen-level +
 * `GPETopBar`/`GPEBottomBar` (see [com.ssbmax.shared.ui.gto.gpe.GPETestScreen]'s
 * doc comment for why).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPEInstructionsPhase(onStart: () -> Unit, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.gpe_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.gpe_back_cd))
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
            Text(stringResource(Res.string.gpe_instructions_headline), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(Res.string.gpe_instructions_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InstructionItem(Icons.AutoMirrored.Filled.ListAlt, stringResource(Res.string.gpe_instruction_scenario_title), stringResource(Res.string.gpe_instruction_scenario_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    InstructionItem(Icons.Default.Timer, stringResource(Res.string.gpe_instruction_time_title), stringResource(Res.string.gpe_instruction_time_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    InstructionItem(Icons.Default.Edit, stringResource(Res.string.gpe_instruction_response_title), stringResource(Res.string.gpe_instruction_response_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    InstructionItem(Icons.Default.Info, stringResource(Res.string.gpe_instruction_review_title), stringResource(Res.string.gpe_instruction_review_desc))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(Res.string.gpe_tip), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(Res.string.gpe_start_test), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
