package me.ankitgrai.kaizen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun chartSurfaceColor(strength: Float = 0.10f): Color {
    val background = MaterialTheme.colorScheme.background
    // Pure black needs a stronger lift or the card disappears into the page.
    val amount = if (background.red + background.green + background.blue < 0.04f) {
        strength.coerceAtLeast(0.16f)
    } else {
        strength
    }
    return lerp(background, Color.White, amount)
}
