package me.ankitgrai.kaizen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import me.ankitgrai.kaizen.navigation.AppNavigation
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.ui.theme.LocalThemeSettings
import me.ankitgrai.kaizen.ui.theme.ThemeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeSettings = remember { ThemeSettings(this) }
            CompositionLocalProvider(LocalThemeSettings provides themeSettings) {
                KaizenTheme {
                    AppNavigation()
                }
            }
        }
    }
}
