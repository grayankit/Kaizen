package me.ankitgrai.kaizen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import me.ankitgrai.kaizen.navigation.AppNavigation

private val TempoColors = darkColorScheme(
    primary = Color(0xFFB8F28B),
    onPrimary = Color(0xFF182511),
    background = Color(0xFF101310),
    onBackground = Color(0xFFE6E9E2),
    surface = Color(0xFF1A1E1A),
    onSurface = Color(0xFFE6E9E2),
    surfaceVariant = Color(0xFF292E29),
    onSurfaceVariant = Color(0xFFA8B0A5),
    secondary = Color(0xFFB8F28B)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = TempoColors) {
                AppNavigation()
            }
        }
    }
}
