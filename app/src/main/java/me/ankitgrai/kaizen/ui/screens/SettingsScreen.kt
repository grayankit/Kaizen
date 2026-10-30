package me.ankitgrai.kaizen.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.settings.formatScreenTimeTarget
import me.ankitgrai.kaizen.settings.snapTargetMinutes
import me.ankitgrai.kaizen.ui.theme.AppThemeId
import me.ankitgrai.kaizen.ui.theme.LocalThemeSettings
import me.ankitgrai.kaizen.ui.theme.colorScheme
import me.ankitgrai.kaizen.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val settings = LocalThemeSettings.current ?: return
    val screenTime = LocalScreenTimeSettings.current
    val scheme = MaterialTheme.colorScheme
    var showTargetDialog by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (screenTime != null) {
                Text(
                    text = "Screen time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.surfaceVariant)
                        .clickable { showTargetDialog = true }
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
                        text = formatScreenTimeTarget(screenTime.targetMinutes),
                        modifier = Modifier.padding(start = 12.dp),
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = settings.mode == mode
                    Text(
                        text = mode.label,
                        color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) scheme.primary else scheme.surfaceVariant)
                            .clickable { settings.selectMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

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
                    checked = settings.oled,
                    onCheckedChange = settings::selectOled
                )
            }

            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Material You follows your wallpaper. The other palettes are the Dartotsu presets.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            AppThemeId.entries.forEach { theme ->
                val selected = settings.themeId == theme
                val preview = theme.previewColor()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) scheme.primaryContainer else scheme.surfaceVariant)
                        .clickable { settings.selectTheme(theme) }
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
        }
    }

    if (showTargetDialog && screenTime != null) {
        ScreenTimeTargetDialog(
            initialMinutes = screenTime.targetMinutes,
            onDismiss = { showTargetDialog = false },
            onConfirm = { minutes ->
                screenTime.selectTargetMinutes(minutes)
                showTargetDialog = false
            }
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
private fun AppThemeId.previewColor() = when (this) {
    AppThemeId.Material -> MaterialTheme.colorScheme.primary
    else -> colorScheme(dark = true).primary
}
