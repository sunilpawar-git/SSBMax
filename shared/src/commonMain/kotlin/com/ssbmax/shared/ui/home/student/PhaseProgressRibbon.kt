package com.ssbmax.shared.ui.home.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.Phase1Progress
import com.ssbmax.shared.domain.model.Phase2Progress
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.icon_trophy
import ssbmax.shared.generated.resources.phase_progress_title

/**
 * Phase Progress Ribbon Component — Phase 5 KMP port of the Android
 * original (`app/.../ui/home/student/PhaseProgressRibbon.kt`).
 * Two-column layout showing Phase 1 and Phase 2 progress.
 *
 * [Phase1Card]/[Phase2Card]/`TestProgressItem` live in `PhaseCards.kt`
 * (same package) — split out purely to stay under this repo's 300-line
 * Quality Limit, no behavior change.
 */
@Composable
fun PhaseProgressRibbon(
    phase1Progress: Phase1Progress?,
    phase2Progress: Phase2Progress?,
    onPhaseClick: (TestPhase) -> Unit,
    onTopicClick: (String) -> Unit, // Navigate to topic screen with Tests tab
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.cardCornerRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.icon_trophy),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(Res.string.phase_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                Phase1Card(
                    progress = phase1Progress,
                    onPhaseClick = { onPhaseClick(TestPhase.PHASE_1) },
                    onTopicClick = onTopicClick,
                    modifier = Modifier.weight(1f)
                )

                Phase2Card(
                    progress = phase2Progress,
                    onPhaseClick = { onPhaseClick(TestPhase.PHASE_2) },
                    onTopicClick = onTopicClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
