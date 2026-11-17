package me.ankitgrai.kaizen.ui.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.components.roundedClickable

data class BarEntry(
    val id: String,
    val label: String,
    val value: Float,
    val showBar: Boolean = true,
    val clickable: Boolean = true,
    val valueLabel: String? = null,
    val contentDescription: String? = null,
)

enum class BarPalette {
    Muted,
    Accent,
}

enum class BarLayout {
    Stacked,
    Plot,
}

enum class BarLabelStyle {
    Medium,
    Small,
    Fixed,
}

data class BarChartStyle(
    val palette: BarPalette,
    val layout: BarLayout,
    val scrollable: Boolean = false,
    val chartHeight: Dp,
    val maxBarHeight: Dp,
    val barWidth: Dp,
    val barCorner: Dp = 8.dp,
    val minBarHeight: Dp = 4.dp,
    val enforceMinOnPositive: Boolean = true,
    val itemSpacing: Dp = 0.dp,
    val labelGap: Dp = 8.dp,
    val animate: Boolean = true,
    val equalWidthUpTo: Int? = null,
    val fixedItemWidth: Dp = 42.dp,
    val clickCorner: Dp = 12.dp,
    val labelStyle: BarLabelStyle = BarLabelStyle.Medium,
    val labelFontSize: TextUnit = 10.sp,
    val labelHeight: Dp = 14.dp,
) {
    companion object {
        val OverviewWeek = BarChartStyle(
            palette = BarPalette.Muted,
            layout = BarLayout.Stacked,
            chartHeight = 112.dp,
            maxBarHeight = 76.dp,
            barWidth = 22.dp,
            itemSpacing = 10.dp,
            labelGap = 10.dp,
        )
        val ReelWeek = BarChartStyle(
            palette = BarPalette.Muted,
            layout = BarLayout.Stacked,
            chartHeight = 152.dp,
            maxBarHeight = 120.dp,
            barWidth = 22.dp,
            itemSpacing = 10.dp,
            labelGap = 8.dp,
        )
        val HistoryDays = BarChartStyle(
            palette = BarPalette.Accent,
            layout = BarLayout.Plot,
            scrollable = true,
            chartHeight = 130.dp,
            maxBarHeight = 130.dp,
            barWidth = 24.dp,
            barCorner = 7.dp,
            equalWidthUpTo = 7,
            fixedItemWidth = 42.dp,
            labelStyle = BarLabelStyle.Fixed,
            labelFontSize = 10.sp,
        )
        val HistoryHours = BarChartStyle(
            palette = BarPalette.Accent,
            layout = BarLayout.Plot,
            scrollable = true,
            chartHeight = 130.dp,
            maxBarHeight = 130.dp,
            barWidth = 16.dp,
            barCorner = 6.dp,
            fixedItemWidth = 32.dp,
            clickCorner = 10.dp,
            labelStyle = BarLabelStyle.Fixed,
            labelFontSize = 9.sp,
        )
        val AppWeek = BarChartStyle(
            palette = BarPalette.Accent,
            layout = BarLayout.Plot,
            chartHeight = 130.dp,
            maxBarHeight = 130.dp,
            barWidth = 26.dp,
            minBarHeight = 3.dp,
            enforceMinOnPositive = false,
            itemSpacing = 2.dp,
            animate = false,
            labelStyle = BarLabelStyle.Small,
        )
    }
}

@Composable
fun SelectableBarChart(
    entries: List<BarEntry>,
    selectedId: String?,
    modifier: Modifier = Modifier,
    onSelected: ((String) -> Unit)? = null,
    style: BarChartStyle = BarChartStyle.HistoryDays,
) {
    val maxValue = entries.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    if (style.layout == BarLayout.Stacked) {
        StackedChart(entries, selectedId, onSelected, style, maxValue, modifier)
    } else {
        PlotChart(entries, selectedId, onSelected, style, maxValue, modifier)
    }
}

@Composable
private fun StackedChart(
    entries: List<BarEntry>,
    selectedId: String?,
    onSelected: ((String) -> Unit)?,
    style: BarChartStyle,
    maxValue: Float,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(style.chartHeight),
        horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEach { entry ->
            StackedBar(entry, entry.id == selectedId, onSelected, style, maxValue)
        }
    }
}

