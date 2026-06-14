package com.ssbmax.ui.tests.ppdt.components.phases

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun PPDTImageViewingPhase(
    imageUrl: String,
    timeRemainingSeconds: Int
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = stringResource(R.string.ppdt_image_observe_instruction),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
        PPDTImageCard(imageRequest)
        Spacer(Modifier.weight(1f))
        PPDTTimerProgressBar(timeRemainingSeconds = timeRemainingSeconds, totalSeconds = 30)
    }
}

@Composable
private fun PPDTImageCard(imageRequest: ImageRequest) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.ppdt_image_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun PPDTTimerProgressBar(timeRemainingSeconds: Int, totalSeconds: Int) {
    LinearProgressIndicator(
        progress = { timeRemainingSeconds / totalSeconds.toFloat() },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    )
}
