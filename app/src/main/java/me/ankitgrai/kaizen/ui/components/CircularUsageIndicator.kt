package me.ankitgrai.kaizen.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable

fun CircularUsageIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 12.dp,
    color: Color = Color(0xFFB39DFF),
    progress: Float  =1f
){
    Canvas(
        modifier = modifier.size(size)
    ) {
        val stroke  = strokeWidth.toPx()
        val diameter  = this.size.minDimension  - stroke
        val topLeft = Offset(
            x  = (this.size.width-diameter)/2,
            y  = (this.size.height - diameter)/2
        )
        drawArc(
            color  = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f,1f),
            useCenter = false,
            topLeft  = topLeft,
            size = Size(diameter,diameter),
            style = Stroke(width = stroke,
                cap = StrokeCap.Butt)
        )
    }
}
@Preview(showBackground = true)
@Composable
private fun ScreenTimeDashboardPreview() {
    MaterialTheme() {
        CircularUsageIndicator()
    }
}