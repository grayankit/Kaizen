package me.ankitgrai.kaizen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.theme.KaizenTheme

data class UsageBar(
    val label: String,
    val minutes: Int
)
@Composable
fun UsageBarChart(
    data: List<UsageBar>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
){
    val accent = Color(0xFF9ACDD5)
    val selectedColor = Color(0xFFB8A8FF)
    val muted = Color(0xFFA5A7B8)
    val lineColor = Color(0xFF353844)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
    ) {
        val maxMinutes = (data.maxOfOrNull { it.minutes } ?: 1)
            .coerceAtLeast(1)

        val barCount = data.size.coerceAtLeast(1)
        val spacing = 8.dp.toPx()
        val barWidth = (
                size.width - spacing * (barCount - 1)
                ) / barCount

        drawLine(
            color = lineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx()
        )

        data.forEachIndexed { index, bar ->
            val fraction = bar.minutes.toFloat() / maxMinutes
            val barHeight = fraction * (size.height - 8.dp.toPx())
            val left = index * (barWidth + spacing)
            val top = size.height - barHeight

            drawRoundRect(
                color = if (index == selectedIndex) {
                    selectedColor
                } else {
                    accent
                },
                topLeft = Offset(left, top),
                size = Size(
                    width = barWidth.coerceAtLeast(0f),
                    height = barHeight.coerceAtLeast(0f)
                ),
                cornerRadius = CornerRadius(
                    x = 10.dp.toPx(),
                    y = 10.dp.toPx()
                )
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        data.forEachIndexed { index, bar ->
            Text(
                text = bar.label,
                fontSize = 10.sp,
                fontWeight = if (index == selectedIndex) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = if (index == selectedIndex) {
                    selectedColor
                } else {
                    muted
                }
            )
        }
    }
}

@Preview
@Composable
private fun PreviewUsageBarChart() {
    KaizenTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            UsageBarChart(
                data = listOf(
                    UsageBar("Mon", 120),
                    UsageBar("Tue", 200),
                    UsageBar("Wed", 150),
                    UsageBar("Thu", 80),
                    UsageBar("Fri", 250),
                    UsageBar("Sat", 300),
                    UsageBar("Sun", 180)
                ),
                selectedIndex = 5
            )
        }
    }
}
