package me.ankitgrai.kaizen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import me.ankitgrai.kaizen.ui.components.KaizenPullToRefresh
import me.ankitgrai.kaizen.ui.components.AppUsageRow
import me.ankitgrai.kaizen.ui.components.CategoryFilter
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.viewmodel.UsageViewModel

private val SORT_OPTIONS = listOf(
    "Most Used",
    "Least Used",
    "Alphabetical"
)

@Composable
fun AppsScreen(
    usageViewModel: UsageViewModel = viewModel(),
    onAppClick: (String) -> Unit = {}
) {
    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var selectedSort by remember { mutableStateOf(SORT_OPTIONS[0]) }

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }
    val uiState by usageViewModel.uiState.collectAsState()
    val isRefreshing by usageViewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()
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
        selectedCategory,
        selectedSort
    ) {
        val matchingApps = uiState.installedApps.filter { app ->
            val matchesSearch = app.name.contains(
                searchQuery,
                ignoreCase = true
            ) || app.packageName.contains(
                searchQuery,
                ignoreCase = true
            )
            val matchesCategory = when(selectedCategory) {
                "All" -> true
                else -> app.category == selectedCategory
            }
            matchesSearch && matchesCategory
        }
        when (selectedSort) {
            SORT_OPTIONS[1] -> matchingApps.sortedBy { it.totalUsageMillis }
            SORT_OPTIONS[2] -> matchingApps.sortedBy { it.name.lowercase() }
            else -> matchingApps.sortedByDescending { it.totalUsageMillis }
        }

    }

    KaizenPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { usageViewModel.refreshAll() } },
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Applications",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Understand where your time goes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        item {
            UsageSummaryCard(
                totalUsageMillis = uiState.totalUsageMills,
                appsUsed = uiState.installedApps.count {
                    it.duration != "0m"
                }
            )
        }

        item {
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "YOUR APPS",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedSort,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable { sortMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SORT_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedSort = option
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                CategoryFilter(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
        }

        when {
            !uiState.hasUsageAccess && !uiState.isLoading -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Usage access is required to display app screen time.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { usageViewModel.openUsageAccessSettings() }) {
                        Text("Grant usage access")
                    }
                }
            }

            uiState.isLoading && uiState.installedApps.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.installedApps.isEmpty() -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = uiState.error ?: "Unable to load applications.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { usageViewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            }

            filteredApps.isEmpty() -> item {
                Text(
                    text = "No applications found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            else -> items(
                items = filteredApps,
                key = { it.packageName }
            ) { app ->
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

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = formattedUsage,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$appsUsed apps used",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "↓ 18%",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "vs yesterday",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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