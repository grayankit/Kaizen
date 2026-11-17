package me.ankitgrai.kaizen.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider as GlanceColorProvider

internal val widgetDayBackground = Color(0xFFF6F2FF)
internal val widgetNightBackground = Color(0xFF1C1B22)
internal val widgetDayContent = Color(0xFF1C1B22)
internal val widgetNightContent = Color(0xFFF6F2FF)
internal val widgetDayMuted = Color(0xFF6B6578)
internal val widgetNightMuted = Color(0xFFB7B1C4)

internal val widgetCornerRadius = 20.dp

@Composable
internal fun widgetOpacity(): Float {
    return WidgetSettings.opacityPercent.coerceIn(
        WidgetSettings.MIN_OPACITY_PERCENT,
        WidgetSettings.MAX_OPACITY_PERCENT,
    ) / 100f
}

internal fun widgetBackgroundColor(opacity: Float): GlanceColorProvider {
    val alpha = opacity.coerceIn(0f, 1f)
    return ColorProvider(
        day = widgetDayBackground.copy(alpha = alpha),
        night = widgetNightBackground.copy(alpha = alpha),
    )
}

// Android 12+ paints its own plate behind a widget unless a view is marked as
// the background. That plate would hide the wallpaper even when our fill is translucent.
internal fun GlanceModifier.widgetBackground(opacity: Float): GlanceModifier =
    fillMaxSize()
        .appWidgetBackground()
        .background(widgetBackgroundColor(opacity))
        .cornerRadius(widgetCornerRadius)
