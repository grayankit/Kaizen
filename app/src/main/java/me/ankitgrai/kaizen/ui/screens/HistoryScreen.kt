package me.ankitgrai.kaizen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.ankitgrai.kaizen.ui.components.AppUsage
import me.ankitgrai.kaizen.ui.components.UsageBar
import me.ankitgrai.kaizen.ui.components.UsagePeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.components.AppUsageRow
import me.ankitgrai.kaizen.ui.components.PeriodSelector
import me.ankitgrai.kaizen.ui.components.UsageBarChart

private data class HistoryDay(
    val date: LocalDate,
    val minutes: Int
)
private val HistoryApps = listOf(
    AppUsage(
        name = "YouTube",
        category = "Video",
        duration = "6h 18m",
        initials = "YT",
        color = Color(0xFFF28B9B),
        usageProgress = 0.90f
    ),
    AppUsage(
        name = "Instagram",
        category = "Social",
        duration = "3h 24m",
        initials = "IG",
        color = Color(0xFFC49BFF),
        usageProgress = 0.62f
    ),
    AppUsage(
        name = "Chrome",
        category = "Web",
        duration = "2h 41m",
        initials = "CH",
        color = Color(0xFF8CB8FF),
        usageProgress = 0.48f
    ),
    AppUsage(
        name = "Spotify",
        category = "Music",
        duration = "1h 56m",
        initials = "SP",
        color = Color(0xFFA6D989),
        usageProgress = 0.34f
    ),
    AppUsage(
        name = "Discord",
        category = "Social",
        duration = "1h 12m",
        initials = "DC",
        color = Color(0xFF9C9FFF),
        usageProgress = 0.22f
    )
)



@Composable
fun HistoryScreen(getDefault: Locale.Companion.() -> java.util.Locale = { java.util.Locale.getDefault() }) {
    var selectedPeriod by remember {
        mutableStateOf(UsagePeriod.WEEK)
    }

    var selectedDate by remember {
        mutableStateOf(LocalDate.now())
    }

    var selectedIndex by remember {
        mutableIntStateOf(0)
    }
    val dailyData = remember (selectedDate) {
        val start = selectedDate.with(
            java.time.DayOfWeek.MONDAY
        )

        val minutes = listOf(
            138, 222, 171, 245, 192, 104, 76
        )

        (0..6).map { index ->
            HistoryDay(
                date = start.plusDays(index.toLong()),
                minutes = minutes[index]
            )
        }
    }
        val chartData = when (selectedPeriod) {
            UsagePeriod.DAY -> listOf(
                UsageBar("00", 0),
                UsageBar("04", 0),
                UsageBar("08", 25),
                UsageBar("12", 85),
                UsageBar("16", 120),
                UsageBar("20", 48)
            )

            UsagePeriod.WEEK -> dailyData.map {
                UsageBar(
                    label = it.date.format(
                        DateTimeFormatter.ofPattern("EE", LocalLocale.current.platformLocale)
                    ).take(1),
                    minutes = it.minutes
                )
            }

            UsagePeriod.MONTH -> (1..30).map { day ->
                UsageBar(
                    label = if (day % 5 == 0) "$day" else "",
                    minutes = 60 + (day * 37 % 200)
                )
            }
        }
        val totalMinutes = when (selectedPeriod) {
            UsagePeriod.DAY -> 278
            UsagePeriod.WEEK -> dailyData.sumOf { it.minutes }
            UsagePeriod.MONTH -> (1..30).sumOf { 60 + (it * 37 % 200) }
        }

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp))
    {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "History",
                color = Color(0xFFE6E6F0),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  ="Understand your screen-time patterns.",
                color = Color(0xFFA5A7B8),
                fontSize = 12.sp
            )
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = {
                    selectedPeriod = it
                    selectedIndex = 0
                },
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF171923),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (selectedPeriod) {
                        UsagePeriod.DAY -> "TODAY'S USAGE"
                        UsagePeriod.WEEK -> "THIS WEEK"
                        UsagePeriod.MONTH -> "THIS MONTH"
                    },
                    color = Color(0xFFA5A7B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${hours}h ${minutes}m",
                    color = Color(0xFFE6E6F0),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sample usage data",
                    color = Color(0xFFA5A7B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                UsageBarChart(
                    data = chartData,
                    selectedIndex = selectedIndex.coerceIn(
                        0,
                        chartData.lastIndex
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF9DD5D9),
                        RoundedCornerShape(15.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowLeft,
                    contentDescription = null,
                    tint = Color(0xFF203F42),
                    modifier = Modifier.clickable {
                        selectedDate = when (selectedPeriod) {
                            UsagePeriod.DAY -> selectedDate.minusDays(1)
                            UsagePeriod.WEEK -> selectedDate.minusWeeks(1)
                            UsagePeriod.MONTH -> selectedDate.minusMonths(1)
                        }
                        selectedIndex = 0
                    }
                )
                Text(
                    text = when (selectedPeriod) {
                        UsagePeriod.DAY -> selectedDate.format(
                            DateTimeFormatter.ofPattern(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            )
                        )

                        UsagePeriod.WEEK -> {
                            val start = selectedDate.with(
                                java.time.DayOfWeek.MONDAY
                            )
                            val end = start.plusDays(6)

                            "${start.format(DateTimeFormatter.ofPattern("dd MMM"))} – " +
                                    end.format(DateTimeFormatter.ofPattern("dd MMM"))
                        }

                        UsagePeriod.MONTH -> selectedDate.format(
                            DateTimeFormatter.ofPattern(
                                "MMMM yyyy",
                                Locale.getDefault()
                            )
                        )
                    },
                    color = Color(0xFF203F42),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF203F42),
                    modifier = Modifier.clickable {
                        selectedDate = when (selectedPeriod) {
                            UsagePeriod.DAY -> selectedDate.plusDays(1)
                            UsagePeriod.WEEK -> selectedDate.plusWeeks(1)
                            UsagePeriod.MONTH -> selectedDate.plusMonths(1)
                        }
                        selectedIndex = 0
                    }
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Most-used apps",
                    color = Color(0xFFE6E6F0),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Ranked by screen time",
                    color = Color(0xFFA5A7B8),
                    fontSize = 11.sp
                )

                HistoryApps.forEach {
                    app ->
                    AppUsageRow(app=app)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    KaizenTheme(darkTheme = true) {
        HistoryScreen()
    }
}

