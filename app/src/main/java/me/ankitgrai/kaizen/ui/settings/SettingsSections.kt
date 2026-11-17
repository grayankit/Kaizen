package me.ankitgrai.kaizen.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.settings.formatScreenTimeTarget
import me.ankitgrai.kaizen.settings.snapTargetMinutes
import me.ankitgrai.kaizen.ui.components.AppIcon
import me.ankitgrai.kaizen.ui.components.roundedClickable
import me.ankitgrai.kaizen.ui.design.SelectableChip
import me.ankitgrai.kaizen.ui.design.SettingsActionRow
import me.ankitgrai.kaizen.ui.design.SettingsSwitchRow
import me.ankitgrai.kaizen.ui.theme.AppThemeId
import me.ankitgrai.kaizen.ui.theme.ThemeMode
import me.ankitgrai.kaizen.widget.WidgetSettings
import me.ankitgrai.kaizen.widget.widgetCornerRadius
import me.ankitgrai.kaizen.widget.widgetDayBackground
import me.ankitgrai.kaizen.widget.widgetDayContent
import me.ankitgrai.kaizen.widget.widgetDayMuted
import me.ankitgrai.kaizen.widget.widgetNightBackground
import me.ankitgrai.kaizen.widget.widgetNightContent
import me.ankitgrai.kaizen.widget.widgetNightMuted

@Composable
internal fun ScreenTimeTargetRow(
    minutes: Int,
    onClick: () -> Unit,
) {
    SettingsActionRow(
        title = "Maximum screen time",
        subtitle = "Daily target for the overview ring",
        action = formatScreenTimeTarget(minutes),
        onClick = onClick,
    )
}

@Composable
internal fun WidgetOpacityRow(
    percent: Int,
    onPercentChange: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Background opacity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Lets your wallpaper show through the widgets. Text stays solid.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$percent%",
                modifier = Modifier.padding(start = 12.dp),
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = percent.toFloat(),
            onValueChange = { raw ->
                val next = raw.roundToInt().coerceIn(
                    WidgetSettings.MIN_OPACITY_PERCENT,
                    WidgetSettings.MAX_OPACITY_PERCENT,
                )
                if (next != percent) onPercentChange(next)
            },
            valueRange = WidgetSettings.MIN_OPACITY_PERCENT.toFloat()..
                WidgetSettings.MAX_OPACITY_PERCENT.toFloat(),
            modifier = Modifier.semantics {
                contentDescription = "Widget background opacity"
            },
        )
        WidgetOpacityPreview(percent)
    }
}

@Composable
private fun WidgetOpacityPreview(percent: Int) {
    val night = isSystemInDarkTheme()
    val background = if (night) widgetNightBackground else widgetDayBackground
    val content = if (night) widgetNightContent else widgetDayContent
    val muted = if (night) widgetNightMuted else widgetDayMuted
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF6C63FF),
                        Color(0xFFB388FF),
                        Color(0xFFFF8FAB),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .clip(RoundedCornerShape(widgetCornerRadius))
                .background(background.copy(alpha = percent / 100f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "2h 15m",
                color = content,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "of 6h",
                color = muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun ThemeModeRow(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            SelectableChip(
                label = mode.label,
                selected = selected == mode,
                onClick = { onSelect(mode) },
                selectedContainer = scheme.primary,
                unselectedContainer = scheme.surfaceVariant,
                selectedContent = scheme.onPrimary,
                unselectedContent = scheme.onSurfaceVariant,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun OledRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsSwitchRow(
        title = "OLED black",
        subtitle = "Pure black background in dark mode",
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
internal fun ThemeOptionRow(
    theme: AppThemeId,
    selected: Boolean,
    onClick: () -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(preview)
                .border(1.dp, scheme.outline, CircleShape),
        )
        Text(
            text = theme.label,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
internal fun ScreenTimeTargetDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
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
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "The overview ring fills as today's usage approaches this target.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = snapTargetMinutes(it) },
                    valueRange = ScreenTimeSettings.MIN_TARGET_MINUTES.toFloat()..
                        ScreenTimeSettings.MAX_TARGET_MINUTES.toFloat(),
                    steps = ScreenTimeSettings.sliderSteps,
                    modifier = Modifier.semantics {
                        contentDescription = "Maximum screen time"
                    },
                )
                Text(
                    text = "${formatScreenTimeTarget(ScreenTimeSettings.MIN_TARGET_MINUTES)} to ${
                        formatScreenTimeTarget(ScreenTimeSettings.MAX_TARGET_MINUTES)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        },
    )
}

@Composable
internal fun ReelSetupBlock(
    serviceOn: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenBattery: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Counts swipes while Kaizen is closed. It does not read what is on screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        SettingsActionRow(
            title = "Accessibility service",
            subtitle = if (serviceOn) {
                "On"
            } else {
                "Off. Allow restricted settings: Apps, Kaizen, menu, then enable the service."
            },
            action = "Open",
            onClick = onOpenAccessibility,
        )
        SettingsActionRow(
            title = "Battery",
            subtitle = "Set use to Unrestricted",
            action = "Open",
            onClick = onOpenBattery,
        )
    }
}

@Composable
internal fun ReelAppRow(
    label: String,
    packageName: String,
    showPackage: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    SettingsSwitchRow(
        title = label,
        subtitle = if (showPackage) packageName else null,
        checked = checked,
        onCheckedChange = onCheckedChange,
        compactSubtitle = true,
        leading = {
            AppIcon(
                packageName = packageName,
                initials = label.firstOrNull()?.uppercase() ?: "?",
                color = scheme.primary,
                size = 40.dp,
            )
        },
    )
}

@Composable
private fun AppThemeId.previewColor() = when (this) {
    AppThemeId.Material -> MaterialTheme.colorScheme.primary
    else -> colorScheme(dark = true).primary
}
