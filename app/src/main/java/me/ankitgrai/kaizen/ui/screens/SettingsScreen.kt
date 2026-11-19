package me.ankitgrai.kaizen.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.reel.ReelApps
import me.ankitgrai.kaizen.data.reel.ReelSetup
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.ui.components.KaizenScrollItem
import me.ankitgrai.kaizen.ui.design.BackTopBar
import me.ankitgrai.kaizen.ui.design.SettingsActionRow
import me.ankitgrai.kaizen.ui.settings.OledRow
import me.ankitgrai.kaizen.ui.settings.ReelAppRow
import me.ankitgrai.kaizen.ui.settings.ReelSetupBlock
import me.ankitgrai.kaizen.ui.settings.ScreenTimeTargetDialog
import me.ankitgrai.kaizen.ui.settings.ScreenTimeTargetRow
import me.ankitgrai.kaizen.ui.settings.ThemeModeRow
import me.ankitgrai.kaizen.ui.settings.ThemeOptionRow
import me.ankitgrai.kaizen.ui.settings.WidgetOpacityRow
import me.ankitgrai.kaizen.ui.theme.AppThemeId
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.ui.theme.LocalThemeSettings
import me.ankitgrai.kaizen.ui.theme.ThemeSettings
import me.ankitgrai.kaizen.viewmodel.UsageViewModel
import me.ankitgrai.kaizen.widget.WidgetSettings
import me.ankitgrai.kaizen.widget.WidgetUpdater

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageCategories: () -> Unit = {},
    usageViewModel: UsageViewModel = viewModel(),
) {
    val settings = LocalThemeSettings.current ?: return
    val screenTime = LocalScreenTimeSettings.current
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTargetDialog by remember { mutableStateOf(false) }
    var widgetOpacity by remember { mutableIntStateOf(WidgetSettings.readOpacityPercent(context)) }
    var serviceOn by remember { mutableStateOf(ReelSetup.isEnabled(context)) }
    var reelChoices by remember { mutableStateOf(ReelApps.installedChoices(context)) }

    val categories by usageViewModel.categories.collectAsStateWithLifecycle()
    val customCategories by usageViewModel.customCategories.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        serviceOn = ReelSetup.isEnabled(context)
        val choices = ReelApps.installedChoices(context)
        reelChoices = choices
        screenTime?.pruneMissingReelApps(choices.map { it.packageName }.toSet())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(title = "Settings", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (screenTime != null) {
                item(key = "screen-time-heading") {
                    SectionHeading("Screen time")
                }
                item(key = "screen-time") {
                    KaizenScrollItem(index = 0) {
                        ScreenTimeTargetRow(
                            minutes = screenTime.targetMinutes,
                            onClick = { showTargetDialog = true },
                        )
                    }
                }

                item(key = "reel-heading") {
                    SectionHeading("Reel counter")
                }
                item(key = "reel-setup") {
                    ReelSetupBlock(
                        serviceOn = serviceOn,
                        onOpenAccessibility = { ReelSetup.openAccessibilitySettings(context) },
                        onOpenBattery = { ReelSetup.openBatterySettings(context) },
                    )
                }
                if (reelChoices.isEmpty()) {
                    item(key = "reel-apps-empty") {
                        Text(
                            text = "No reel apps installed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val sharedLabels = reelChoices
                        .groupingBy { it.label }
                        .eachCount()
                        .filterValues { it > 1 }
                        .keys
                    reelChoices.forEachIndexed { index, app ->
                        item(key = "reel-${app.packageName}") {
                            KaizenScrollItem(index = index) {
                                ReelAppRow(
                                    label = app.label,
                                    packageName = app.packageName,
                                    showPackage = app.label in sharedLabels,
                                    checked = app.packageName in screenTime.reelApps,
                                    onCheckedChange = { enabled ->
                                        val installed = reelChoices.map { it.packageName }.toSet()
                                        val base = screenTime.reelApps.filter { it in installed }.toSet()
                                        val updated = if (enabled) {
                                            base + app.packageName
                                        } else {
                                            base - app.packageName
                                        }
                                        screenTime.selectReelApps(updated)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "categories-heading") {
                SectionHeading("Categories")
            }
            item(key = "categories-manage") {
                KaizenScrollItem(index = 0) {
                    val customCount = customCategories.size
                    val totalCount = categories.size
                    SettingsActionRow(
                        title = "Manage categories",
                        subtitle = if (customCount > 0) "$customCount custom, ${totalCount - customCount} default" else "$totalCount default categories",
                        action = "Manage",
                        onClick = onManageCategories,
                    )
                }
            }

            item(key = "widgets-heading") {
                SectionHeading("Widgets")
            }
            item(key = "widget-opacity") {
                KaizenScrollItem(index = 0) {
                    WidgetOpacityRow(
                        percent = widgetOpacity,
                        onPercentChange = { percent ->
                            widgetOpacity = percent
                            WidgetSettings.writeOpacityPercent(context, percent)
                            WidgetUpdater.publishAppearance(context)
                        },
                    )
                }
            }

            item(key = "appearance-heading") {
                SectionHeading("Appearance")
            }
            item(key = "theme-mode") {
                KaizenScrollItem(index = 1) {
                    ThemeModeRow(selected = settings.mode, onSelect = settings::selectMode)
                }
            }
            item(key = "oled") {
                KaizenScrollItem(index = 2) {
                    OledRow(checked = settings.oled, onCheckedChange = settings::selectOled)
                }
            }
            item(key = "theme-heading") {
                SectionHeading("Theme")
            }
            item(key = "theme-description") {
                Text(
                    text = "Material You follows your wallpaper. The other palettes are the Dartotsu presets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            itemsIndexed(
                items = AppThemeId.entries,
                key = { _, theme -> theme.name },
            ) { index, theme ->
                KaizenScrollItem(index = index + 3) {
                    ThemeOptionRow(
                        theme = theme,
                        selected = settings.themeId == theme,
                        onClick = { settings.selectTheme(theme) },
                    )
                }
            }
        }
    }

    if (showTargetDialog && screenTime != null) {
        ScreenTimeTargetDialog(
            initialMinutes = screenTime.targetMinutes,
            onDismiss = { showTargetDialog = false },
            onConfirm = { minutes ->
                screenTime.selectTargetMinutes(minutes)
                showTargetDialog = false
                scope.launch { WidgetUpdater.update(context) }
            },
        )
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    val context = LocalContext.current
    val themeSettings = remember { ThemeSettings(context) }
    val screenTimeSettings = remember { ScreenTimeSettings(context) }
    CompositionLocalProvider(
        LocalThemeSettings provides themeSettings,
        LocalScreenTimeSettings provides screenTimeSettings,
    ) {
        KaizenTheme(dynamicColor = false) {
            SettingsScreen(onBack = {})
        }
    }
}
