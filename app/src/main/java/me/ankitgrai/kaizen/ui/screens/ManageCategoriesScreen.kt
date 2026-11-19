package me.ankitgrai.kaizen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.ankitgrai.kaizen.domain.AppCategory
import me.ankitgrai.kaizen.ui.components.AppIcon
import me.ankitgrai.kaizen.ui.components.AppSearchBar
import me.ankitgrai.kaizen.ui.components.CreateCategoryDialog
import me.ankitgrai.kaizen.ui.components.SelectCategoryDialog
import me.ankitgrai.kaizen.ui.components.roundedClickable
import me.ankitgrai.kaizen.ui.design.BackTopBar
import me.ankitgrai.kaizen.ui.design.SelectableChip
import me.ankitgrai.kaizen.viewmodel.UsageViewModel

@Composable
fun ManageCategoriesScreen(
    onBack: () -> Unit,
    usageViewModel: UsageViewModel = viewModel(),
) {
    val scheme = MaterialTheme.colorScheme

    val categories by usageViewModel.categories.collectAsStateWithLifecycle()
    val customCategories by usageViewModel.customCategories.collectAsStateWithLifecycle()
    val overrides by usageViewModel.categoryOverrides.collectAsStateWithLifecycle()
    val uiState by usageViewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var showOnlyCustomOverrides by remember { mutableStateOf(false) }

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var createCategoryError by remember { mutableStateOf<String?>(null) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    // Dialog for picking a category for a specific app
    var selectedAppForCategoryPicker by remember { mutableStateOf<Pair<String, String>?>(null) } // (packageName, appName)

    val customCategoryNames = remember(customCategories) {
        customCategories.map { it.name }.toSet()
    }

    // App count by category
    val categoryAppCounts = remember(uiState.installedApps) {
        uiState.installedApps.groupingBy { it.category }.eachCount()
    }

    // Filter apps based on search, selected category chip, and custom override toggle
    val filteredApps = remember(
        uiState.installedApps,
        searchQuery,
        selectedFilterCategory,
        showOnlyCustomOverrides,
        overrides
    ) {
        uiState.installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.name.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedFilterCategory) {
                "All" -> true
                else -> app.category.equals(selectedFilterCategory, ignoreCase = true)
            }

            val matchesCustomOnly = !showOnlyCustomOverrides || overrides.containsKey(app.packageName)

            matchesSearch && matchesCategory && matchesCustomOnly
        }.sortedBy { it.name.lowercase() }
    }

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

    if (categoryToDelete != null) {
        val targetName = categoryToDelete!!
        val appCount = categoryAppCounts[targetName] ?: 0
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete '$targetName'?") },
            text = {
                Text(
                    if (appCount > 0) {
                        "This will remove the category. $appCount apps assigned to it will revert to their default category."
                    } else {
                        "Are you sure you want to delete this custom category?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        usageViewModel.deleteCategory(targetName)
                        if (selectedFilterCategory == targetName) {
                            selectedFilterCategory = "All"
                        }
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete", color = scheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedAppForCategoryPicker != null) {
        val (pkgName, appName) = selectedAppForCategoryPicker!!
        val currentCategory = uiState.installedApps.firstOrNull { it.packageName == pkgName }?.category
            ?: "Other"
        val hasOverride = overrides.containsKey(pkgName)

        SelectCategoryDialog(
            appName = appName,
            currentCategory = currentCategory,
            availableCategories = categories,
            hasCustomOverride = hasOverride,
            onCategorySelected = { cat ->
                usageViewModel.setAppCategory(pkgName, cat)
                selectedAppForCategoryPicker = null
            },
            onResetToDefault = {
                usageViewModel.resetAppCategory(pkgName)
                selectedAppForCategoryPicker = null
            },
            onCreateCategoryClick = {
                selectedAppForCategoryPicker = null
                createCategoryError = null
                showCreateCategoryDialog = true
            },
            onDismiss = { selectedAppForCategoryPicker = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        BackTopBar(
            title = "Manage Categories",
            onBack = onBack,
            useDefaultColors = true,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Category Strip with Create Action
            item(key = "categories-header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onBackground,
                        )
                        FilledTonalButton(
                            onClick = {
                                createCategoryError = null
                                showCreateCategoryDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Category",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Horizontal Category Pills with count and delete option for custom categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allCount = uiState.installedApps.size
                        CategoryFilterPill(
                            label = "All ($allCount)",
                            isSelected = selectedFilterCategory == "All",
                            onClick = { selectedFilterCategory = "All" }
                        )

                        categories.forEach { categoryName ->
                            val count = categoryAppCounts[categoryName] ?: 0
                            val isCustom = categoryName in customCategoryNames
                            val isSelected = selectedFilterCategory == categoryName

                            CategoryFilterPill(
                                label = "$categoryName ($count)",
                                isSelected = isSelected,
                                isCustom = isCustom,
                                onClick = { selectedFilterCategory = categoryName },
                                onDelete = if (isCustom) {
                                    { categoryToDelete = categoryName }
                                } else null
                            )
                        }
                    }
                }
            }

            // Search & Sub-Filter Row
            item(key = "search-filter-section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${filteredApps.size} apps found",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SelectableChip(
                                label = "Overridden Only",
                                selected = showOnlyCustomOverrides,
                                onClick = { showOnlyCustomOverrides = !showOnlyCustomOverrides },
                                selectedContainer = scheme.primaryContainer,
                                unselectedContainer = scheme.surfaceContainerHigh,
                                selectedContent = scheme.primary,
                                unselectedContent = scheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                selectedFontWeight = FontWeight.SemiBold,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Installed Apps List
            if (filteredApps.isEmpty()) {
                item(key = "empty-apps") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps match the selected criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    val hasOverride = overrides.containsKey(app.packageName)
                    AppCategoryManagementRow(
                        name = app.name,
                        packageName = app.packageName,
                        initials = app.initials,
                        color = app.color,
                        category = app.category,
                        hasCustomOverride = hasOverride,
                        onSelectCategory = {
                            selectedAppForCategoryPicker = app.packageName to app.name
                        },
                        onResetCategory = {
                            usageViewModel.resetAppCategory(app.packageName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterPill(
    label: String,
    isSelected: Boolean,
    isCustom: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh
    val contentColor = if (isSelected) scheme.primary else scheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .then(
                if (isCustom && !isSelected) {
                    Modifier.border(1.dp, scheme.outlineVariant, RoundedCornerShape(50))
                } else Modifier
            )
            .roundedClickable(RoundedCornerShape(50), onClick = onClick)
            .padding(
                start = 14.dp,
                end = if (onDelete != null) 4.dp else 14.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .roundedClickable(CircleShape, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Delete Category",
                    tint = scheme.error,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun AppCategoryManagementRow(
    name: String,
    packageName: String,
    initials: String,
    color: androidx.compose.ui.graphics.Color,
    category: String,
    hasCustomOverride: Boolean,
    onSelectCategory: () -> Unit,
    onResetCategory: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIcon(
                packageName = packageName,
                initials = initials,
                color = color,
                size = 42.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Interactive category badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (hasCustomOverride) scheme.primaryContainer else scheme.surfaceContainerHigh
                    )
                    .roundedClickable(RoundedCornerShape(10.dp), onClick = onSelectCategory)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasCustomOverride) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = "Change Category",
                    tint = if (hasCustomOverride) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Quick reset button if overridden
            if (hasCustomOverride) {
                IconButton(
                    onClick = onResetCategory,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset to auto-detected category",
                        tint = scheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
