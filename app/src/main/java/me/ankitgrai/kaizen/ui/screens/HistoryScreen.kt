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
import androidx.compose.ui.graphics.Color
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
        mutableIntStateOf(defaultSelectedHour(periodDate, today))
    }
    val period = historyPeriod(
        days = historyState.days,
        selectedPeriod = selectedPeriod,
        periodDate = periodDate,
        selectedDate = selectedDate,
        today = today,
    )

    SyncHistoryLoads(
        selectedPeriod = selectedPeriod,
        periodDate = periodDate,
        historyDays = historyState.days,
        dayHoursState = dayHoursState,
        selectedHour = selectedHour,
        periodStart = period.periodStart,
        periodEnd = period.periodEnd,
        onLoadDayHours = onLoadDayHours,
        onLoadMostUsedForPeriod = onLoadMostUsedForPeriod,
        onSelectHour = { selectedHour = it },
    )

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
                onPeriodSelected = { periodChoice ->
                    selectedPeriod = periodChoice
                    periodDate = today
                    selectedDate = today
                },
                modifier = Modifier.fillMaxWidth(),
            )
            HistorySummaryCard(
                period = selectedPeriod,
                historyState = historyState,
                totalUsage = period.totalUsage,
                averageUsage = period.averageUsage,
                recordedEmpty = period.recordedDays.isEmpty(),
            )
            PeriodNavigation(
                label = periodLabel(periodDate, selectedPeriod, locale),
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
            HistoryChartCard(
                historyState = historyState,
                selectedPeriod = selectedPeriod,
                periodDate = periodDate,
                chartDays = period.chartDays,
                selectedDay = period.selectedDay,
                dayHoursState = dayHoursState,
                selectedHour = selectedHour,
                today = today,
                locale = locale,
                onHourSelected = { selectedHour = it },
                onDaySelected = { selectedDate = it },
            )
            HistoryMostUsedCard(
                selectedPeriod = selectedPeriod,
                periodDate = periodDate,
                periodLabel = periodLabel(periodDate, selectedPeriod, locale),
                periodAppsState = periodAppsState,
                periodStart = period.periodStart,
                periodEnd = period.periodEnd,
                locale = locale,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun defaultSelectedHour(periodDate: LocalDate, today: LocalDate): Int {
    return if (periodDate == today) LocalTime.now().hour else -1
}

private data class HistoryPeriod(
    val chartDays: List<DailyDeviceUsage>,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val selectedDay: DailyDeviceUsage?,
    val recordedDays: List<DailyDeviceUsage>,
    val totalUsage: Long,
    val averageUsage: Long,
)

private fun historyPeriod(
    days: List<DailyDeviceUsage>,
    selectedPeriod: UsagePeriod,
    periodDate: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
): HistoryPeriod {
    val historyByDate = days.associateBy { it.date }
    val chartDays = chartDates(selectedPeriod, periodDate).map { date ->
        historyByDate[date] ?: DailyDeviceUsage(
            date = date,
            totalUsageMillis = 0L,
            recorded = false,
        )
    }
    val recordedDays = chartDays.filter { recordedOn(it, today, historyByDate) }
    val totalUsage = recordedDays.sumOf { it.totalUsageMillis }
    return HistoryPeriod(
        chartDays = chartDays,
        periodStart = chartDays.first().date,
        periodEnd = chartDays.last().date.coerceAtMost(today),
        selectedDay = selectedChartDay(chartDays, selectedDate, today),
        recordedDays = recordedDays,
        totalUsage = totalUsage,
        averageUsage = averageUsage(recordedDays, totalUsage),
    )
}

private fun recordedOn(
    day: DailyDeviceUsage,
    today: LocalDate,
    historyByDate: Map<LocalDate, DailyDeviceUsage>,
): Boolean {
    return !day.date.isAfter(today) && historyByDate.containsKey(day.date)
}

private fun selectedChartDay(
    chartDays: List<DailyDeviceUsage>,
    selectedDate: LocalDate,
    today: LocalDate,
): DailyDeviceUsage? {
    return chartDays.firstOrNull { it.date == selectedDate }
        ?: chartDays.lastOrNull { !it.date.isAfter(today) }
}

private fun averageUsage(recordedDays: List<DailyDeviceUsage>, totalUsage: Long): Long {
    if (recordedDays.isEmpty()) return 0L
    return totalUsage / recordedDays.size
}

@Composable
private fun SyncHistoryLoads(
    selectedPeriod: UsagePeriod,
    periodDate: LocalDate,
    historyDays: List<DailyDeviceUsage>,
    dayHoursState: DayHoursUiState,
    selectedHour: Int,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    onLoadDayHours: (LocalDate) -> Unit,
    onLoadMostUsedForPeriod: (LocalDate, LocalDate) -> Unit,
    onSelectHour: (Int) -> Unit,
) {
    LaunchedEffect(selectedPeriod, periodDate, historyDays) {
        if (selectedPeriod == UsagePeriod.DAY) onLoadDayHours(periodDate)
    }
    LaunchedEffect(periodDate, dayHoursState.date, dayHoursState.hours, selectedPeriod) {
        selectBusiestHour(selectedPeriod, periodDate, dayHoursState, selectedHour, onSelectHour)
    }
    LaunchedEffect(periodStart, periodEnd) {
        onLoadMostUsedForPeriod(periodStart, periodEnd)
    }
}

private fun selectBusiestHour(
    selectedPeriod: UsagePeriod,
    periodDate: LocalDate,
    dayHoursState: DayHoursUiState,
    selectedHour: Int,
    onSelectHour: (Int) -> Unit,
) {
    if (selectedPeriod != UsagePeriod.DAY) return
    if (dayHoursState.date != periodDate || dayHoursState.hours.isEmpty()) return
    if (selectedHour >= 0) return
    onSelectHour(dayHoursState.hours.maxBy { it.totalUsageMillis }.hour)
}

@Composable
private fun HistorySummaryCard(
    period: UsagePeriod,
    historyState: HistoryUiState,
    totalUsage: Long,
    averageUsage: Long,
    recordedEmpty: Boolean,
) {
    val loadingEmpty = historyState.isLoading && historyState.days.isEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(chartSurfaceColor())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = usageHeading(period),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = summaryValue(loadingEmpty, totalUsage),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = summaryCaption(loadingEmpty, historyState.error, recordedEmpty, averageUsage),
            color = summaryCaptionColor(historyState.error),
            fontSize = 11.sp,
        )
    }
}

