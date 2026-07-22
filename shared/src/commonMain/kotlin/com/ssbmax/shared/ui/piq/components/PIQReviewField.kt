package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared label/value row used by [PIQReviewPersonalSection] and
 * [PIQReviewCareerSection] -- ports the Android original's private
 * `PIQReviewScreen.ReviewField()`, promoted to internal-visible so both
 * section files (split out to respect the 300-line limit) can use it.
 */
@Composable
fun ReviewField(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
