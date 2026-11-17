package me.ankitgrai.kaizen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import me.ankitgrai.kaizen.data.reel.DailyReelCount
import me.ankitgrai.kaizen.domain.formatWeekdayAbbrev
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.ui.design.BackTopBar
import me.ankitgrai.kaizen.ui.design.BarChartStyle
import me.ankitgrai.kaizen.ui.design.BarEntry
import me.ankitgrai.kaizen.ui.design.SelectableBarChart
import me.ankitgrai.kaizen.ui.theme.chartSurfaceColor
import me.ankitgrai.kaizen.viewmodel.UsageViewModel

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
        BackTopBar(title = "Reel history", onBack = onBack, tintContent = false)
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
    val locale = LocalLocale.current.platformLocale
    val filled = remember(days, today) {
        val byDate = days.associateBy { it.date }
        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            byDate[date] ?: DailyReelCount(date = date, count = 0)
        }
    }

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
            SelectableBarChart(
                entries = filled.map { day ->
                    BarEntry(
                        id = day.date.toString(),
                        label = day.date.formatWeekdayAbbrev(locale),
                        value = day.count.toFloat(),
                        valueLabel = if (day.count > 0) day.count.toString() else null,
                        clickable = false,
                    )
                },
                selectedId = today.toString(),
                style = BarChartStyle.ReelWeek,
            )
        }
    }
}