private fun usageHeading(period: UsagePeriod): String = when (period) {
    UsagePeriod.DAY -> "DAILY USAGE"
    UsagePeriod.WEEK -> "WEEKLY USAGE"
    UsagePeriod.MONTH -> "MONTHLY USAGE"
}

private fun summaryValue(loadingEmpty: Boolean, totalUsage: Long): String {
    return if (loadingEmpty) "Loading..." else formatDuration(totalUsage)
}

private fun summaryCaption(
    loadingEmpty: Boolean,
    error: String?,
    recordedEmpty: Boolean,
    averageUsage: Long,
): String {
    if (loadingEmpty) return "Fetching device usage"
    if (error != null) return error
    if (recordedEmpty) return "No recorded usage available for this period"
    return "Average per recorded day: ${formatDuration(averageUsage)}"
}

@Composable
private fun summaryCaptionColor(error: String?): Color {
    return if (error != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun HistoryChartCard(
    historyState: HistoryUiState,
    selectedPeriod: UsagePeriod,
    periodDate: LocalDate,
    chartDays: List<DailyDeviceUsage>,
    selectedDay: DailyDeviceUsage?,
    dayHoursState: DayHoursUiState,
    selectedHour: Int,
    today: LocalDate,
    locale: java.util.Locale,
    onHourSelected: (Int) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(chartSurfaceColor())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = chartHeading(selectedPeriod),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        HistoryChartBody(
            historyState = historyState,
            selectedPeriod = selectedPeriod,
            periodDate = periodDate,
            chartDays = chartDays,
            selectedDay = selectedDay,
            dayHoursState = dayHoursState,
            selectedHour = selectedHour,
            today = today,
            locale = locale,
            onHourSelected = onHourSelected,
            onDaySelected = onDaySelected,
        )
    }
}

private fun chartHeading(period: UsagePeriod): String = when (period) {
    UsagePeriod.DAY -> "Usage by hour"
    UsagePeriod.WEEK -> "Daily screen time"
    UsagePeriod.MONTH -> "Usage by day"
}

@Composable
private fun HistoryChartBody(
    historyState: HistoryUiState,
    selectedPeriod: UsagePeriod,
    periodDate: LocalDate,
    chartDays: List<DailyDeviceUsage>,
    selectedDay: DailyDeviceUsage?,
    dayHoursState: DayHoursUiState,
    selectedHour: Int,
    today: LocalDate,
    locale: java.util.Locale,
    onHourSelected: (Int) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
) {
    if (historyState.isLoading && historyState.days.isEmpty()) {
        CenteredProgress(height = 150.dp)
        return
    }
    val error = historyState.error
    if (error != null) {
        HistoryError(error)
        return
    }
    if (selectedPeriod == UsagePeriod.DAY) {
        DayHistoryBody(
            date = periodDate,
            hours = dayHoursState.hours,
            selectedHour = selectedHour,
            today = today,
            hoursReady = hoursReady(dayHoursState, periodDate),
            hoursAvailable = hoursAvailable(dayHoursState, periodDate),
            selectedHourUsage = dayHoursState.hours.firstOrNull { it.hour == selectedHour },
            locale = locale,
            onHourSelected = onHourSelected,
        )
    } else {
        PeriodHistoryBody(
            chartDays = chartDays,
            selectedDay = selectedDay,
            today = today,
            period = selectedPeriod,
            locale = locale,
            onDaySelected = onDaySelected,
        )
    }
}

@Composable
private fun HistoryError(message: String) {
    Text(
        text = "Couldn't load your usage history.\n$message",
        color = MaterialTheme.colorScheme.error,
    )
}

private fun hoursReady(state: DayHoursUiState, periodDate: LocalDate): Boolean {
    return state.date == periodDate && !state.isLoading
}

private fun hoursAvailable(state: DayHoursUiState, periodDate: LocalDate): Boolean {
    return hoursReady(state, periodDate) && state.available
}

@Composable
private fun DayHistoryBody(
    date: LocalDate,
    hours: List<HourlyDeviceUsage>,
    selectedHour: Int,
    today: LocalDate,
    hoursReady: Boolean,
    hoursAvailable: Boolean,
    selectedHourUsage: HourlyDeviceUsage?,
    locale: java.util.Locale,
    onHourSelected: (Int) -> Unit,
) {
    if (!hoursReady) {
        CenteredProgress(height = 150.dp)
        return
    }
    if (!hoursAvailable) {
        Text(
            text = "Hourly usage isn't available for this day.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    HourlyUsageChart(
        date = date,
        hours = hours,
        selectedHour = selectedHour,
        today = today,
        onHourSelected = onHourSelected,
    )
    if (selectedHour in 0..23) {
        HourDetail(date, selectedHour, selectedHourUsage, today, locale)
    }
}

@Composable
private fun HourDetail(
    date: LocalDate,
    hour: Int,
    usage: HourlyDeviceUsage?,
    today: LocalDate,
    locale: java.util.Locale,
) {
    val recorded = hourRecorded(date, hour, usage, today)
    UsageDetailCard(
        title = formatHourRange(hour, locale),
        subtitle = if (recorded) "Screen time" else "No usage yet",
        value = hourDetailValue(recorded, usage),
    )
}

private fun hourRecorded(
    date: LocalDate,
    hour: Int,
    usage: HourlyDeviceUsage?,
    today: LocalDate,
): Boolean {
    return usage != null && !isFutureHour(date, hour, today)
}

private fun hourDetailValue(recorded: Boolean, usage: HourlyDeviceUsage?): String {
    if (!recorded || usage == null) return "—"
    return formatDuration(usage.totalUsageMillis)
}

@Composable
private fun PeriodHistoryBody(
    chartDays: List<DailyDeviceUsage>,
    selectedDay: DailyDeviceUsage?,
    today: LocalDate,
    period: UsagePeriod,
    locale: java.util.Locale,
    onDaySelected: (LocalDate) -> Unit,
) {
    HistoryUsageChart(
        days = chartDays,
        selectedDate = selectedDay?.date,
        today = today,
        period = period,
        onDaySelected = onDaySelected,
    )
    val day = selectedDay ?: return
    UsageDetailCard(
        title = day.date.formatWeekdayDate(locale),
        subtitle = dayDetailSubtitle(day, today),
        value = dayDetailValue(day),
    )
}

private fun dayDetailSubtitle(day: DailyDeviceUsage, today: LocalDate): String = when {
    !day.recorded -> "No usage data saved"
    day.date == today -> "Today's screen time"
    else -> "Screen time"
}

private fun dayDetailValue(day: DailyDeviceUsage): String {
    return if (day.recorded) formatDuration(day.totalUsageMillis) else "—"
}

@Composable
private fun HistoryMostUsedCard(
    selectedPeriod: UsagePeriod,
    periodDate: LocalDate,
    periodLabel: String,
    periodAppsState: PeriodAppsUiState,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    locale: java.util.Locale,
) {
    val ready = periodAppsState.startDate == periodStart &&
        periodAppsState.endDate == periodEnd
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
            text = mostUsedSubtitle(selectedPeriod, periodDate, periodLabel, locale),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        MostUsedApps(
            apps = periodAppsState.apps,
            isLoading = !ready || periodAppsState.isLoading,
            emptyText = "No app usage recorded for this period.",
            itemSpacing = 12.dp,
            emptyFontSize = 12.sp,
        )
    }
}

private fun mostUsedSubtitle(
    period: UsagePeriod,
    periodDate: LocalDate,
    periodLabel: String,
    locale: java.util.Locale,
): String = when (period) {
    UsagePeriod.DAY -> "Time used on ${periodDate.formatShortDayMonth(locale)}"
    UsagePeriod.WEEK -> "Time used from $periodLabel"
    UsagePeriod.MONTH -> "Time used in $periodLabel"
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
