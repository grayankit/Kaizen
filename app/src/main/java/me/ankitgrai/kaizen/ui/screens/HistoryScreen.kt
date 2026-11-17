package me.ankitgrai.kaizen.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.usage.DailyDeviceUsage
import me.ankitgrai.kaizen.data.usage.HourlyDeviceUsage
import me.ankitgrai.kaizen.domain.UsagePeriod
import me.ankitgrai.kaizen.domain.canAdvance
import me.ankitgrai.kaizen.domain.chartDates
import me.ankitgrai.kaizen.domain.formatDuration
import me.ankitgrai.kaizen.domain.formatHourRange
import me.ankitgrai.kaizen.domain.formatShortDayMonth
import me.ankitgrai.kaizen.domain.formatWeekdayDate
import me.ankitgrai.kaizen.domain.formatWeekdayShort
import me.ankitgrai.kaizen.domain.hourTickLabel
import me.ankitgrai.kaizen.domain.isFutureHour
import me.ankitgrai.kaizen.domain.periodLabel
import me.ankitgrai.kaizen.domain.shiftPeriod
import me.ankitgrai.kaizen.ui.components.KaizenPullToRefresh
import me.ankitgrai.kaizen.ui.components.PeriodSelector
import me.ankitgrai.kaizen.ui.design.BarChartStyle
import me.ankitgrai.kaizen.ui.design.BarEntry
import me.ankitgrai.kaizen.ui.design.CenteredProgress
import me.ankitgrai.kaizen.ui.design.MostUsedApps
import me.ankitgrai.kaizen.ui.design.ScreenHeader
import me.ankitgrai.kaizen.ui.design.SelectableBarChart
import me.ankitgrai.kaizen.ui.design.TabScreenPadding
import me.ankitgrai.kaizen.ui.design.UsageDetailCard
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.ui.theme.chartSurfaceColor
import me.ankitgrai.kaizen.viewmodel.DayHoursUiState
import me.ankitgrai.kaizen.viewmodel.HistoryUiState
import me.ankitgrai.kaizen.viewmodel.PeriodAppsUiState
import me.ankitgrai.kaizen.viewmodel.UsageViewModel

@Composable
fun HistoryScreen(
    usageViewModel: UsageViewModel = viewModel(),
) {
    val historyState by usageViewModel.historyState.collectAsStateWithLifecycle()
    val dayHoursState by usageViewModel.dayHoursState.collectAsStateWithLifecycle()
    val periodAppsState by usageViewModel.periodAppsState.collectAsStateWithLifecycle()
    val isRefreshing by usageViewModel.isRefreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        usageViewModel.refreshHistory()
    }

    HistoryContent(
        historyState = historyState,
        dayHoursState = dayHoursState,
        periodAppsState = periodAppsState,
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { usageViewModel.refreshAll() } },
        onLoadDayHours = usageViewModel::loadDayHours,
        onLoadMostUsedForPeriod = usageViewModel::loadMostUsedForPeriod,
    )
}

