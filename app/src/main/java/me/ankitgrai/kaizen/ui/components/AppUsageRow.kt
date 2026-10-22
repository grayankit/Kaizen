package me.ankitgrai.kaizen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ankitgrai.kaizen.ui.theme.KaizenTheme

data class AppUsage(
    val name: String,
    val category: String,
    val duration: String,
    val initials: String,
    val color: Color,
    val usageProgress: Float
)

@Composable
fun AppUsageRow(
    app: AppUsage,
    modifier: Modifier = Modifier
) {
    val iconBackground = app.color.copy(alpha = 0.18f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0xFF171923))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ){
                Text(
                    text = app.initials,
                    color = app.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.name,
                    color = Color(0xFFE6E6F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = app.category,
                    color = Color(0xFFA5A7B8),
                    fontSize = 11.sp
                )
            }
            Text(
                text = app.duration,
                color = Color(0xFFE6E6F0),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF353844)))
        {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        app.usageProgress.coerceIn(0f,1f)
                    )
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(app.color)
            )
        }
    }
}


@Preview
@Composable

private fun PreviewAppUsageRow() {
    KaizenTheme(darkTheme = true) {
        AppUsageRow(
            app = AppUsage(
                name = "YouTube",
                category = "Entertainment",
                duration = "1h 42m",
                initials = "YT",
                color = Color(0xFFFF8178),
                usageProgress = 0.8f
            )
        )
    }
}