package me.ankitgrai.kaizen.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ankitgrai.kaizen.MainActivity
import me.ankitgrai.kaizen.R
import me.ankitgrai.kaizen.data.reel.ReelRepository
import me.ankitgrai.kaizen.data.reel.ReelSetup
import me.ankitgrai.kaizen.settings.ScreenTimeSettings

class ReelCountWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetSettings.ensureLoaded(context)
        val snapshot = withContext(Dispatchers.IO) { loadSnapshot(context) }
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        provideContent {
            ReelCountWidgetContent(
                snapshot = snapshot,
                reelsToday = context.getString(R.string.widget_reels_today),
                countingOff = context.getString(R.string.widget_counting_off),
                openApp = openApp,
            )
        }
    }
}

private data class ReelWidgetSnapshot(
    val count: Int,
    val counting: Boolean,
)

private suspend fun loadSnapshot(context: Context): ReelWidgetSnapshot {
    val enabled = ScreenTimeSettings.readReelApps(context)
    val count = ReelRepository(context).todayTotal(enabled)
    return ReelWidgetSnapshot(
        count = count,
        counting = ReelSetup.isEnabled(context),
    )
}

@Composable
private fun ReelCountWidgetContent(
    snapshot: ReelWidgetSnapshot,
    reelsToday: String,
    countingOff: String,
    openApp: Intent,
) {
    Box(modifier = GlanceModifier.widgetBackground(widgetOpacity())) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(openApp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = snapshot.count.toString(),
                style = titleStyle,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = if (snapshot.counting) reelsToday else countingOff,
                style = labelStyle,
            )
        }
    }
}

private val titleStyle = widgetTitleStyle(28.sp, TextAlign.Center)
private val labelStyle = widgetLabelStyle(TextAlign.Center)
