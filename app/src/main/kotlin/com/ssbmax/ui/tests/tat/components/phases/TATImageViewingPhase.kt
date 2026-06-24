package com.ssbmax.ui.tests.tat.components.phases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ssbmax.R

/**
 * TAT Image Viewing Phase
 * Shows the image for 30 seconds (or blank slide for #12)
 *
 * Fixed: Image card uses aspectRatio(4f/3f) instead of weight(1f) to prevent blank space bands.
 * ContentScale.Crop fills card without letterboxing.
 */
@Composable
fun TATImageViewingPhase(
    imageUrl: String,
    timeRemaining: Int,
    sequenceNumber: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("tat_image_viewing_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.tat_image_number, sequenceNumber, 12),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.tat_observe_carefully),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, null)
                    Text(
                        "${timeRemaining}s",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (timeRemaining <= 10) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }

        // Image card with fixed aspectRatio(4f/3f) — prevents blank space bands
        if (sequenceNumber == 12) {
            TATBlankSlideCard()
        } else {
            TATImageCard(imageUrl = imageUrl, sequenceNumber = sequenceNumber)
        }

        Spacer(Modifier.weight(1f))

        // Full-width timer progress bar at bottom
        LinearProgressIndicator(
            progress = { timeRemaining / 30f },
            modifier = Modifier.fillMaxWidth(),
            color = if (timeRemaining < 10) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

/**
 * TAT Image Card — displays picture with fixed 4:3 aspect ratio
 * ContentScale.Crop fills card without letterboxing (blank bands)
 */
@Composable
private fun TATImageCard(imageUrl: String, sequenceNumber: Int) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .testTag("tat_image_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.tat_picture_content_description, sequenceNumber),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * TAT Blank Slide Card (Picture #12) — imagination test
 */
@Composable
private fun TATBlankSlideCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .testTag("tat_blank_slide_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.tat_blank_slide_instruction),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}
