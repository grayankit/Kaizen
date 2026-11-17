package me.ankitgrai.kaizen.widget

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

internal val widgetTitleColor = ColorProvider(
    day = widgetDayContent,
    night = widgetNightContent,
)

internal val widgetLabelColor = ColorProvider(
    day = widgetDayMuted,
    night = widgetNightMuted,
)

internal fun widgetTitleStyle(
    fontSize: TextUnit,
    align: TextAlign? = null,
) = TextStyle(
    color = widgetTitleColor,
    fontSize = fontSize,
    fontWeight = FontWeight.Bold,
    textAlign = align,
)

internal fun widgetLabelStyle(
    align: TextAlign? = null,
) = TextStyle(
    color = widgetLabelColor,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    textAlign = align,
)
