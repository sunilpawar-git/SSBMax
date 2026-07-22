package com.ssbmax.shared.ui.tat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.TATPhase
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.tat_button_next
import ssbmax.shared.generated.resources.tat_button_submit_test

/**
 * KMP port of `app/.../ui/tests/tat/components/TATComponents.kt`, unchanged
 * behavior. Bottom bar only -- the Android original's top bar is inlined
 * directly into `TATTestScreen.kt` (kept the same way here, matching
 * `PPDTTestScreen`'s own top-bar-inlined-in-screen precedent).
 */
@Composable
fun TATBottomBar(
    phase: TATPhase,
    canMoveNext: Boolean,
    canSubmit: Boolean,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (canSubmit) {
                Button(onClick = onSubmit) {
                    Text(stringResource(Res.string.tat_button_submit_test))
                }
            } else {
                Button(onClick = onNext, enabled = canMoveNext) {
                    Text(stringResource(Res.string.tat_button_next))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
