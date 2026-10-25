package me.ankitgrai.kaizen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import me.ankitgrai.kaizen.ui.components.AppSearchBar
import me.ankitgrai.kaizen.ui.components.AppUsageRow
import me.ankitgrai.kaizen.ui.components.CategoryFilter
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.viewmodel.UsageViewModel


@Composable
fun AppsScreen(
    usageViewModel: UsageViewModel = viewModel()
) {
    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }
    val uiState by usageViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, usageViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageViewModel.refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredApps = remember(
        uiState.installedApps,
        searchQuery,
        selectedCategory
    ) {
        uiState.installedApps.filter { app ->
            val matchesSearch = app.name.contains(
                searchQuery,
                ignoreCase = true
            )
            val matchesCategory = when(selectedCategory) {
                "All" -> true
                "Social" -> app.category == "Social"
                "Video" -> app.category.startsWith("Video")
                "Work" -> app.category == "Work"
                "Music" -> app.category == "Music"
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Applications",
                color = Color(0xFFE6E6F0),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Understand where your time goes.",
                color = Color(0xFFA5A7B8),
                fontSize = 12.sp
            )
        }

        UsageSummaryCard(
            totalUsageMillis = uiState.totalUsageMills,
            appsUsed = uiState.installedApps.count{
                it.duration != "0m"
            }
        )

        AppSearchBar(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "YOUR APPS",
                color = Color(0xFFA5A7B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Ranked by screen time",
                color = Color(0xFFA5A7B8),
                fontSize = 10.sp
            )

            CategoryFilter(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                }
            )
        }

        if (filteredApps.isEmpty()) {
            Text(
                text = "No applications found.",
                color = Color(0xFFA5A7B8),
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredApps.forEach { app ->
                    AppUsageRow(app = app)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun UsageSummaryCard(
    totalUsageMillis: Long,
    appsUsed: Int
) {
    val totalMinutes = totalUsageMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val formattedUsage = "${hours}h ${minutes}m"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "TOTAL USAGE TODAY",
                color = Color(0xFFA5A7B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = formattedUsage,
                color = Color(0xFFE6E6F0),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$appsUsed apps used",
                color = Color(0xFFA5A7B8),
                fontSize = 11.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "↓ 18%",
                color = Color(0xFF9DD5D9),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "vs yesterday",
                color = Color(0xFFA5A7B8),
                fontSize = 10.sp
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun AppsScreenPreview(){
    KaizenTheme(darkTheme = true) {
        AppsScreen()
    }
}