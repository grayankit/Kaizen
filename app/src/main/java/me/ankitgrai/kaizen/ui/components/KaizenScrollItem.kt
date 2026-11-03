package me.ankitgrai.kaizen.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun KaizenScrollItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SCALE_MILLIS,
                easing = LinearEasing
            )
        )
    }
    Box(
        modifier = modifier.graphicsLayer {
            val slide = EaseInOut.transform(
                (progress.value / (SLIDE_MILLIS.toFloat() / SCALE_MILLIS)).coerceIn(0f, 1f)
            )
            val scale = 0.1f + 0.9f * EaseInOut.transform(progress.value)
            scaleX = scale
            scaleY = scale
            translationY = -size.height * (1f - slide)
            transformOrigin = TransformOrigin.Center
        }
    ) {
        content()
    }
}

private const val SLIDE_MILLIS = 200
private const val SCALE_MILLIS = 400
