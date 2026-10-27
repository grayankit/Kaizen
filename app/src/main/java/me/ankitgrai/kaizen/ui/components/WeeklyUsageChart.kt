package me.ankitgrai.kaizen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.ankitgrai.kaizen.data.usage.DailyAppUsage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight

@Composable
fun WeeklyUsageChart(
    history: List<DailyAppUsage>,
    selectedDate: LocalDate?,
    onDaySelected: (LocalDate) -> Unit
) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedBackground = barColor.copy(alpha = 0.12f)

    val maxUsage = history.maxOfOrNull {
        it.totalTimeInForeground
    }?.coerceAtLeast(1L) ?: 1L

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        history.forEach {day ->
            val isSelected = day.date == selectedDate
            val fraction  = (
                    day.totalTimeInForeground.toFloat() / maxUsage
                    ).coerceIn(0f,1f)
            val barHeight = if (day.totalTimeInForeground > 0L) {
                (130f * fraction).dp
            } else {
                3.dp
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if(isSelected) selectedBackground
                        else Color.Transparent
                    )
                    .clickable{
                        onDaySelected(day.date)
                    }
                    .padding(
                        horizontal = 2.dp,
                        vertical = 6.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(barHeight)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 8.dp,
                                    topEnd = 8.dp
                                )
                            )
                            .background(
                                if (isSelected) {
                                    barColor
                                } else {
                                    barColor.copy(alpha = 0.65f)
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = day.date.format(
                        DateTimeFormatter.ofPattern(
                            "EE",
                            LocalLocale.current.platformLocale
                        )
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        labelColor
                    }
                )

            }
        }
    }

}