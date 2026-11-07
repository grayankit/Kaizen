package me.ankitgrai.kaizen.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.R

/**
 * Compose splash screen shown after the native Android SplashScreen API exits.
 *
 * HOW ANIMATIONS WORK IN COMPOSE:
 *
 * [Animatable] is a state holder that can be animated from one value to another.
 * Think of it like a variable whose changes are automatically smoothed over time.
 *
 * [LaunchedEffect] runs a coroutine when the composable enters the composition.
 * We use it here to kick off our animations. The key `Unit` means "run once".
 *
 * We animate two properties:
 *  - alpha (opacity): 0f → 1f  makes the logo fade in
 *  - scale: 0.6f → 1f          makes the logo grow into place
 *
 * Both run concurrently via coroutines (the second `animateTo` is called right
 * after the first without waiting because we use `launch { }` for one of them).
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // 1. Create Animatable state holders with their initial (start) values
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }

    // 2. LaunchedEffect runs this block once when SplashScreen enters the screen.
    //    It's a coroutine scope, so we can call `animateTo` which suspends until done.
    LaunchedEffect(Unit) {
        // Animate alpha (fade in) and scale (grow) together
        // `animateTo` suspends until the animation completes
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        // After fade-in, hold for a moment, then trigger navigation
        kotlinx.coroutines.delay(600)
        onSplashFinished()
    }

    // Also animate scale concurrently with alpha — we launch it separately
    // so it doesn't block the outer coroutine
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    // 3. The UI: dark background, centered logo + app name
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Hard dark background — not affected by the user's theme
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            // Apply both alpha and scale transforms to the whole column
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
        ) {
            // App icon — same vector used in the launcher
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Kaizen logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Kaizen",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "改善 · continuous improvement",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
