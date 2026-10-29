package me.ankitgrai.kaizen.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

private object IconCache {
    private val bitmaps = mutableMapOf<String, Bitmap>()

    fun get(packageName: String): Bitmap? = bitmaps[packageName]

    fun put(packageName: String, bitmap: Bitmap) {
        bitmaps[packageName] = bitmap
    }
}

private fun Drawable.toIconBitmap(size: Int = 96): Bitmap {
    val bitmap = createBitmap(size, size)

    val canvas = Canvas(bitmap)

    val drawable = mutate()
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)

    return bitmap
}

@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    packageName: String,
    initials: String,
    color: Color,
    size: Dp = 44.dp,
) {
    val context = LocalContext.current

    val bitmapState = produceState<Bitmap?>(
        initialValue = null,
        key1 = packageName
    ) {
        value = IconCache.get(packageName) ?: withContext(Dispatchers.IO) {
            if (packageName.isBlank()) {
                null
            } else {
                try {
                    context.packageManager
                        .getApplicationIcon(packageName)
                        .toIconBitmap()
                        .also { IconCache.put(packageName, it) }
                } catch (exception: Exception) {
                    android.util.Log.w(
                        "KaizenAppIcon",
                        "Failed to load icon for $packageName",
                        exception
                    )
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = bitmapState.value

        if (bitmap != null) {
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = "$initials app icon",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = initials,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}