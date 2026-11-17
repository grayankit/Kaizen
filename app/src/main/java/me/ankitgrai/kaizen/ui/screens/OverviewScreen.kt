package me.ankitgrai.kaizen.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.reel.ReelSetup
import me.ankitgrai.kaizen.data.usage.DailyDeviceUsage
import me.ankitgrai.kaizen.domain.formatDuration
import me.ankitgrai.kaizen.domain.formatShortWeekdayDate
import me.ankitgrai.kaizen.domain.formatWeekdayDate
import me.ankitgrai.kaizen.domain.formatWeekdayNarrow
import me.ankitgrai.kaizen.domain.weekDates
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.settings.formatScreenTimeTarget
import me.ankitgrai.kaizen.ui.components.CircularUsageIndicator
import me.ankitgrai.kaizen.ui.components.KaizenPullToRefresh
import me.ankitgrai.kaizen.ui.components.ReelCountCard
import me.ankitgrai.kaizen.ui.design.BarChartStyle
import me.ankitgrai.kaizen.ui.design.BarEntry
import me.ankitgrai.kaizen.ui.design.MostUsedApps
import me.ankitgrai.kaizen.ui.design.SelectableBarChart
import me.ankitgrai.kaizen.ui.design.TabScreenPadding
import me.ankitgrai.kaizen.ui.theme.chartSurfaceColor
import me.ankitgrai.kaizen.viewmodel.DayAppsUiState
import me.ankitgrai.kaizen.viewmodel.HistoryUiState
import me.ankitgrai.kaizen.viewmodel.UsageUiState
import me.ankitgrai.kaizen.viewmodel.UsageViewModel

@Composable
fun OverviewScreen(
    onAppClick: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSeeReelHistory: () -> Unit = {},
    usageViewModel: UsageViewModel = viewModel(),
) {
    val uiState by usageViewModel.uiState.collectAsStateWithLifecycle()
    val historyState by usageViewModel.historyState.collectAsStateWithLifecycle()
    val dayAppsState by usageViewModel.dayAppsState.collectAsStateWithLifecycle()
    val isRefreshing by usageViewModel.isRefreshing.collectAsStateWithLifecycle()
    val reelCountToday by usageViewModel.reelCountToday.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var countingEnabled by remember { mutableStateOf(ReelSetup.isEnabled(context)) }
    val targetUsageMillis = LocalScreenTimeSettings.current?.targetMillis
        ?: (ScreenTimeSettings.DEFAULT_TARGET_MINUTES * 60_000L)
    val reelApps = LocalScreenTimeSettings.current?.reelApps ?: emptySet()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        countingEnabled = ReelSetup.isEnabled(context)
        usageViewModel.refresh()
        usageViewModel.refreshHistory()
        usageViewModel.refreshReel(reelApps)
    }

    LaunchedEffect(selectedDate, historyState.days) {
        usageViewModel.loadMostUsed(selectedDate)
    }

    OverviewContent(
        uiState = uiState,
        historyState = historyState,
        dayAppsState = dayAppsState,
        isRefreshing = isRefreshing,
        selectedDate = selectedDate,
        reelCountToday = reelCountToday,
        countingEnabled = countingEnabled,
        onDateSelected = { selectedDate = it },
        onRefreshAll = { scope.launch { usageViewModel.refreshAll() } },
        onGrantAccess = { usageViewModel.openUsageAccessSettings() },
        onRefreshUsage = { usageViewModel.refresh() },
        onAppClick = onAppClick,
        onOpenSettings = onOpenSettings,
        onSeeReelHistory = onSeeReelHistory,
        targetUsageMillis = targetUsageMillis,
    )
}

