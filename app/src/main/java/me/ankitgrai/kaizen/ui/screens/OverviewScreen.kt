package me.ankitgrai.kaizen.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.components.CircularUsageIndicator
import androidx.compose.ui.tooling.preview.Preview
import me.ankitgrai.kaizen.ui.theme.KaizenTheme

@Composable
fun OverviewScreen() {

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

            UsageSummary()

            // Weekly chart
            WeeklyUsageCard()

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
                    text = "Your top apps today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppUsageList()
            }
        }
    }

@Composable
fun UsageSummary(){
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
            Text(
                text = "4h 38m",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
        CircularUsageIndicator(
            size = 90.dp,
            strokeWidth = 12.dp,
            color = Color(0xFFB8F28B),
            progress = 0.77f
        )
    }
}

@Composable
fun WeeklyUsageCard() {
    data class DayUsage(
        val day: String,
        val hours: Float
    )

    val days = listOf(
        DayUsage("S", 3.2f),
        DayUsage("M", 4.1f),
        DayUsage("T", 5.0f),
        DayUsage("W", 3.8f),
        DayUsage("T", 4.7f),
        DayUsage("F", 4.63f),
        DayUsage("S", 2.1f)
    )

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
                days.forEachIndexed { index, day ->

                    val isToday = index == 5

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((day.hours / 6f * 76f).dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp
                                    )
                                )
                                .background(
                                    if (isToday) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color(0xFF454E43)
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = day.day,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isToday) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageList() {
    data class AppItem(
        val name: String,
        val category: String,
        val duration: String,
        val initials: String,
        val progress: Float,
        val tint: Color
    )

    val apps = listOf(
        AppItem(
            name = "YouTube",
            category = "Entertainment",
            duration = "1h 42m",
            initials = "YT",
            progress = 1f,
            tint = Color(0xFFFF8178)
        ),
        AppItem(
            name = "Instagram",
            category = "Social",
            duration = "58m",
            initials = "IG",
            progress = 0.57f,
            tint = Color(0xFFE6A0CF)
        ),
        AppItem(
            name = "Chrome",
            category = "Productivity",
            duration = "42m",
            initials = "CH",
            progress = 0.41f,
            tint = Color(0xFF8CB8FF)
        ),
        AppItem(
            name = "Spotify",
            category = "Music",
            duration = "31m",
            initials = "SP",
            progress = 0.30f,
            tint = Color(0xFFB8F28B)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 4.dp
            )
        ) {
            apps.forEachIndexed { index, app ->

                Column(
                    modifier = Modifier.padding(vertical = 14.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = app.tint.copy(alpha = 0.15f)
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.initials,
                                    color = app.tint,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = app.duration,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    UsageProgress(
                        progress = app.progress,
                        color = app.tint,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (index != apps.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
fun UsageProgress(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewScreenPreview() {
    KaizenTheme(darkTheme = true) {
        OverviewScreen()
    }
}

