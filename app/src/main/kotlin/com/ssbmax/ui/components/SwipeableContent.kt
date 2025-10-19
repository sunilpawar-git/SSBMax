package com.ssbmax.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Direction of swipe gesture
 */
enum class SwipeDirection {
    LEFT,
    RIGHT
}

/**
 * Swipeable content wrapper for gesture-based navigation
 * Enables swipe left/right between tabs or pages
 *
 * @param modifier Modifier for the container
 * @param enabled Whether swipe gestures are enabled
 * @param swipeThreshold Minimum swipe distance to trigger navigation (0f to 1f of screen width)
 * @param onSwipeLeft Callback when user swipes left (swipe right to left)
 * @param onSwipeRight Callback when user swipes right (swipe left to right)
 * @param content The content to display
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
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                val threshold = size.width * swipeThreshold
                                
                                when {
                                    offsetX < -threshold && onSwipeLeft != null -> {
                                        // Swiped left (swipe right to left)
                                        onSwipeLeft()
                                    }
                                    offsetX > threshold && onSwipeRight != null -> {
                                        // Swiped right (swipe left to right)
                                        onSwipeRight()
                                    }
                                }
                                
                                // Reset
                                offsetX = 0f
                                isDragging = false
                            },
                            onDragCancel = {
                                offsetX = 0f
                                isDragging = false
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                // Limit swipe distance
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
 * Tab swipeable content with visual indicators
 * Shows edge shadows/glows when swiping
 *
 * @param modifier Modifier for the container
 * @param currentIndex Current tab index
 * @param totalTabs Total number of tabs
 * @param onTabChange Callback when tab changes via swipe
 * @param content The content to display
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
        onSwipeLeft = if (canSwipeLeft) {
            { onTabChange(currentIndex + 1) }
        } else null,
        onSwipeRight = if (canSwipeRight) {
            { onTabChange(currentIndex - 1) }
        } else null,
        content = content
    )
}

/**
 * Pager-style swipeable content
 * For horizontal paging between screens
 *
 * @param modifier Modifier for the container
 * @param currentPage Current page index
 * @param totalPages Total number of pages
 * @param onPageChange Callback when page changes
 * @param enabledLeft Whether swiping left (to next page) is enabled
 * @param enabledRight Whether swiping right (to previous page) is enabled
 * @param content The content to display
 */
@Composable
fun PagerSwipeableContent(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabledLeft: Boolean = true,
    enabledRight: Boolean = true,
    content: @Composable () -> Unit
) {
    val canSwipeLeft = currentPage < totalPages - 1 && enabledLeft
    val canSwipeRight = currentPage > 0 && enabledRight
    
    SwipeableContent(
        modifier = modifier,
        enabled = true,
        swipeThreshold = 0.3f,
        onSwipeLeft = if (canSwipeLeft) {
            { onPageChange(currentPage + 1) }
        } else null,
        onSwipeRight = if (canSwipeRight) {
            { onPageChange(currentPage - 1) }
        } else null,
        content = content
    )
}

/**
 * Dismissible swipeable content
 * For swipe-to-dismiss interactions
 *
 * @param modifier Modifier for the container
 * @param onDismiss Callback when content is dismissed
 * @param dismissDirection Direction(s) allowed for dismissal
 * @param content The content to display
 */
@Composable
fun DismissibleSwipeableContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissDirection: Set<SwipeDirection> = setOf(SwipeDirection.LEFT, SwipeDirection.RIGHT),
    content: @Composable () -> Unit
) {
    SwipeableContent(
        modifier = modifier,
        enabled = true,
        swipeThreshold = 0.4f,
        onSwipeLeft = if (SwipeDirection.LEFT in dismissDirection) onDismiss else null,
        onSwipeRight = if (SwipeDirection.RIGHT in dismissDirection) onDismiss else null,
        content = content
    )
}

