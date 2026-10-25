package me.ankitgrai.kaizen.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.outlined.ArrowLeft
import androidx.compose.material.icons.materialIcon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import java.time.format.TextStyle
import java.util.Locale
import androidx.core.net.toUri

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


    val todayUsage = detailsState.history.lastOrNull()
        ?.totalTimeInForeground ?: 0L
    val totalUsage = uiState.totalUsageMills
    val percentage = if (totalUsage > 0L) {
        todayUsage.toFloat() / totalUsage * 100f
    } else {
        0f
    }
    val averageUsage = if(detailsState.history.isNotEmpty()){
        detailsState.history.sumOf {
            it.totalTimeInForeground
        }
    }else{
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
                        detailsState.isLoading -> {
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
                                history = detailsState.history
                            )
                            Spacer(Modifier.height(4.dp))

                            Text(
                                text  ="Average per day: ${formatDuration(averageUsage)}",
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

@Composable
private fun WeeklyUsageChart(
    history: List<DailyAppUsage>
) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxUsage = history.maxOfOrNull {
        it.totalTimeInForeground
    }?.coerceAtLeast(1L) ?: 1L

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val labelSpace = 28.dp.toPx()
        val chartHeight = size.height - labelSpace
        val gap = 10.dp.toPx()
        val barWidth = (
                (size.width - gap * (history.size - 1)) /
                        history.size
                ).coerceAtMost(36.dp.toPx())

        history.forEachIndexed { index, item ->
            val barHeight = (
                    item.totalTimeInForeground.toFloat() /
                            maxUsage
                    ) * chartHeight

            val x = index * (barWidth + gap) +
                    (size.width -
                            (history.size * barWidth +
                                    (history.size - 1) * gap)) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = x,
                    y = chartHeight - barHeight
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = barWidth,
                    height = barHeight.coerceAtLeast(3.dp.toPx())
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    8.dp.toPx()
                )
            )

            val label = item.date.dayOfWeek
                .getDisplayName(
                    TextStyle.SHORT,
                    Locale.getDefault()
                )
                .take(2)

            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2f,
                size.height - 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 11.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
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

