package me.ankitgrai.kaizen.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.ankitgrai.kaizen.data.usage.DailyDeviceUsage
import me.ankitgrai.kaizen.ui.components.PeriodSelector
import me.ankitgrai.kaizen.ui.components.UsagePeriod
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.viewmodel.UsageViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.tooling.preview.Preview
import me.ankitgrai.kaizen.ui.components.KaizenPullToRefresh
import me.ankitgrai.kaizen.ui.theme.chartSurfaceColor
import me.ankitgrai.kaizen.viewmodel.HistoryUiState



@Composable
fun HistoryScreen(
    usageViewModel: UsageViewModel = viewModel()
) {
    val historyState by usageViewModel.historyState.collectAsState()
    val isRefreshing by usageViewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        usageViewModel.refreshHistory()
    }

    HistoryContent(
        historyState = historyState,
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { usageViewModel.refreshAll() } }
    )
}

@Composable
fun HistoryContent(
    historyState: HistoryUiState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val today = LocalDate.now()

    var selectedPeriod by remember {
        mutableStateOf(UsagePeriod.WEEK)
    }

    var periodDate by remember {
        mutableStateOf(today)
    }

    var selectedDate by remember {
        mutableStateOf(today)
    }

    val historyByDate = historyState.days.associateBy {
        it.date
    }

    val chartDays = when (selectedPeriod) {
        UsagePeriod.DAY -> {
            listOf(periodDate)
        }

        UsagePeriod.WEEK -> {
            val start = periodDate.with(DayOfWeek.MONDAY)

            (0..6).map { offset ->
                start.plusDays(offset.toLong())
            }
        }

        UsagePeriod.MONTH -> {
            (1..periodDate.lengthOfMonth()).map { day ->
                periodDate.withDayOfMonth(day)
            }
        }
    }.map { date ->
        historyByDate[date] ?: DailyDeviceUsage(
            date = date,
            totalUsageMillis = 0L,
            recorded = false
        )
    }

    val selectedDay = chartDays.firstOrNull {
        it.date == selectedDate
    } ?: chartDays.lastOrNull {
        !it.date.isAfter(today)
    }

    val recordedDays = chartDays.filter {
        !it.date.isAfter(today) &&
                historyByDate.containsKey(it.date)
    }

    val totalUsage = recordedDays.sumOf {
        it.totalUsageMillis
    }

    val averageUsage = if (recordedDays.isNotEmpty()) {
        totalUsage / recordedDays.size
    } else {
        0L
    }

    val periodLabel = when (selectedPeriod) {
        UsagePeriod.DAY -> periodDate.format(
            DateTimeFormatter.ofPattern(
                "dd MMM yyyy",
                LocalLocale.current.platformLocale
            )
        )

        UsagePeriod.WEEK -> {
            val start = periodDate.with(DayOfWeek.MONDAY)
            val end = start.plusDays(6)

            "${start.format(DateTimeFormatter.ofPattern("dd MMM", LocalLocale.current.platformLocale))} – " +
                    end.format(
                        DateTimeFormatter.ofPattern(
                            "dd MMM",
                            LocalLocale.current.platformLocale
                        )
                    )
        }

        UsagePeriod.MONTH -> periodDate.format(
            DateTimeFormatter.ofPattern(
                "MMMM yyyy",
                LocalLocale.current.platformLocale
            )
        )
    }

    KaizenPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Screen heading
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "History",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Understand where your time goes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // Day, week, and month selector
        PeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { period ->
                selectedPeriod = period
                periodDate = today
                selectedDate = today
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Usage summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(chartSurfaceColor())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (selectedPeriod) {
                    UsagePeriod.DAY -> "DAILY USAGE"
                    UsagePeriod.WEEK -> "WEEKLY USAGE"
                    UsagePeriod.MONTH -> "MONTHLY USAGE"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (historyState.isLoading && historyState.days.isEmpty()) {
                    "Loading..."
                } else {
                    formatHistoryDuration(totalUsage)
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when {
                    historyState.isLoading && historyState.days.isEmpty() ->
                        "Fetching device usage"

                    historyState.error != null ->
                        historyState.error.orEmpty()

                    recordedDays.isEmpty() ->
                        "No recorded usage available for this period"
                    else ->
                        "Average per recorded day: ${
                            formatHistoryDuration(averageUsage)
                        }"
                },
                color = if (historyState.error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 11.sp
            )
        }

        // Period navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowLeft,
                contentDescription = "Previous period",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clickable {
                    periodDate = when (selectedPeriod) {
                        UsagePeriod.DAY -> periodDate.minusDays(1)
                        UsagePeriod.WEEK -> periodDate.minusWeeks(1)
                        UsagePeriod.MONTH -> periodDate.minusMonths(1)
                    }
                    selectedDate = periodDate
                }
            )

            Text(
                text = periodLabel,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            val nextDate = when (selectedPeriod) {
                UsagePeriod.DAY -> periodDate.plusDays(1)
                UsagePeriod.WEEK -> periodDate.plusWeeks(1)
                UsagePeriod.MONTH -> periodDate.plusMonths(1)
            }

            val canGoForward = !nextDate.isAfter(today)

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
                contentDescription = "Next period",
                tint = if (canGoForward) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                },
                modifier = Modifier.clickable(
                    enabled = canGoForward
                ) {
                    periodDate = nextDate
                    selectedDate = periodDate
                }
            )
        }

        // Daily usage chart
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(chartSurfaceColor())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when (selectedPeriod) {
                    UsagePeriod.DAY -> "Selected day"
                    UsagePeriod.WEEK -> "Daily screen time"
                    UsagePeriod.MONTH -> "Usage by day"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            when {
                historyState.isLoading && historyState.days.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                historyState.error != null -> {
                    Text(
                        text = "Couldn't load your usage history.\n" +
                                historyState.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    HistoryUsageChart(
                        days = chartDays,
                        selectedDate = selectedDay?.date,
                        today = today,
                        period = selectedPeriod,
                        onDaySelected = { date ->
                            selectedDate = date
                        }
                    )

                    selectedDay?.let { day ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(chartSurfaceColor(0.16f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = day.date.format(
                                    DateTimeFormatter.ofPattern(
                                        "EEEE, MMM d",
                                        LocalLocale.current.platformLocale
                                    )
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = when {
                                    !day.recorded -> "No usage data saved"
                                    day.date == today -> "Today's screen time"
                                    else -> "Screen time"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )

                            Text(
                                text = if (day.recorded) {
                                    formatHistoryDuration(day.totalUsageMillis)
                                } else {
                                    "—"
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
    }
}

@Composable
private fun HistoryUsageChart(
    days: List<DailyDeviceUsage>,
    selectedDate: LocalDate?,
    today: LocalDate,
    period: UsagePeriod,
    onDaySelected: (LocalDate) -> Unit
) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxUsage = days.maxOfOrNull {
        it.totalUsageMillis
    }?.coerceAtLeast(1L) ?: 1L

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val itemWidth = if (days.size <= 7) {
            maxWidth / days.size.coerceAtLeast(1)
        } else {
            42.dp
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top
        ) {
            days.forEach { day ->
                val selected = day.date == selectedDate
                val fraction = (
                        day.totalUsageMillis.toFloat() / maxUsage
                        ).coerceIn(0f, 1f)

                val targetHeight = if (day.recorded && day.totalUsageMillis > 0L) {
                    (130f * fraction).dp.coerceAtLeast(4.dp)
                } else {
                    4.dp
                }
                val barHeight by animateDpAsState(
                    targetValue = targetHeight,
                    animationSpec = tween(220),
                    label = "historyBar"
                )

                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) {
                                barColor.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable(
                            enabled = !day.date.isAfter(today)
                        ) {
                            onDaySelected(day.date)
                        }
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (day.recorded) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 7.dp,
                                            topEnd = 7.dp
                                        )
                                    )
                                    .background(
                                        if (selected) {
                                            barColor
                                        } else {
                                            barColor.copy(alpha = 0.65f)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val label = when (period) {
                        UsagePeriod.DAY -> day.date.format(
                            DateTimeFormatter.ofPattern(
                                "d MMM",
                                LocalLocale.current.platformLocale
                            )
                        )

                        UsagePeriod.WEEK -> day.date.format(
                            DateTimeFormatter.ofPattern(
                                "EE",
                                LocalLocale.current.platformLocale
                            )
                        )

                        UsagePeriod.MONTH -> {
                            if (
                                day.date.dayOfMonth % 5 == 0 ||
                                selected
                            ) {
                                day.date.dayOfMonth.toString()
                            } else {
                                ""
                            }
                        }
                    }

                    Text(
                        text = label,
                        modifier = Modifier.height(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            labelColor
                        },
                        fontSize = 10.sp,
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }
        }
    }
}

private fun formatHistoryDuration(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val sampleDays = (0..6).map { offset ->
        DailyDeviceUsage(
            date = startOfWeek.plusDays(offset.toLong()),
            totalUsageMillis = (offset + 1) * 3_600_000L
        )
    }

    KaizenTheme(darkTheme = true) {
        HistoryContent(
            historyState = HistoryUiState(
                isLoading = false,
                days = sampleDays
            )
        )
    }
}

