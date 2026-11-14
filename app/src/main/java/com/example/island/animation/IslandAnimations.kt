package com.example.island.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.IslandConfiguration

object IslandAnimations {

    fun <T> morphSpringSpec(config: IslandConfiguration): AnimationSpec<T> {
        if (config.batteryFriendlyMode) {
            return tween(durationMillis = 180, easing = FastOutSlowInEasing)
        }
        val speedFactor = config.animationSpeedMultiplier.coerceIn(0.5f, 2.0f)
        val effectiveStiffness = (config.animationPreset.stiffness * speedFactor)
            .coerceIn(Spring.StiffnessLow, Spring.StiffnessHigh)
        val effectiveDamping = config.animationPreset.dampingRatio.coerceIn(0.45f, 1.0f)
        return spring(
            dampingRatio = effectiveDamping,
            stiffness = effectiveStiffness
        )
    }
}

@Composable
fun AnimatedEqualizerBars(
    isPlaying: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    maxHeight: Dp = 16.dp,
    batterySaver: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_transition")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying && !batterySaver) 0.95f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = if (isPlaying && !batterySaver) 1.0f else 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 270, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = if (isPlaying && !batterySaver) 0.85f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_3"
    )
    val phase4 by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = if (isPlaying && !batterySaver) 0.90f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_4"
    )

    val phases = listOf(phase1, phase2, phase3, phase4)

    Row(
        modifier = modifier
            .width((barCount * 4 + (barCount - 1) * 2).dp)
            .height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width((barCount * 4 + (barCount - 1) * 2).dp)
                .height(maxHeight)
        ) {
            val barWidthPx = 3.dp.toPx()
            val gapPx = 2.5.dp.toPx()
            val totalHeight = size.height
            for (i in 0 until barCount) {
                val fraction = if (isPlaying) phases[i % phases.size] else 0.22f
                val barHeight = (totalHeight * fraction).coerceAtLeast(3.dp.toPx())
                val topOffset = (totalHeight - barHeight) / 2f
                val leftOffset = i * (barWidthPx + gapPx)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(leftOffset, topOffset),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
                )
            }
        }
    }
}