@Composable
fun HistoryContent(
    historyState: HistoryUiState,
    dayHoursState: DayHoursUiState = DayHoursUiState(),
    periodAppsState: PeriodAppsUiState = PeriodAppsUiState(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onLoadDayHours: (LocalDate) -> Unit = {},
    onLoadMostUsedForPeriod: (LocalDate, LocalDate) -> Unit = { _, _ -> },
) {
    val today = LocalDate.now()
    val locale = LocalLocale.current.platformLocale

    var selectedPeriod by remember { mutableStateOf(UsagePeriod.WEEK) }
    var periodDate by remember { mutableStateOf(today) }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedHour by remember(periodDate) {
        mutableIntStateOf(if (periodDate == today) LocalTime.now().hour else -1)
    }

    LaunchedEffect(selectedPeriod, periodDate, historyState.days) {
        if (selectedPeriod == UsagePeriod.DAY) {
            onLoadDayHours(periodDate)
        }
    }

    LaunchedEffect(periodDate, dayHoursState.date, dayHoursState.hours) {
        if (selectedPeriod != UsagePeriod.DAY) return@LaunchedEffect
        if (dayHoursState.date != periodDate || dayHoursState.hours.isEmpty()) return@LaunchedEffect
        if (selectedHour < 0) {
            selectedHour = dayHoursState.hours.maxBy { it.totalUsageMillis }.hour
        }
    }

    val hoursReady = dayHoursState.date == periodDate && !dayHoursState.isLoading
    val hoursAvailable = hoursReady && dayHoursState.available
    val selectedHourUsage = dayHoursState.hours.firstOrNull { it.hour == selectedHour }
    val historyByDate = historyState.days.associateBy { it.date }
    val chartDays = chartDates(selectedPeriod, periodDate).map { date ->
        historyByDate[date] ?: DailyDeviceUsage(
            date = date,
            totalUsageMillis = 0L,
            recorded = false,
        )
    }
    val periodStart = chartDays.first().date
    val periodEnd = chartDays.last().date.coerceAtMost(today)

    LaunchedEffect(periodStart, periodEnd) {
        onLoadMostUsedForPeriod(periodStart, periodEnd)
    }

    val periodAppsReady = periodAppsState.startDate == periodStart &&
        periodAppsState.endDate == periodEnd
    val selectedDay = chartDays.firstOrNull { it.date == selectedDate }
        ?: chartDays.lastOrNull { !it.date.isAfter(today) }
    val recordedDays = chartDays.filter {
        !it.date.isAfter(today) && historyByDate.containsKey(it.date)
    }
    val totalUsage = recordedDays.sumOf { it.totalUsageMillis }
    val averageUsage = if (recordedDays.isNotEmpty()) {
        totalUsage / recordedDays.size
    } else {
        0L
    }
    val label = periodLabel(periodDate, selectedPeriod, locale)

    KaizenPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TabScreenPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(
                title = "History",
                subtitle = "Understand where your time goes.",
            )

            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { period ->
                    selectedPeriod = period
                    periodDate = today
                    selectedDate = today
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(chartSurfaceColor())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = when (selectedPeriod) {
                        UsagePeriod.DAY -> "DAILY USAGE"
                        UsagePeriod.WEEK -> "WEEKLY USAGE"
                        UsagePeriod.MONTH -> "MONTHLY USAGE"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (historyState.isLoading && historyState.days.isEmpty()) {
                        "Loading..."
                    } else {
                        formatDuration(totalUsage)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        historyState.isLoading && historyState.days.isEmpty() ->
                            "Fetching device usage"
                        historyState.error != null -> historyState.error
                        recordedDays.isEmpty() ->
                            "No recorded usage available for this period"
                        else -> "Average per recorded day: ${formatDuration(averageUsage)}"
                    },
                    color = if (historyState.error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 11.sp,
                )
            }

            PeriodNavigation(
                label = label,
                canGoForward = canAdvance(periodDate, selectedPeriod, today),
                onPrevious = {
                    periodDate = shiftPeriod(periodDate, selectedPeriod, -1)
                    selectedDate = periodDate
                },
                onNext = {
                    periodDate = shiftPeriod(periodDate, selectedPeriod, 1)
                    selectedDate = periodDate
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(chartSurfaceColor())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = when (selectedPeriod) {
                        UsagePeriod.DAY -> "Usage by hour"
                        UsagePeriod.WEEK -> "Daily screen time"
                        UsagePeriod.MONTH -> "Usage by day"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                when {
                    historyState.isLoading && historyState.days.isEmpty() ->
                        CenteredProgress(height = 150.dp)

                    historyState.error != null -> Text(
                        text = "Couldn't load your usage history.\n${historyState.error}",
                        color = MaterialTheme.colorScheme.error,
                    )

                    selectedPeriod == UsagePeriod.DAY && !hoursReady ->
                        CenteredProgress(height = 150.dp)

                    selectedPeriod == UsagePeriod.DAY && !hoursAvailable -> Text(
                        text = "Hourly usage isn't available for this day.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    selectedPeriod == UsagePeriod.DAY -> {
                        HourlyUsageChart(
                            date = periodDate,
                            hours = dayHoursState.hours,
                            selectedHour = selectedHour,
                            today = today,
                            onHourSelected = { selectedHour = it },
                        )
                        if (selectedHour in 0..23) {
                            val recorded = selectedHourUsage != null &&
                                !isFutureHour(periodDate, selectedHour, today)
                            UsageDetailCard(
                                title = formatHourRange(selectedHour, locale),
                                subtitle = if (recorded) "Screen time" else "No usage yet",
                                value = if (recorded) {
                                    formatDuration(selectedHourUsage.totalUsageMillis)
                                } else {
                                    "—"
                                },
                            )
                        }
                    }

                    else -> {
                        HistoryUsageChart(
                            days = chartDays,
                            selectedDate = selectedDay?.date,
                            today = today,
                            period = selectedPeriod,
                            onDaySelected = { selectedDate = it },
                        )
                        selectedDay?.let { day ->
                            UsageDetailCard(
                                title = day.date.formatWeekdayDate(locale),
                                subtitle = when {
                                    !day.recorded -> "No usage data saved"
                                    day.date == today -> "Today's screen time"
                                    else -> "Screen time"
                                },
                                value = if (day.recorded) {
                                    formatDuration(day.totalUsageMillis)
                                } else {
                                    "—"
                                },
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(chartSurfaceColor())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Most used apps",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (selectedPeriod) {
                        UsagePeriod.DAY -> "Time used on ${periodDate.formatShortDayMonth(locale)}"
                        UsagePeriod.WEEK -> "Time used from $label"
                        UsagePeriod.MONTH -> "Time used in $label"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                MostUsedApps(
                    apps = periodAppsState.apps,
                    isLoading = !periodAppsReady || periodAppsState.isLoading,
                    emptyText = "No app usage recorded for this period.",
                    itemSpacing = 12.dp,
                    emptyFontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PeriodNavigation(
    label: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowLeft,
            contentDescription = "Previous period",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.clickable(onClick = onPrevious),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
            contentDescription = "Next period",
            tint = if (canGoForward) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            },
            modifier = Modifier.clickable(enabled = canGoForward, onClick = onNext),
        )
    }
}

@Composable
private fun HourlyUsageChart(
    date: LocalDate,
    hours: List<HourlyDeviceUsage>,
    selectedHour: Int,
    today: LocalDate,
    onHourSelected: (Int) -> Unit,
) {
    val byHour = hours.associateBy { it.hour }
    val locale = LocalLocale.current.platformLocale
    SelectableBarChart(
        entries = (0..23).map { hour ->
            val usage = byHour[hour]?.totalUsageMillis ?: 0L
            val future = isFutureHour(date, hour, today)
            BarEntry(
                id = hour.toString(),
                label = if (hour % 3 == 0 || hour == selectedHour) {
                    hourTickLabel(hour, locale)
                } else {
                    ""
                },
                value = usage.toFloat(),
                showBar = !future,
                clickable = !future,
            )
        },
        selectedId = selectedHour.toString(),
        onSelected = { onHourSelected(it.toInt()) },
        style = BarChartStyle.HistoryHours,
    )
}

@Composable
private fun HistoryUsageChart(
    days: List<DailyDeviceUsage>,
    selectedDate: LocalDate?,
    today: LocalDate,
    period: UsagePeriod,
    onDaySelected: (LocalDate) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    SelectableBarChart(
        entries = days.map { day ->
            val selected = day.date == selectedDate
            BarEntry(
                id = day.date.toString(),
                label = when (period) {
                    UsagePeriod.DAY -> day.date.formatShortDayMonth(locale)
                    UsagePeriod.WEEK -> day.date.formatWeekdayShort(locale)
                    UsagePeriod.MONTH -> if (day.date.dayOfMonth % 5 == 0 || selected) {
                        day.date.dayOfMonth.toString()
                    } else {
                        ""
                    }
                },
                value = day.totalUsageMillis.toFloat(),
                showBar = day.recorded,
                clickable = !day.date.isAfter(today),
            )
        },
        selectedId = selectedDate?.toString(),
        onSelected = { onDaySelected(LocalDate.parse(it)) },
        style = BarChartStyle.HistoryDays,
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val sampleDays = (0..6).map { offset ->
        DailyDeviceUsage(
            date = startOfWeek.plusDays(offset.toLong()),
            totalUsageMillis = (offset + 1) * 3_600_000L,
        )
    }

    KaizenTheme(darkTheme = true) {
        HistoryContent(
            historyState = HistoryUiState(
                isLoading = false,
                days = sampleDays,
            ),
        )
    }
}
