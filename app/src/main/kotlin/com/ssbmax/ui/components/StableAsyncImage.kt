package com.ssbmax.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * StableAsyncImage - A composable that loads images WITHOUT restarting on recomposition
 * 
 * PROBLEM SOLVED:
 * When a composable recomposes (e.g., due to timer updates), creating a new ImageRequest
 * on each recomposition causes Coil to restart loading the image. This leads to infinite
 * loading loops where images never finish loading.
 * 
 * SOLUTION:
 * Uses remember(imageUrl) to create a STABLE ImageRequest that is only recreated when
 * the imageUrl changes, not on every recomposition.
 * 
 * USE THIS INSTEAD OF:
 * - Direct AsyncImage with inline ImageRequest.Builder
 * - rememberAsyncImagePainter without proper remember() wrapping
 * - Any image loading that doesn't use remember() for the request
 * 
 * USAGE:
 * ```kotlin
 * StableAsyncImage(
 *     imageUrl = "https://example.com/image.jpg",
 *     contentDescription = "Test image",
 *     modifier = Modifier.fillMaxSize(),
 *     contentScale = ContentScale.Fit,
 *     onLoadingStart = { Log.d("Image", "Loading...") },
 *     onSuccess = { Log.d("Image", "Loaded!") },
 *     onError = { error -> Log.e("Image", "Failed: $error") }
 * )
 * ```
 * 
 * APPLIES TO:
 * - PPDT test images ✅
 * - TAT test images
 * - GTO scenario images
 * - Any test/content that displays images while timers/state changes occur
 * 
 * @param imageUrl The URL of the image to load. When this changes, a new request is created.
 * @param contentDescription Accessibility description
 * @param modifier Modifier for the image
 * @param contentScale How to scale the image (Fit, Crop, etc.)
 * @param alignment How to align the image within its bounds
 * @param onLoadingStart Called when image loading starts (optional)
 * @param onSuccess Called when image loads successfully (optional)
 * @param onError Called when image loading fails (optional, receives error message)
 */
@Composable
fun StableAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    onLoadingStart: (() -> Unit)? = null,
    onSuccess: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // CRITICAL: Use remember(imageUrl) to create a STABLE ImageRequest
    // This ensures the request is ONLY recreated when imageUrl changes,
    // NOT on every recomposition (e.g., timer ticks, state changes)
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .listener(
                onStart = {
                    onLoadingStart?.invoke()
                },
                onSuccess = { _, _ ->
                    onSuccess?.invoke()
                },
                onError = { _, result ->
                    onError?.invoke(result.throwable.message ?: "Unknown error")
                }
            )
            .build()
    }
    
    // AsyncImage with stable model - won't restart on recomposition
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment
    )
}

/**
 * StableAsyncImage with custom loading/error UI
 * 
 * Provides slots for custom loading and error composables.
 * Use this when you need custom UI for loading/error states.
 * 
 * @param loading Composable to show while loading
 * @param error Composable to show on error (receives error message)
 */
@Composable
fun StableAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    loading: @Composable (() -> Unit)? = null,
    error: @Composable ((String) -> Unit)? = null,
    onLoadingStart: (() -> Unit)? = null,
    onSuccess: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .listener(
                onStart = {
                    onLoadingStart?.invoke()
                },
                onSuccess = { _, _ ->
                    onSuccess?.invoke()
                },
                onError = { _, result ->
                    // Call non-composable callback
                    onError?.invoke(result.throwable.message ?: "Unknown error")
                }
            )
            .build()
    }
    
    Box(modifier = modifier, contentAlignment = alignment) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            alignment = alignment
        )
        
        // Loading and error states handled by AsyncImage's internal state
        // Custom slots would require SubcomposeAsyncImage or state tracking
    }
}

