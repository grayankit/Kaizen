package me.ankitgrai.kaizen.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import me.ankitgrai.kaizen.ui.components.AppUsage
import me.ankitgrai.kaizen.ui.components.AppUsageRow

@Composable
fun MostUsedApps(
    apps: List<AppUsage>,
    isLoading: Boolean,
    emptyText: String,
    modifier: Modifier = Modifier,
    blockedText: String? = null,
    onAppClick: ((AppUsage) -> Unit)? = null,
    animate: Boolean = false,
    compactLoading: Boolean = false,
    itemSpacing: Dp = 10.dp,
    emptyFontSize: TextUnit = TextUnit.Unspecified,
) {
    when {
        blockedText != null -> Text(
            text = blockedText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        isLoading -> if (compactLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            CenteredProgress(height = 80.dp)
        }

        apps.isEmpty() -> Text(
            text = emptyText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = emptyFontSize,
        )

        animate -> AnimatedContent(
            targetState = apps,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "mostUsedApps",
        ) { shown ->
            AppList(shown, onAppClick, itemSpacing)
        }

        else -> AppList(apps, onAppClick, itemSpacing, modifier)
    }
}

@Composable
private fun AppList(
    apps: List<AppUsage>,
    onAppClick: ((AppUsage) -> Unit)?,
    itemSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        apps.forEach { app ->
            AppUsageRow(
                app = app,
                onClick = { onAppClick?.invoke(app) },
            )
        }
    }
}
