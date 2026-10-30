package me.ankitgrai.kaizen.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.usage.DailyDeviceUsage
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.settings.formatScreenTimeTarget
import me.ankitgrai.kaizen.ui.components.AppUsageRow
import me.ankitgrai.kaizen.ui.components.CircularUsageIndicator
import me.ankitgrai.kaizen.ui.components.KaizenPullToRefresh
import me.ankitgrai.kaizen.ui.theme.chartSurfaceColor
import me.ankitgrai.kaizen.viewmodel.DayAppsUiState
import me.ankitgrai.kaizen.viewmodel.HistoryUiState
import me.ankitgrai.kaizen.viewmodel.UsageUiState
import me.ankitgrai.kaizen.viewmodel.UsageViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val MAX_BAR_HEIGHT_DP = 76.dp

@Composable
fun OverviewScreen(
    onAppClick: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    usageViewModel: UsageViewModel = viewModel()
) {
    val uiState by usageViewModel.uiState.collectAsStateWithLifecycle()
    val historyState by usageViewModel.historyState.collectAsStateWithLifecycle()
    val dayAppsState by usageViewModel.dayAppsState.collectAsStateWithLifecycle()
    val isRefreshing by usageViewModel.isRefreshing.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val targetUsageMillis = LocalScreenTimeSettings.current?.targetMillis
        ?: ScreenTimeSettings.DEFAULT_TARGET_MINUTES * 60_000L

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        usageViewModel.refresh()
        usageViewModel.refreshHistory()
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
        onDateSelected = { selectedDate = it },
        onRefreshAll = { scope.launch { usageViewModel.refreshAll() } },
        onGrantAccess = { usageViewModel.openUsageAccessSettings() },
        onRefreshUsage = { usageViewModel.refresh() },
        onAppClick = onAppClick,
        onOpenSettings = onOpenSettings,
        targetUsageMillis = targetUsageMillis
    )
}

@Composable
fun OverviewContent(
    uiState: UsageUiState,
    historyState: HistoryUiState,
    dayAppsState: DayAppsUiState,
    isRefreshing: Boolean,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onRefreshAll: () -> Unit,
    onGrantAccess: () -> Unit,
    onRefreshUsage: () -> Unit,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    targetUsageMillis: Long = ScreenTimeSettings.DEFAULT_TARGET_MINUTES * 60_000L,
    modifier: Modifier = Modifier
) {
    KaizenPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshAll,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getGreetingMessage(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Here's how you've spent your time today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            UsageSummary(
                totalUsageMillis = uiState.totalUsageMills,
                isLoading = uiState.isLoading && uiState.totalUsageMills == 0L,
                hasUsageAccess = uiState.hasUsageAccess,
                error = uiState.error,
                onGrantAccess = onGrantAccess,
                onRefresh = onRefreshUsage,
                targetUsageMillis = targetUsageMillis
            )

            WeeklyUsageCard(
                days = historyState.days,
                selectedDate = selectedDate,
                onDaySelected = onDateSelected
            )

            // Most used applications
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Most used",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val locale = LocalLocale.current.platformLocale
                val formattedDate = remember(selectedDate, locale) {
                    if (selectedDate == LocalDate.now()) {
                        "Your top apps today"
                    } else {
                        "Your top apps on ${
                            selectedDate.format(
                                DateTimeFormatter.ofPattern("EEE, MMM d", locale)
                            )
                        }"
                    }
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {
                    !uiState.hasUsageAccess -> {
                        Text(
                            text = "Grant usage access to see your applications.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    dayAppsState.isLoading && dayAppsState.apps.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    dayAppsState.apps.isEmpty() -> {
                        Text(
                            text = "No application usage recorded for this day.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        AnimatedContent(
                            targetState = dayAppsState.apps,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                            label = "mostUsedApps"
                        ) { apps ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                apps.forEach { app ->
                                    AppUsageRow(
                                        app = app,
                                        onClick = {
                                            if (app.packageName.isNotBlank()) {
                                                onAppClick(app.packageName)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsageSummary(
    totalUsageMillis: Long,
    isLoading: Boolean,
    hasUsageAccess: Boolean,
    error: String?,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
    targetUsageMillis: Long = ScreenTimeSettings.DEFAULT_TARGET_MINUTES * 60_000L,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "TODAY'S USAGE",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            when {
                !hasUsageAccess -> {
                    Text(
                        text = "Access required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onGrantAccess) {
                        Text("Grant access")
                    }
                }

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                error != null -> {
                    Text(
                        text = "Unable to load usage",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }

                else -> {
                    Text(
                        text = formatOverviewDuration(totalUsageMillis),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "of ${formatScreenTimeTarget((targetUsageMillis / 60_000L).toInt())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (hasUsageAccess && !isLoading && error == null) {
            CircularUsageIndicator(
                size = 90.dp,
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary,
                progress = (totalUsageMillis.toFloat() / targetUsageMillis.coerceAtLeast(1L)).coerceIn(0f, 1f)
            )
        }
    }
}

@Composable
fun WeeklyUsageCard(
    days: List<DailyDeviceUsage>,
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    val (week, selectedDay, maxMillis) = remember(days, selectedDate) {
        val start = today.with(DayOfWeek.MONDAY)
        val byDate = days.associateBy { it.date }
        val weekList = (0..6).map { offset ->
            val date = start.plusDays(offset.toLong())
            byDate[date] ?: DailyDeviceUsage(
                date = date,
                totalUsageMillis = 0L,
                recorded = false
            )
        }
        val max = weekList.maxOf { it.totalUsageMillis }.coerceAtLeast(1L)
        val selected = weekList.firstOrNull { it.date == selectedDate }
            ?: weekList.last { !it.date.isAfter(today) }

        Triple(weekList, selected, max)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = chartSurfaceColor())
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "7 DAYS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                week.forEach { day ->
                    val selected = day.date == selectedDay.date
                    val label = day.date.dayOfWeek.getDisplayName(
                        TextStyle.NARROW,
                        LocalLocale.current.platformLocale
                    )
                    val targetHeight = if (day.recorded && day.totalUsageMillis > 0L) {
                        (day.totalUsageMillis.toFloat() / maxMillis * MAX_BAR_HEIGHT_DP.value).dp.coerceAtLeast(4.dp)
                    } else {
                        4.dp
                    }

                    val barHeight by animateDpAsState(
                        targetValue = targetHeight,
                        animationSpec = tween(220),
                        label = "overviewBar"
                    )

                    val isClickable = !day.date.isAfter(today)
                    val contentDesc = "${day.date.dayOfWeek.name}, ${formatOverviewDuration(day.totalUsageMillis)}"

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .semantics { contentDescription = contentDesc }
                            .clickable(enabled = isClickable) {
                                onDaySelected(day.date)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (day.recorded) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Text(
                text = selectedDay.date.format(
                    DateTimeFormatter.ofPattern("EEEE, MMM d", LocalLocale.current.platformLocale)
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            AnimatedContent(
                targetState = selectedDay.date to selectedDay.totalUsageMillis,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                label = "selectedDayUsage"
            ) { (date, millis) ->
                val recorded = week.firstOrNull { it.date == date }?.recorded == true
                Text(
                    text = when {
                        date.isAfter(today) -> "No usage yet"
                        !recorded -> "No usage data saved"
                        else -> formatOverviewDuration(millis)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatOverviewDuration(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun getGreetingMessage(): String {
    return when (LocalTime.now().hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