@Composable
private fun RowScope.StackedBar(
    entry: BarEntry,
    selected: Boolean,
    onSelected: ((String) -> Unit)?,
    style: BarChartStyle,
    maxValue: Float,
) {
    val barHeight = barHeight(entry, style, maxValue)
    val clickable = onSelected != null && entry.clickable
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .then(
                if (clickable) {
                    Modifier.roundedClickable(RoundedCornerShape(style.clickCorner)) {
                        onSelected.invoke(entry.id)
                    }
                } else {
                    Modifier
                },
            )
            .semantics {
                entry.contentDescription?.let { contentDescription = it }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (entry.valueLabel != null) {
            Text(
                text = entry.valueLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        if (entry.showBar) {
            Box(
                modifier = Modifier
                    .width(style.barWidth)
                    .height(barHeight)
                    .clip(
                        RoundedCornerShape(
                            topStart = style.barCorner,
                            topEnd = style.barCorner,
                        ),
                    )
                    .background(barColor(selected, style.palette)),
            )
        } else {
            Spacer(modifier = Modifier.height(style.minBarHeight))
        }
        Spacer(modifier = Modifier.height(style.labelGap))
        BarLabel(entry.label, selected, style)
    }
}

@Composable
private fun PlotChart(
    entries: List<BarEntry>,
    selectedId: String?,
    onSelected: ((String) -> Unit)?,
    style: BarChartStyle,
    maxValue: Float,
    modifier: Modifier,
) {
    if (!style.scrollable) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            entries.forEach { entry ->
                PlotBar(
                    entry = entry,
                    selected = entry.id == selectedId,
                    onSelected = onSelected,
                    style = style,
                    maxValue = maxValue,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val itemWidth = if (style.equalWidthUpTo != null && entries.size <= style.equalWidthUpTo) {
            maxWidth / entries.size.coerceAtLeast(1)
        } else {
            style.fixedItemWidth
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top,
        ) {
            entries.forEach { entry ->
                PlotBar(
                    entry = entry,
                    selected = entry.id == selectedId,
                    onSelected = onSelected,
                    style = style,
                    maxValue = maxValue,
                    modifier = Modifier.width(itemWidth),
                )
            }
        }
    }
}

@Composable
private fun PlotBar(
    entry: BarEntry,
    selected: Boolean,
    onSelected: ((String) -> Unit)?,
    style: BarChartStyle,
    maxValue: Float,
    modifier: Modifier,
) {
    val barHeight = barHeight(entry, style, maxValue)
    val clickable = onSelected != null && entry.clickable
    val highlight = selected && style.palette == BarPalette.Accent
    Column(
        modifier = modifier
            .then(
                if (clickable) {
                    Modifier.roundedClickable(RoundedCornerShape(style.clickCorner)) {
                        onSelected.invoke(entry.id)
                    }
                } else {
                    Modifier
                },
            )
            .background(
                if (highlight) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(style.chartHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (entry.showBar) {
                Box(
                    modifier = Modifier
                        .width(style.barWidth)
                        .height(barHeight)
                        .clip(
                            RoundedCornerShape(
                                topStart = style.barCorner,
                                topEnd = style.barCorner,
                            ),
                        )
                        .background(barColor(selected, style.palette)),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BarLabel(entry.label, selected, style)
    }
}

@Composable
private fun barHeight(entry: BarEntry, style: BarChartStyle, maxValue: Float): Dp {
    val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
    val target = when {
        entry.value > 0f && style.enforceMinOnPositive ->
            (style.maxBarHeight.value * fraction).dp.coerceAtLeast(style.minBarHeight)
        entry.value > 0f -> (style.maxBarHeight.value * fraction).dp
        else -> style.minBarHeight
    }
    val animated by animateDpAsState(
        targetValue = target,
        animationSpec = tween(220),
        label = "usageBar",
    )
    return if (style.animate) animated else target
}

@Composable
private fun barColor(selected: Boolean, palette: BarPalette): Color {
    val primary = MaterialTheme.colorScheme.primary
    return when (palette) {
        BarPalette.Muted -> if (selected) primary else MaterialTheme.colorScheme.surfaceVariant
        BarPalette.Accent -> if (selected) primary else primary.copy(alpha = 0.65f)
    }
}

@Composable
private fun BarLabel(
    label: String,
    selected: Boolean,
    style: BarChartStyle,
) {
    val primary = MaterialTheme.colorScheme.primary
    val color = when (style.palette) {
        BarPalette.Muted -> if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
        BarPalette.Accent -> if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    val weight = if (selected) FontWeight.Bold else FontWeight.Normal
    when (style.labelStyle) {
        BarLabelStyle.Medium -> Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = weight,
        )
        BarLabelStyle.Small -> Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = weight,
        )
        BarLabelStyle.Fixed -> Text(
            text = label,
            modifier = Modifier.height(style.labelHeight),
            color = color,
            fontSize = style.labelFontSize,
            fontWeight = weight,
        )
    }
}
