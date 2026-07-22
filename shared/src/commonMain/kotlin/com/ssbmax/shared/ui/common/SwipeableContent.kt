package com.ssbmax.shared.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * KMP port of the Android `app/.../ui/components/SwipeableContent.kt` --
 * swipe-gesture wrapper for gesture-based tab navigation (used by
 * [com.ssbmax.shared.ui.topic.TopicScreen]'s 3-tab layout). Only
 * `SwipeableContent`/`TabSwipeableContent` ported -- `PagerSwipeableContent`/
 * `DismissibleSwipeableContent`/`SwipeDirection` (same Android original file)
 * confirmed dead code, zero call sites anywhere in `app/ui`, correctly not
 * ported.
 */
@Composable
fun SwipeableContent(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    swipeThreshold: Float = 0.3f,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        label = "swipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = animatedOffsetX
                alpha = 1f - (abs(animatedOffsetX) / 1000f).coerceIn(0f, 0.3f)
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                val threshold = size.width * swipeThreshold
                                when {
                                    offsetX < -threshold && onSwipeLeft != null -> onSwipeLeft()
                                    offsetX > threshold && onSwipeRight != null -> onSwipeRight()
                                }
                                offsetX = 0f
                                isDragging = false
                            },
                            onDragCancel = {
                                offsetX = 0f
                                isDragging = false
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val maxSwipe = size.width * 0.5f
                                offsetX = (offsetX + dragAmount).coerceIn(-maxSwipe, maxSwipe)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * Tab swipeable content -- swipe left/right between tabs.
 */
@Composable
fun TabSwipeableContent(
    currentIndex: Int,
    totalTabs: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val canSwipeLeft = currentIndex < totalTabs - 1
    val canSwipeRight = currentIndex > 0

    SwipeableContent(
        modifier = modifier,
        enabled = true,
        swipeThreshold = 0.25f,
        onSwipeLeft = if (canSwipeLeft) { { onTabChange(currentIndex + 1) } } else null,
        onSwipeRight = if (canSwipeRight) { { onTabChange(currentIndex - 1) } } else null,
        content = content
    )
}
