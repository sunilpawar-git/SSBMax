package com.ssbmax.shared.ui.gto.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.ssbmax.shared.platform.audio.WhiteNoisePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * KMP port of `app/.../ui/tests/gto/common/GTOWhiteNoisePlayer.kt`'s visual
 * overlay + state wiring, unchanged behavior -- shared by GD and Lecturette
 * (and future GPE/PGT/etc.). The audio half already lives in
 * [com.ssbmax.shared.platform.audio.WhiteNoisePlayer] (Phase 4, Koin-injected
 * per-platform actual); this file is the visual grain overlay + the
 * [WhiteNoiseState] wrapper that ties audio+visual together, same split as
 * the Android original.
 */
@Composable
fun AnimatedWhiteNoiseOverlay(
    baseAlpha: Float = 0.1f,
    intensityRange: Float = 0.05f,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "noise_intensity")
    val intensity by infiniteTransition.animateFloat(
        initialValue = baseAlpha,
        targetValue = baseAlpha + intensityRange,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "intensity"
    )

    WhiteNoiseOverlay(alpha = intensity, isEnabled = isEnabled, modifier = modifier)
}

@Composable
private fun WhiteNoiseOverlay(
    alpha: Float,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    var seed by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            seed = Random.nextLong()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width.toInt()
        val canvasHeight = size.height.toInt()
        val grainSize = 8f
        val random = Random(seed)
        val stepSize = grainSize.toInt()

        for (x in 0 until canvasWidth step stepSize) {
            for (y in 0 until canvasHeight step stepSize) {
                val brightness = random.nextFloat()
                if (brightness > 0.1f) {
                    drawRect(
                        color = Color.White.copy(alpha = brightness * alpha),
                        topLeft = Offset(x.toFloat(), y.toFloat()),
                        size = Size(grainSize, grainSize)
                    )
                }
            }
        }
    }
}

/** Manages audio and visual white noise together. */
@Stable
class WhiteNoiseState(
    private val player: WhiteNoisePlayer,
    private val scope: CoroutineScope
) {
    private val _isEnabled = mutableStateOf(false)
    val isEnabled: State<Boolean> = _isEnabled

    fun enable() {
        if (!_isEnabled.value) {
            scope.launch { player.startAudio() }
            _isEnabled.value = true
        }
    }

    fun disable() {
        if (_isEnabled.value) {
            player.stopAudio()
            _isEnabled.value = false
        }
    }
}

@Composable
fun rememberWhiteNoiseState(player: WhiteNoisePlayer): WhiteNoiseState {
    val scope = rememberCoroutineScope()
    return remember(player) { WhiteNoiseState(player, scope) }
}
