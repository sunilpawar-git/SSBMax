package com.ssbmax.shared.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.not_yet_ported_body
import ssbmax.shared.generated.resources.not_yet_ported_title

/**
 * Honest placeholder for a destination [SSBMaxNavHost] must route to but
 * whose real screen hasn't been ported into `commonMain/ui` yet (59 of 61
 * screens, as of this phase — see the migration plan's Phase 5 scope).
 *
 * Exists so the commonMain nav graph is genuinely navigable end-to-end
 * (Splash -> Login/RoleSelection -> Home) on both Android and iOS today,
 * without pretending unported destinations are done. NOT a silent no-op —
 * it visibly tells whoever lands here that the real screen is still on the
 * Android-only `app` nav graph, matching this plan's "fail loud, don't hide
 * skipped work" rule.
 */
@Composable
fun NotYetPortedScreen(screenName: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.not_yet_ported_title, screenName),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.not_yet_ported_body),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
