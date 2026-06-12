package com.ssbmax.ui.tests.oir.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

/**
 * Bottom navigation bar for OIR test: Previous / progress indicator / Next or Submit.
 */
@Composable
internal fun OIRTestBottomBar(
    currentIndex: Int,
    totalQuestions: Int,
    isAnswered: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentIndex > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.oir_previous))
            }

            Text(
                text = stringResource(R.string.oir_progress_format, currentIndex + 1, totalQuestions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (currentIndex < totalQuestions - 1) {
                Button(
                    onClick = onNext,
                    enabled = isAnswered
                ) {
                    Text(stringResource(R.string.oir_next))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Button(
                    onClick = onSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.oir_submit_test))
                }
            }
        }
    }
}
