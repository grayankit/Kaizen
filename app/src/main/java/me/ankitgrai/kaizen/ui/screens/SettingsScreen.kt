package me.ankitgrai.kaizen.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import me.ankitgrai.kaizen.data.reel.ReelApps
import me.ankitgrai.kaizen.data.reel.ReelSetup
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.settings.formatScreenTimeTarget
import me.ankitgrai.kaizen.settings.snapTargetMinutes
import me.ankitgrai.kaizen.ui.components.AppIcon
import me.ankitgrai.kaizen.ui.components.KaizenScrollItem
import me.ankitgrai.kaizen.ui.components.roundedClickable
import me.ankitgrai.kaizen.ui.theme.AppThemeId
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.ui.theme.LocalThemeSettings
import me.ankitgrai.kaizen.ui.theme.ThemeSettings
import me.ankitgrai.kaizen.ui.theme.colorScheme
import me.ankitgrai.kaizen.ui.theme.ThemeMode
import me.ankitgrai.kaizen.widget.WidgetUpdater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val settings = LocalThemeSettings.current ?: return
    val screenTime = LocalScreenTimeSettings.current
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTargetDialog by remember { mutableStateOf(false) }
    var serviceOn by remember { mutableStateOf(ReelSetup.isEnabled(context)) }
    var reelChoices by remember { mutableStateOf(ReelApps.installedChoices(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        serviceOn = ReelSetup.isEnabled(context)
        val choices = ReelApps.installedChoices(context)
        reelChoices = choices
        screenTime?.pruneMissingReelApps(choices.map { it.packageName }.toSet())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = scheme.background,
                titleContentColor = scheme.onBackground,
                navigationIconContentColor = scheme.onBackground
            )
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (screenTime != null) {
                item(key = "screen-time-heading") {
                    Text(
                        text = "Screen time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item(key = "screen-time") {
                    KaizenScrollItem(index = 0) {
                        ScreenTimeTargetRow(
                            minutes = screenTime.targetMinutes,
                            onClick = { showTargetDialog = true }
                        )
                    }
                }

                item(key = "reel-heading") {
                    Text(
                        text = "Reel counter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item(key = "reel-setup") {
                    ReelSetupBlock(
                        serviceOn = serviceOn,
                        onOpenAccessibility = { ReelSetup.openAccessibilitySettings(context) },
                        onOpenBattery = { ReelSetup.openBatterySettings(context) },
                    )
                }
                item(key = "reel-apps-heading") {
                    Text(
                        text = "Count swipes in",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item(key = "reel-apps-description") {
                    Text(
                        text = "Patched apps are included when the package name contains Instagram, YouTube, or TikTok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (reelChoices.isEmpty()) {
                    item(key = "reel-apps-empty") {
                        Text(
                            text = "No Instagram, YouTube, or TikTok apps are installed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                } else {
                    reelChoices.forEachIndexed { index, app ->
                        item(key = "reel-${app.packageName}") {
                            KaizenScrollItem(index = index) {
                                ReelAppRow(
                                    label = app.label,
                                    packageName = app.packageName,
                                    checked = app.packageName in screenTime.reelApps,
                                    onCheckedChange = { enabled ->
                                        val installed = reelChoices.map { it.packageName }.toSet()
                                        val base = screenTime.reelApps.filter { it in installed }.toSet()
                                        val updated = if (enabled) base + app.packageName else base - app.packageName
                                        screenTime.selectReelApps(updated)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "appearance-heading") {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item(key = "theme-description") {
                Text(
                    text = "Material You follows your wallpaper. The other palettes are the Dartotsu presets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            itemsIndexed(
                items = AppThemeId.entries,
                key = { _, theme -> theme.name }
            ) { index, theme ->
                KaizenScrollItem(index = index + 3) {
                    ThemeOptionRow(
                        theme = theme,
                        selected = settings.themeId == theme,
                        onClick = { settings.selectTheme(theme) }
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
            }
        )
    }
}

@Composable
private fun ScreenTimeTargetRow(
    minutes: Int,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .roundedClickable(RoundedCornerShape(16.dp), onClick = onClick)
            .background(scheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Maximum screen time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Daily target for the overview ring",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Text(
            text = formatScreenTimeTarget(minutes),
            modifier = Modifier.padding(start = 12.dp),
            color = scheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ThemeModeRow(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Text(
                text = mode.label,
                color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier
                    .roundedClickable(RoundedCornerShape(50)) { onSelect(mode) }
                    .background(if (isSelected) scheme.primary else scheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun OledRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "OLED black",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Pure black background in dark mode",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemeOptionRow(
    theme: AppThemeId,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val preview = theme.previewColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .roundedClickable(RoundedCornerShape(16.dp), onClick = onClick)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(preview)
                .border(1.dp, scheme.outline, CircleShape)
        )
        Text(
            text = theme.label,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ScreenTimeTargetDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var minutes by remember(initialMinutes) { mutableIntStateOf(initialMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maximum screen time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = formatScreenTimeTarget(minutes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "The overview ring fills as today's usage approaches this target.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = snapTargetMinutes(it) },
                    valueRange = ScreenTimeSettings.MIN_TARGET_MINUTES.toFloat()..
                        ScreenTimeSettings.MAX_TARGET_MINUTES.toFloat(),
                    steps = ScreenTimeSettings.sliderSteps,
                    modifier = Modifier.semantics {
                        contentDescription = "Maximum screen time"
                    }
                )
                Text(
                    text = "${formatScreenTimeTarget(ScreenTimeSettings.MIN_TARGET_MINUTES)} to ${
                        formatScreenTimeTarget(ScreenTimeSettings.MAX_TARGET_MINUTES)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReelSetupBlock(
    serviceOn: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenBattery: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Kaizen counts each swipe to a new video in Instagram, YouTube, and TikTok. The counter runs while Kaizen is closed. It only hears scroll events from the apps you turn on. It does not read captions, names, or messages, and it does not block anything.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Accessibility service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (serviceOn) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (serviceOn) scheme.primary else scheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = "Android hides the accessibility switch until you allow restricted settings. Open Apps, Kaizen, menu, Allow restricted settings, then enable the service.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Button(
            onClick = onOpenAccessibility,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open accessibility settings")
        }
        Text(
            text = "Samsung stops counting unless battery use is Unrestricted. If this opens app info, tap Battery, then Unrestricted.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onOpenBattery,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open battery settings")
        }
    }
}

@Composable
private fun ReelAppRow(
    label: String,
    packageName: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            packageName = packageName,
            initials = label.firstOrNull()?.uppercase() ?: "?",
            color = scheme.primary,
            size = 40.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AppThemeId.previewColor() = when (this) {
    AppThemeId.Material -> MaterialTheme.colorScheme.primary
    else -> colorScheme(dark = true).primary
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
        LocalScreenTimeSettings provides screenTimeSettings
    ) {
        KaizenTheme(dynamicColor = false) {
            SettingsScreen(onBack = {})
        }
    }
}
