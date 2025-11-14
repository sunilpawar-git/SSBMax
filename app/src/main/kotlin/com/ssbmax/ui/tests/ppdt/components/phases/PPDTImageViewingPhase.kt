package com.ssbmax.ui.tests.ppdt.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * PPDT Image Viewing Phase
 * Shows the hazy image for 30 seconds
 */
@Composable
fun PPDTImageViewingPhase(
    imageUrl: String,
    timeRemainingSeconds: Int
) {
    // CRITICAL DEBUG: Log every time this composable is called
    android.util.Log.d("PPDTTestScreen", "🎨 ImageViewingPhase recomposed with imageUrl: $imageUrl")
    android.util.Log.d("PPDTTestScreen", "🎨 imageUrl length: ${imageUrl.length}, isEmpty: ${imageUrl.isEmpty()}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "Observe the image carefully. Try to identify characters, their mood, and what's happening.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Show imageUrl in UI for debugging
                if (imageUrl.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚠️ IMAGE URL IS EMPTY",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check ViewModel logs",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    val context = LocalContext.current

                    // Log the URL being loaded (only when URL changes)
                    LaunchedEffect(imageUrl) {
                        android.util.Log.d("PPDTTestScreen", "🖼️ NEW URL SET: $imageUrl")
                    }

                    // CRITICAL FIX: Create stable ImageRequest with remember(imageUrl)
                    // This ensures the request is ONLY recreated when imageUrl changes
                    val imageRequest = remember(imageUrl) {
                        android.util.Log.d("PPDTTestScreen", "🔄 Creating NEW ImageRequest for: $imageUrl")
                        ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .listener(
                                onStart = {
                                    android.util.Log.d("PPDTTestScreen", "🖼️ Coil: Loading started")
                                },
                                onSuccess = { _, _ ->
                                    android.util.Log.d("PPDTTestScreen", "✅ Coil: Image loaded successfully!")
                                },
                                onError = { _, result ->
                                    android.util.Log.e("PPDTTestScreen", "❌ Coil: Load failed: ${result.throwable.message}", result.throwable)
                                }
                            )
                            .build()
                    }

                    // AsyncImage with stable model - won't restart on recomposition
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "PPDT Test Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { timeRemainingSeconds / 30f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
