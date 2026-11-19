package me.ankitgrai.kaizen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.domain.formatDuration
import me.ankitgrai.kaizen.ui.components.AppSearchBar
import me.ankitgrai.kaizen.ui.components.AppUsageRow
import me.ankitgrai.kaizen.ui.components.CategoryFilter
import me.ankitgrai.kaizen.ui.components.CreateCategoryDialog
import me.ankitgrai.kaizen.ui.components.KaizenPullToRefresh
import me.ankitgrai.kaizen.ui.components.KaizenScrollItem
import me.ankitgrai.kaizen.ui.design.CenteredProgress
import me.ankitgrai.kaizen.ui.design.PromptMessage
import me.ankitgrai.kaizen.ui.design.ScreenHeader
import me.ankitgrai.kaizen.ui.design.TabScreenPadding
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

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var createCategoryError by remember { mutableStateOf<String?>(null) }

    val categories by usageViewModel.categories.collectAsStateWithLifecycle()
    val uiState by usageViewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by usageViewModel.isRefreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            errorMessage = createCategoryError,
            onDismiss = {
                showCreateCategoryDialog = false
                createCategoryError = null
            },
            onConfirm = { name ->
                usageViewModel.addCategory(name) { result ->
                    result.fold(
                        onSuccess = {
                            selectedCategory = name
                            showCreateCategoryDialog = false
                            createCategoryError = null
                        },
                        onFailure = { error ->
                            createCategoryError = error.message ?: "Failed to create category"
                        }
                    )
                }
            }
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        usageViewModel.refresh()
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

    val categoryTotalMillis = remember(uiState.installedApps, selectedCategory) {
        if (selectedCategory == "All") {
            uiState.totalUsageMillis
        } else {
            uiState.installedApps
                .filter { it.category.equals(selectedCategory, ignoreCase = true) }
                .sumOf { it.totalUsageMillis }
        }
    }

    val categoryAppsCount = remember(uiState.installedApps, selectedCategory) {
        if (selectedCategory == "All") {
            uiState.installedApps.size
        } else {
            uiState.installedApps.count { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val categoryPercentage = remember(categoryTotalMillis, uiState.totalUsageMillis) {
        if (uiState.totalUsageMillis > 0L) {
            ((categoryTotalMillis.toDouble() / uiState.totalUsageMillis) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    KaizenPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { usageViewModel.refreshAll() } },
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = TabScreenPadding,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeader(
                title = "Applications",
                subtitle = "Understand where your time goes.",
            )
        }

        item {
            UsageSummaryCard(
                totalUsageMillis = uiState.totalUsageMillis,
                yesterdayUsageMillis = uiState.yesterdayUsageMillis,
                appsUsed = uiState.installedApps.count { it.totalUsageMillis > 0L },
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
                    onCategorySelected = { selectedCategory = it },
                    categories = listOf("All") + categories,
                    onAddCategoryClick = {
                        createCategoryError = null
                        showCreateCategoryDialog = true
                    }
                )
                AnimatedVisibility(
                    visible = selectedCategory != "All",
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CategoryUsageSummary(
                        category = selectedCategory,
                        totalUsageMillis = categoryTotalMillis,
                        appCount = categoryAppsCount,
                        percentageOfTotal = categoryPercentage
                    )
                }
            }
        }

        when {
            !uiState.hasUsageAccess && !uiState.isLoading -> item {
                PromptMessage(
                    message = "Usage access is required to display app screen time.",
                    actionLabel = "Grant usage access",
                    onAction = { usageViewModel.openUsageAccessSettings() },
                )
            }

            uiState.isLoading && uiState.installedApps.isEmpty() -> item {
                CenteredProgress(modifier = Modifier.padding(vertical = 32.dp))
            }

            uiState.error != null && uiState.installedApps.isEmpty() -> item {
                PromptMessage(
                    message = uiState.error ?: "Unable to load applications.",
                    actionLabel = "Retry",
                    onAction = { usageViewModel.refresh() },
                    isError = true,
                )
            }

            filteredApps.isEmpty() -> item {
                Text(
                    text = "No applications found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            else -> itemsIndexed(
                items = filteredApps,
                key = { _, app -> app.packageName }
            ) { index, app ->
                KaizenScrollItem(index = index) {
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

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    }
}

@Composable
private fun UsageSummaryCard(
    totalUsageMillis: Long,
    yesterdayUsageMillis: Long?,
    appsUsed: Int
) {
    val formattedUsage = formatDuration(totalUsageMillis, alwaysShowHours = true)
    val comparison = remember(totalUsageMillis, yesterdayUsageMillis) {
        usageComparison(totalUsageMillis, yesterdayUsageMillis)
    }
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
                text = comparison.label,
                color = when (comparison.direction) {
                    UsageChangeDirection.Up -> MaterialTheme.colorScheme.error
                    UsageChangeDirection.Down -> MaterialTheme.colorScheme.primary
                    UsageChangeDirection.None -> MaterialTheme.colorScheme.onSurfaceVariant
                },
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

private enum class UsageChangeDirection {
    Up,
    Down,
    None
}

private data class UsageComparison(
    val label: String,
    val direction: UsageChangeDirection
)

private fun usageComparison(
    todayUsageMillis: Long,
    yesterdayUsageMillis: Long?
): UsageComparison {
    if (yesterdayUsageMillis == null || yesterdayUsageMillis == 0L) {
        return UsageComparison("—", UsageChangeDirection.None)
    }

    val percentage = (
        (todayUsageMillis - yesterdayUsageMillis).toDouble() /
            yesterdayUsageMillis * 100
        ).toInt()

    return when {
        percentage > 0 -> UsageComparison(
            label = "↑ $percentage%",
            direction = UsageChangeDirection.Up
        )
        percentage < 0 -> UsageComparison(
            label = "↓ ${-percentage}%",
            direction = UsageChangeDirection.Down
        )
        else -> UsageComparison("No change", UsageChangeDirection.None)
    }
}
@Composable
private fun CategoryUsageSummary(
    category: String,
    totalUsageMillis: Long,
    appCount: Int,
    percentageOfTotal: Int,
    modifier: Modifier = Modifier
) {
    val formattedDuration = formatDuration(totalUsageMillis, alwaysShowHours = true)
    val appCountText = if (appCount == 1) "1 app" else "$appCount apps"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "TOTAL FOR ${category.uppercase()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formattedDuration,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• $appCountText • $percentageOfTotal% of today",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 2.dp)
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

@Preview(showBackground = true)
@Composable
private fun CategoryUsageSummaryPreview() {
    KaizenTheme(darkTheme = true) {
        CategoryUsageSummary(
            category = "Social",
            totalUsageMillis = 8_100_000L,
            appCount = 3,
            percentageOfTotal = 45
        )
    }
}
