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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ankitgrai.kaizen.MainActivity
import me.ankitgrai.kaizen.R
import me.ankitgrai.kaizen.data.local.KaizenDatabase
import me.ankitgrai.kaizen.data.usage.UsageRepository
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.settings.formatScreenTimeTarget

class ScreenTimeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) { loadSnapshot(context) }
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
                openApp = openApp
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
    val repository = UsageRepository(context)
    val targetMillis = ScreenTimeSettings.readTargetMillis(context)
    if (!repository.hasUsageAccess()) {
        return WidgetSnapshot(
            hasAccess = false,
            totalMillis = 0L,
            targetMillis = targetMillis
        )
    }
    val today = LocalDate.now().toString()
    val total = KaizenDatabase.get(context)
        .usageHistoryDao()
        .deviceUsageBetween(today, today)
        .firstOrNull()
        ?.totalUsageMillis
        ?: 0L
    return WidgetSnapshot(
        hasAccess = true,
        totalMillis = total,
        targetMillis = targetMillis
    )
}

@Composable
private fun ScreenTimeWidgetContent(
    snapshot: WidgetSnapshot,
    ring: Bitmap?,
    grantAccess: String,
    openAppLabel: String,
    openApp: Intent
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(20.dp)
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
                text = formatWidgetDuration(snapshot.totalMillis),
                style = titleStyle
            )
            Text(
                text = "of ${formatScreenTimeTarget((snapshot.targetMillis / 60_000L).toInt())}",
                style = labelStyle
            )
        }
    }
}

private fun formatWidgetDuration(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
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
private val backgroundColor = ColorProvider(
    day = Color(0xFFF6F2FF),
    night = Color(0xFF1C1B22)
)
private val titleColor = ColorProvider(
    day = Color(0xFF1C1B22),
    night = Color(0xFFF6F2FF)
)
private val labelColor = ColorProvider(
    day = Color(0xFF6B6578),
    night = Color(0xFFB7B1C4)
)
private val titleStyle = TextStyle(
    color = titleColor,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold
)
private val labelStyle = TextStyle(
    color = labelColor,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium
)
private val messageStyle = TextStyle(
    color = titleColor,
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center
)
