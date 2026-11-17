package me.ankitgrai.kaizen.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ankitgrai.kaizen.AppGraph
import me.ankitgrai.kaizen.MainActivity
import me.ankitgrai.kaizen.R
import me.ankitgrai.kaizen.domain.formatDuration
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.settings.formatScreenTimeTarget

class ScreenTimeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) { loadSnapshot(context) }
        val opacity = WidgetSettings.readOpacity(context)
        val ring = if (snapshot.hasAccess) {
            usageRingBitmap(snapshot.progress)
        } else {
            null
        }
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        provideContent {
            ScreenTimeWidgetContent(
                snapshot = snapshot,
                ring = ring,
                grantAccess = context.getString(R.string.widget_grant_access),
                openAppLabel = context.getString(R.string.widget_open_app),
                openApp = openApp,
                opacity = opacity,
            )
        }
    }
}

private data class WidgetSnapshot(
    val hasAccess: Boolean,
    val totalMillis: Long,
    val targetMillis: Long
) {
    val progress: Float
        get() = (totalMillis.toFloat() / targetMillis.coerceAtLeast(1L)).coerceIn(0f, 1f)
}

private suspend fun loadSnapshot(context: Context): WidgetSnapshot {
    val repository = AppGraph.usageRepository(context)
    val targetMillis = ScreenTimeSettings.readTargetMillis(context)
    if (!repository.hasUsageAccess()) {
        return WidgetSnapshot(
            hasAccess = false,
            totalMillis = 0L,
            targetMillis = targetMillis,
        )
    }
    return WidgetSnapshot(
        hasAccess = true,
        totalMillis = repository.todayDeviceUsageMillis(),
        targetMillis = targetMillis,
    )
}

@Composable
private fun ScreenTimeWidgetContent(
    snapshot: WidgetSnapshot,
    ring: Bitmap?,
    grantAccess: String,
    openAppLabel: String,
    openApp: Intent,
    opacity: Float,
) {
    Box(modifier = GlanceModifier.widgetBackground(opacity)) {
        ScreenTimeWidgetBody(
            snapshot = snapshot,
            ring = ring,
            grantAccess = grantAccess,
            openAppLabel = openAppLabel,
            openApp = openApp,
        )
    }
}

@Composable
private fun ScreenTimeWidgetBody(
    snapshot: WidgetSnapshot,
    ring: Bitmap?,
    grantAccess: String,
    openAppLabel: String,
    openApp: Intent,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(openApp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!snapshot.hasAccess) {
            Text(
                text = grantAccess,
                style = messageStyle.copy(textAlign = TextAlign.Center)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = openAppLabel,
                style = labelStyle.copy(textAlign = TextAlign.Center)
            )
        } else {
            if (ring != null) {
                Image(
                    provider = ImageProvider(ring),
                    contentDescription = null,
                    modifier = GlanceModifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
            Text(
                text = formatDuration(snapshot.totalMillis),
                style = titleStyle
            )
            Text(
                text = "of ${formatScreenTimeTarget((snapshot.targetMillis / 60_000L).toInt())}",
                style = labelStyle
            )
        }
    }
}

private fun usageRingBitmap(progress: Float, sizePx: Int = 192): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val stroke = sizePx * 0.12f
    val inset = stroke / 2f + 1f
    val oval = RectF(inset, inset, sizePx - inset, sizePx - inset)
    val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = RING_COLOR.copy(alpha = 0.22f).toArgb()
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(oval, 0f, 360f, false, track)
    val sweep = 360f * progress.coerceIn(0f, 1f)
    if (sweep <= 0f) return bitmap
    val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = RING_COLOR.toArgb()
        strokeCap = if (sweep >= 359f) Paint.Cap.BUTT else Paint.Cap.ROUND
    }
    if (sweep >= 359f) {
        canvas.drawArc(oval, 0f, 360f, false, arc)
    } else {
        canvas.drawArc(oval, -90f, sweep, false, arc)
    }
    return bitmap
}

private val RING_COLOR = Color(0xFFB39DFF)
private val titleStyle = widgetTitleStyle(22.sp)
private val labelStyle = widgetLabelStyle()
private val messageStyle = widgetTitleStyle(16.sp, TextAlign.Center)
