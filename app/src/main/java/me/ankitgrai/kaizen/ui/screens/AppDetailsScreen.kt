package me.ankitgrai.kaizen.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import me.ankitgrai.kaizen.data.usage.DailyAppUsage
import me.ankitgrai.kaizen.ui.components.AppIcon
import me.ankitgrai.kaizen.ui.components.AppUsage
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.viewmodel.AppDetailsUiState
import me.ankitgrai.kaizen.viewmodel.UsageUiState
import me.ankitgrai.kaizen.viewmodel.UsageViewModel
import androidx.core.net.toUri
import me.ankitgrai.kaizen.ui.components.WeeklyUsageChart
import java.time.ZoneId
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale
import java.time.format.DateTimeFormatter

@Composable
fun AppDetailsScreen(
    packageName: String,
    onBack: () -> Unit,
    usageViewModel: UsageViewModel = viewModel()
){

    val uiState by usageViewModel.uiState.collectAsState()
    val detailsState by usageViewModel.appDetailsState.collectAsState()

    LaunchedEffect(packageName) {
        usageViewModel.loadAppDetails(packageName)
    }

    AppDetailsContent(
        packageName = packageName,
        onBack = onBack,
        uiState = uiState,
        detailsState = detailsState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsContent(
    packageName: String,
    onBack: () -> Unit,
    uiState: UsageUiState,
    detailsState: AppDetailsUiState
){
    val context= LocalContext.current
    val app = uiState.installedApps.firstOrNull{
        it.packageName == packageName
    }
    val todayUsage = app?.totalUsageMillis ?: 0L
    val totalUsage = uiState.totalUsageMills
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
    var selectedDate by remember(packageName) {
        mutableStateOf<LocalDate?>(null)
    }

    val selectedDay = correctedHistory.firstOrNull {
        it.date == selectedDate
    } ?: correctedHistory.lastOrNull()

    val completedDays = correctedHistory.filter {
        it.date != today
    }

    val averageUsage = if (completedDays.isNotEmpty()) {
        completedDays.sumOf {
            it.totalTimeInForeground
        } / completedDays.size
    } else {
        0L
    }
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text("${app?.name} details")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) {innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIcon(
                    packageName = packageName,
                    initials = app?.initials ?: "?",
                    color = app?.color ?: MaterialTheme.colorScheme.primary,
                    size = 60.dp
                )
                Column {
                    Text(
                        text = app?.name ?: packageName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = app?.category ?: "Application",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)

            )
            {
                Column(
                    modifier = Modifier.padding(20.dp)
                )
                {
                    Text(
                        text = "Today's Usage",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = formatDuration(todayUsage),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${percentage.toInt()}% of your total app usage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            )
            {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                )
                {
                    Text(
                        text = "Last 7 days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    when {
                        detailsState.isLoading && detailsState.history.isEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        detailsState.error != null -> {
                            Text(
                                text = detailsState.error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        detailsState.history.isEmpty() -> {
                            Text("No historical usage data available.")
                        }

                        else -> {
                            WeeklyUsageChart(
                                history = correctedHistory,
                                selectedDate = selectedDay?.date,
                                onDaySelected = { date ->
                                    selectedDate = date
                                }
                            )
                            selectedDay?.let { day ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = day.date.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "EEEE, MMM d",
                                                        LocalLocale.current.platformLocale
                                                    )
                                                ),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Text(
                                                text = if (day.date == today) {
                                                    "Today's screen time"
                                                } else {
                                                    "Screen time"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = formatDuration(day.totalTimeInForeground),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Average (previous 6 days): ${formatDuration(averageUsage)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:$packageName".toUri()
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    null
                )
            }
        }

    }
}


private fun formatDuration(milliseconds: Long): String {
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
                        totalUsageMillis = 6_120_000L
                    )
                ),
                totalUsageMills = 18_000_000L
            ),
            detailsState = AppDetailsUiState(
                isLoading = false,
                history = listOf(
                    DailyAppUsage(
                        date = LocalDate.now(),
                        packageName = "com.google.android.youtube",
                        totalTimeInForeground = 6_120_000L
                    )
                )
            )
        )
    }
}

