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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * TAT Image Viewing Phase
 * Shows the image for 30 seconds (or blank slide for #12)
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
            .padding(16.dp),
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
                        "Viewing Picture $sequenceNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Observe carefully",
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Check if this is the 12th image (blank slide for imagination test)
            if (sequenceNumber == 12) {
                // Show blank white box for imagination test
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Blank Slide\n(Use your imagination)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Existing image loading code for regular TAT images (1-11)
                val context = LocalContext.current

                // CRITICAL FIX: Create stable ImageRequest with remember(imageUrl)
                // This ensures the request is ONLY recreated when imageUrl changes
                val imageRequest = remember(imageUrl) {
                    android.util.Log.d("TATTestScreen", "🔄 Creating NEW ImageRequest for: $imageUrl")
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .listener(
                            onStart = {
                                android.util.Log.d("TATTestScreen", "🖼️ Coil: Loading started")
                            },
                            onSuccess = { _, _ ->
                                android.util.Log.d("TATTestScreen", "✅ Coil: Image loaded successfully!")
                            },
                            onError = { _, result ->
                                android.util.Log.e("TATTestScreen", "❌ Coil: Load failed: ${result.throwable.message}", result.throwable)
                            }
                        )
                        .build()
                }

                // AsyncImage with stable model - won't restart on recomposition
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "TAT Picture $sequenceNumber",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Full-width timer progress bar (matches PPDT implementation)
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
