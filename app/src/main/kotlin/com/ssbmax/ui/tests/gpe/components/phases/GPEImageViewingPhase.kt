package com.ssbmax.ui.tests.gpe.components.phases

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssbmax.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.imageLoader

/**
 * GPE Image Viewing Phase
 * Shows the tactical scenario image for 60 seconds
 */
@Composable
fun GPEImageViewingPhase(
    imageUrl: String,
    timeRemainingSeconds: Int
) {
    android.util.Log.d("GPETestScreen", "🎨 ImageViewingPhase recomposed with imageUrl: $imageUrl")

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
                text = stringResource(R.string.gpe_image_viewing_instruction),
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
                if (imageUrl.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.error_image_url_empty),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.check_logs),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    val context = LocalContext.current

                    LaunchedEffect(imageUrl) {
                        android.util.Log.d("GPETestScreen", "🖼️ NEW URL SET: $imageUrl")
                    }

                    val imageRequest = remember(imageUrl) {
                        android.util.Log.d("GPETestScreen", "🔄 Creating NEW ImageRequest for: $imageUrl")
                        ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .listener(
                                onStart = {
                                    android.util.Log.d("GPETestScreen", "🖼️ Coil: Loading started")
                                },
                                onSuccess = { _, _ ->
                                    android.util.Log.d("GPETestScreen", "✅ Coil: Image loaded successfully!")
                                },
                                onError = { _, result ->
                                    android.util.Log.e("GPETestScreen", "❌ Coil: Load failed: ${result.throwable.message}", result.throwable)
                                }
                            )
                            .build()
                    }

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = stringResource(R.string.gpe_test_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onLoading = {
                            android.util.Log.d("GPETestScreen", "⏳ State: Loading")
                        },
                        onSuccess = {
                            android.util.Log.d("GPETestScreen", "✅ State: Success")
                        },
                        onError = { state ->
                            android.util.Log.e("GPETestScreen", "❌ State: Error: ${state.result.throwable.message}")
                        },
                        error = rememberVectorPainter(Icons.Filled.BrokenImage)
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { timeRemainingSeconds / 60f }, // 60 seconds for GPE
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
