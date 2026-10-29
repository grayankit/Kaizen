package me.ankitgrai.kaizen.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.components.CircularUsageIndicator
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import me.ankitgrai.kaizen.data.usage.DailyDeviceUsage
import me.ankitgrai.kaizen.ui.components.AppUsageRow
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.viewmodel.UsageViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalLocale

@Composable
fun OverviewScreen(
    onAppClick: (String) -> Unit = {}
) {
    val usageViewModel: UsageViewModel = viewModel()
    val uiState by usageViewModel.uiState.collectAsState()
    val historyState by usageViewModel.historyState.collectAsState()
    val dayAppsState by usageViewModel.dayAppsState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val lifecycleOwner  = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner,usageViewModel) {
        val observer = LifecycleEventObserver{_, event ->
            if(event == Lifecycle.Event.ON_RESUME){
                usageViewModel.refresh()
                usageViewModel.refreshHistory()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }



        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Header
            Column {
                Text(
                    text = "Good Evening",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )


                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Here's how you've spent your time today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            UsageSummary(
                totalUsageMillis = uiState.totalUsageMills,
                isLoading = uiState.isLoading,
                hasUsageAccess = uiState.hasUsageAccess,
                error = uiState.error,
                onGrantAccess = {
                    usageViewModel.openUsageAccessSettings()
                },
                onRefresh = {
                    usageViewModel.refresh()
                }
            )

            // Weekly chart
            WeeklyUsageCard(
                days = historyState.days,
                selectedDate = selectedDate,
                onDaySelected = { selectedDate = it }
            )

            LaunchedEffect(selectedDate, historyState.days) {
                usageViewModel.loadMostUsed(selectedDate)
            }

            // Most used applications
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Most used",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (selectedDate == LocalDate.now()) {
                        "Your top apps today"
                    } else {
                        "Your top apps on ${
                            selectedDate.format(
                                DateTimeFormatter.ofPattern(
                                    "EEE, MMM d",
                                    LocalLocale.current.platformLocale
                                )
                            )
                        }"
                    },
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            dayAppsState.apps.forEach { app ->
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

@Composable
fun UsageSummary(
    totalUsageMillis:Long,
    isLoading: Boolean,
    hasUsageAccess: Boolean,
    error: String?,
    onGrantAccess:() -> Unit,
    onRefresh:() -> Unit
){
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text  = "TODAY'S USAGE",
                style  = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            when{
                !hasUsageAccess ->{
                    Text(
                        text = "Access required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onGrantAccess){
                        Text("Grant access")
                    }
                }
                isLoading ->{
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                error != null ->{
                    Text(
                        text = "Unable to load usage",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
                else ->{
                    val totalMinutes = totalUsageMillis/60_000L
                    val hours = totalMinutes/60
                    val minutes = totalMinutes % 60

                    Text(
                        text = "${hours}h ${minutes}m",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        if(hasUsageAccess && !isLoading && error == null){
            CircularUsageIndicator(
                size = 90.dp,
                strokeWidth = 12.dp,
                color = Color(0xFFB8F28B),
                progress = (
                        totalUsageMillis.toFloat() / (6* 60  * 60 * 1000f)
                        ).coerceIn(0f,1f)
            )
        }

    }
}

@Composable
fun WeeklyUsageCard(
    days: List<DailyDeviceUsage>,
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit
) {

    val today = LocalDate.now()
    val start = today.with (DayOfWeek.MONDAY)
    val byDate = days.associateBy { it.date }
    val week = (0..6).map { offset ->
        val date = start.plusDays(offset.toLong())
        byDate[date] ?: DailyDeviceUsage(
            date = date,
            totalUsageMillis = 0L,
            recorded = false
        )
    }
    val maxMillis = week.maxOf { it.totalUsageMillis }.coerceAtLeast(1L)
    val selectedDay = week.firstOrNull { it.date == selectedDate }
        ?: week.last { !it.date.isAfter(today) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    val barHeight = if (day.recorded && day.totalUsageMillis > 0L) {
                        (day.totalUsageMillis.toFloat() / maxMillis * 76f).dp
                            .coerceAtLeast(4.dp)
                    } else {
                        4.dp
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !day.date.isAfter(today)) {
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
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 8.dp,
                                            topEnd = 8.dp
                                        )
                                    )
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color(0xFF454E43)
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
                            fontWeight = if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                }
            }

            Text(
                text = selectedDay.date.format(
                    DateTimeFormatter.ofPattern(
                        "EEEE, MMM d",
                        LocalLocale.current.platformLocale
                    )
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    selectedDay.date.isAfter(today) -> "No usage yet"
                    !selectedDay.recorded -> "No usage data saved"
                    else -> formatOverviewDuration(selectedDay.totalUsageMillis)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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

@Preview(showBackground = true)
@Composable
private fun OverviewScreenPreview() {
    KaizenTheme(darkTheme = true) {
        OverviewScreen()
    }
}

