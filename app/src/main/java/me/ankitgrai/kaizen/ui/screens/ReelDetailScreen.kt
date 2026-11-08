package me.ankitgrai.kaizen.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.ankitgrai.kaizen.data.reel.DailyReelCount
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.ui.theme.chartSurfaceColor
import me.ankitgrai.kaizen.viewmodel.UsageViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val BAR_MAX_HEIGHT = 120.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelDetailScreen(
    onBack: () -> Unit,
    usageViewModel: UsageViewModel = viewModel(),
) {
    val history by usageViewModel.reelHistory.collectAsStateWithLifecycle()
    val reelApps = LocalScreenTimeSettings.current?.reelApps ?: emptySet()

    LaunchedEffect(reelApps) {
        usageViewModel.refreshReel(reelApps)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = { Text("Reel history") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReelBarChart(days = history)
        }
    }
}

@Composable
private fun ReelBarChart(
    days: List<DailyReelCount>,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val filled = remember(days, today) {
        val byDate = days.associateBy { it.date }
        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            byDate[date] ?: DailyReelCount(date = date, count = 0)
        }
    }
    val maxCount = remember(filled) { filled.maxOf { it.count }.coerceAtLeast(1) }
    val labelFormatter = remember { DateTimeFormatter.ofPattern("EEE") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = chartSurfaceColor()),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Last 7 days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BAR_MAX_HEIGHT + 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                filled.forEach { day ->
                    val targetHeight = if (day.count > 0) {
                        (day.count.toFloat() / maxCount * BAR_MAX_HEIGHT.value).dp
                            .coerceAtLeast(4.dp)
                    } else {
                        4.dp
                    }
                    val barHeight by animateDpAsState(
                        targetValue = targetHeight,
                        animationSpec = tween(220),
                        label = "reelBar",
                    )
                    val isToday = day.date == today

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (day.count > 0) {
                            Text(
                                text = "${day.count}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = day.date.format(labelFormatter),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
