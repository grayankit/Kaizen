package me.ankitgrai.kaizen.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.ZoneId
import me.ankitgrai.kaizen.data.usage.DailyAppUsage
import me.ankitgrai.kaizen.domain.formatDuration
import me.ankitgrai.kaizen.domain.formatWeekdayDate
import me.ankitgrai.kaizen.domain.formatWeekdayShort
import me.ankitgrai.kaizen.ui.components.AppIcon
import me.ankitgrai.kaizen.ui.components.AppUsage
import me.ankitgrai.kaizen.ui.design.BackTopBar
import me.ankitgrai.kaizen.ui.design.BarChartStyle
import me.ankitgrai.kaizen.ui.design.BarEntry
import me.ankitgrai.kaizen.ui.design.CenteredProgress
import me.ankitgrai.kaizen.ui.design.DetailCardLayout
import me.ankitgrai.kaizen.ui.design.SelectableBarChart
import me.ankitgrai.kaizen.ui.design.UsageDetailCard
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.viewmodel.AppDetailsUiState
import me.ankitgrai.kaizen.viewmodel.UsageUiState
import me.ankitgrai.kaizen.viewmodel.UsageViewModel

@Composable
fun AppDetailsScreen(
    packageName: String,
    onBack: () -> Unit,
    usageViewModel: UsageViewModel = viewModel(),
) {
    val uiState by usageViewModel.uiState.collectAsStateWithLifecycle()
    val detailsState by usageViewModel.appDetailsState.collectAsStateWithLifecycle()

    LaunchedEffect(packageName) {
        usageViewModel.loadAppDetails(packageName)
    }

    AppDetailsContent(
        packageName = packageName,
        onBack = onBack,
        uiState = uiState,
        detailsState = detailsState,
    )
}

@Composable
fun AppDetailsContent(
    packageName: String,
    onBack: () -> Unit,
    uiState: UsageUiState,
    detailsState: AppDetailsUiState,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val app = uiState.installedApps.firstOrNull { it.packageName == packageName }
    val todayUsage = app?.totalUsageMillis ?: 0L
    val totalUsage = uiState.totalUsageMillis
    val percentage = if (totalUsage > 0L) {
        todayUsage.toFloat() / totalUsage * 100f
    } else {
        0f
    }
    val today = LocalDate.now(ZoneId.systemDefault())
    val correctedHistory = detailsState.history.map { day ->
        if (day.date == today) {
            day.copy(totalTimeInForeground = todayUsage)
        } else {
            day
        }
    }
    var selectedDate by remember(packageName) { mutableStateOf<LocalDate?>(null) }
    val selectedDay = correctedHistory.firstOrNull { it.date == selectedDate }
        ?: correctedHistory.lastOrNull()
    val completedDays = correctedHistory.filter { it.date != today }
    val averageUsage = if (completedDays.isNotEmpty()) {
        completedDays.sumOf { it.totalTimeInForeground } / completedDays.size
    } else {
        0L
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = "${app?.name} details",
                onBack = onBack,
                useDefaultColors = true,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppIcon(
                    packageName = packageName,
                    initials = app?.initials ?: "?",
                    color = app?.color ?: MaterialTheme.colorScheme.primary,
                    size = 60.dp,
                )
                Column {
                    Text(
                        text = app?.name ?: packageName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = app?.category ?: "Application",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Today's Usage",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = formatDuration(todayUsage),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${percentage.toInt()}% of your total app usage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Last 7 days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    when {
                        detailsState.isLoading && detailsState.history.isEmpty() ->
                            CenteredProgress()

                        detailsState.error != null -> Text(
                            text = detailsState.error,
                            color = MaterialTheme.colorScheme.error,
                        )

                        detailsState.history.isEmpty() -> Text("No historical usage data available.")

                        else -> {
                            SelectableBarChart(
                                entries = correctedHistory.map { day ->
                                    BarEntry(
                                        id = day.date.toString(),
                                        label = day.date.formatWeekdayShort(locale),
                                        value = day.totalTimeInForeground.toFloat(),
                                    )
                                },
                                selectedId = selectedDay?.date?.toString(),
                                onSelected = { selectedDate = LocalDate.parse(it) },
                                style = BarChartStyle.AppWeek,
                            )
                            selectedDay?.let { day ->
                                UsageDetailCard(
                                    title = day.date.formatWeekdayDate(locale),
                                    subtitle = if (day.date == today) {
                                        "Today's screen time"
                                    } else {
                                        "Screen time"
                                    },
                                    value = formatDuration(day.totalTimeInForeground),
                                    layout = DetailCardLayout.Inline,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Average (previous 6 days): ${formatDuration(averageUsage)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:$packageName".toUri(),
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
private fun AppDetailsScreenPreview() {
    KaizenTheme(darkTheme = true) {
        AppDetailsContent(
            packageName = "com.google.android.youtube",
            onBack = {},
            uiState = UsageUiState(
                isLoading = false,
                hasUsageAccess = true,
                installedApps = listOf(
                    AppUsage(
                        name = "YouTube",
                        category = "Entertainment",
                        duration = "1h 42m",
                        initials = "YT",
                        color = Color(0xFFFF8178),
                        usageProgress = 0.35f,
                        packageName = "com.google.android.youtube",
                        totalUsageMillis = 6_120_000L,
                    ),
                ),
                totalUsageMillis = 18_000_000L,
            ),
            detailsState = AppDetailsUiState(
                isLoading = false,
                history = listOf(
                    DailyAppUsage(
                        date = LocalDate.now(),
                        packageName = "com.google.android.youtube",
                        totalTimeInForeground = 6_120_000L,
                    ),
                ),
            ),
        )
    }
}