@Composable
fun OverviewContent(
    modifier: Modifier = Modifier,
    uiState: UsageUiState,
    historyState: HistoryUiState,
    dayAppsState: DayAppsUiState,
    isRefreshing: Boolean,
    selectedDate: LocalDate,
    reelCountToday: Int = 0,
    countingEnabled: Boolean = false,
    onDateSelected: (LocalDate) -> Unit,
    onRefreshAll: () -> Unit,
    onGrantAccess: () -> Unit,
    onRefreshUsage: () -> Unit,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSeeReelHistory: () -> Unit = {},
    targetUsageMillis: Long = ScreenTimeSettings.DEFAULT_TARGET_MINUTES * 60_000L,
) {
    KaizenPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshAll,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TabScreenPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = getGreetingMessage(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = "Here's how you've spent your time today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            UsageSummary(
                totalUsageMillis = uiState.totalUsageMillis,
                isLoading = uiState.isLoading && uiState.totalUsageMillis == 0L,
                hasUsageAccess = uiState.hasUsageAccess,
                error = uiState.error,
                onGrantAccess = onGrantAccess,
                onRefresh = onRefreshUsage,
                targetUsageMillis = targetUsageMillis,
            )

            WeeklyUsageCard(
                days = historyState.days,
                selectedDate = selectedDate,
                onDaySelected = onDateSelected,
            )

            ReelCountCard(
                count = reelCountToday,
                countingEnabled = countingEnabled,
                onSeeHistory = onSeeReelHistory,
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Most used",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                val locale = LocalLocale.current.platformLocale
                val formattedDate = remember(selectedDate, locale) {
                    if (selectedDate == LocalDate.now()) {
                        "Your top apps today"
                    } else {
                        "Your top apps on ${selectedDate.formatShortWeekdayDate(locale)}"
                    }
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                MostUsedApps(
                    apps = dayAppsState.apps,
                    isLoading = dayAppsState.isLoading && dayAppsState.apps.isEmpty(),
                    emptyText = "No application usage recorded for this day.",
                    blockedText = if (!uiState.hasUsageAccess) {
                        "Grant usage access to see your applications."
                    } else {
                        null
                    },
                    onAppClick = { app ->
                        if (app.packageName.isNotBlank()) onAppClick(app.packageName)
                    },
                    animate = true,
                    compactLoading = true,
                )
            }
        }
    }
}

@Composable
fun UsageSummary(
    modifier: Modifier = Modifier,
    totalUsageMillis: Long,
    isLoading: Boolean,
    hasUsageAccess: Boolean,
    error: String?,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    targetUsageMillis: Long = ScreenTimeSettings.DEFAULT_TARGET_MINUTES * 60_000L,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "TODAY'S USAGE",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
            when {
                !hasUsageAccess -> {
                    Text(
                        text = "Access required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Button(onClick = onGrantAccess) {
                        Text("Grant access")
                    }
                }

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                error != null -> {
                    Text(
                        text = "Unable to load usage",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }

                else -> {
                    Text(
                        text = formatDuration(totalUsageMillis),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "of ${formatScreenTimeTarget((targetUsageMillis / 60_000L).toInt())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (hasUsageAccess && !isLoading && error == null) {
            CircularUsageIndicator(
                size = 90.dp,
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary,
                progress = (totalUsageMillis.toFloat() / targetUsageMillis.coerceAtLeast(1L)).coerceIn(0f, 1f),
            )
        }
    }
}

@Composable
fun WeeklyUsageCard(
    days: List<DailyDeviceUsage>,
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val locale = LocalLocale.current.platformLocale

    val (week, selectedDay) = remember(days, selectedDate, today) {
        val byDate = days.associateBy { it.date }
        val weekList = weekDates(today).map { date ->
            byDate[date] ?: DailyDeviceUsage(
                date = date,
                totalUsageMillis = 0L,
                recorded = false,
            )
        }
        val selected = weekList.firstOrNull { it.date == selectedDate }
            ?: weekList.last { !it.date.isAfter(today) }
        weekList to selected
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = chartSurfaceColor()),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "7 DAYS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
            }

            SelectableBarChart(
                entries = week.map { day ->
                    BarEntry(
                        id = day.date.toString(),
                        label = day.date.formatWeekdayNarrow(locale),
                        value = day.totalUsageMillis.toFloat(),
                        showBar = day.recorded,
                        clickable = !day.date.isAfter(today),
                        contentDescription = "${day.date.dayOfWeek.name}, ${formatDuration(day.totalUsageMillis)}",
                    )
                },
                selectedId = selectedDay.date.toString(),
                onSelected = { onDaySelected(LocalDate.parse(it)) },
                style = BarChartStyle.OverviewWeek,
            )

            Text(
                text = selectedDay.date.formatWeekdayDate(locale),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            AnimatedContent(
                targetState = selectedDay.date to selectedDay.totalUsageMillis,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                label = "selectedDayUsage",
            ) { (date, millis) ->
                val recorded = week.firstOrNull { it.date == date }?.recorded == true
                Text(
                    text = when {
                        date.isAfter(today) -> "No usage yet"
                        !recorded -> "No usage data saved"
                        else -> formatDuration(millis)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun getGreetingMessage(): String {
    return when (LocalTime.now().hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}
