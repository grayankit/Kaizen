package me.ankitgrai.kaizen.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

val LocalThemeSettings = staticCompositionLocalOf<ThemeSettings?> { null }

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun KaizenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val settings = LocalThemeSettings.current
    val dark = when (settings?.mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
        null -> darkTheme
    }
    val themeId = settings?.themeId ?: if (dynamicColor) AppThemeId.Material else AppThemeId.OneDark
    val context = LocalContext.current
    val colorScheme = when (themeId) {
        AppThemeId.Material if Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        AppThemeId.Material -> {
            if (dark) oneDarkDark() else oneDarkLight()
        }
        else -> themeId.colorScheme(dark)
    }
    val themed = when {
        dark && settings?.oled == true -> colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF1E1E1E),
            surfaceContainer = Color(0xFF161616),
            surfaceContainerHigh = Color(0xFF242424),
            surfaceContainerHighest = Color(0xFF2C2C2C)
        )
        dark -> colorScheme.copy(
            background = lerp(colorScheme.background, Color.White, 0.06f),
            surface = lerp(colorScheme.surface, Color.White, 0.06f)
        )
        else -> colorScheme
    }

    MaterialTheme(
        colorScheme = themed,
        typography = Typography,
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = themed.background
            ) {
                content()
            }
        }
    )
}
