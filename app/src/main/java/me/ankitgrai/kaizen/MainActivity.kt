package me.ankitgrai.kaizen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import me.ankitgrai.kaizen.navigation.AppNavigation
import me.ankitgrai.kaizen.settings.LocalScreenTimeSettings
import me.ankitgrai.kaizen.settings.ScreenTimeSettings
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.ui.theme.LocalThemeSettings
import me.ankitgrai.kaizen.ui.theme.ThemeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called BEFORE super.onCreate and setContent
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val themeSettings = remember { ThemeSettings(this) }
            val screenTimeSettings = remember { ScreenTimeSettings(this) }
            CompositionLocalProvider(
                LocalThemeSettings provides themeSettings,
                LocalScreenTimeSettings provides screenTimeSettings
            ) {
                KaizenTheme {
                    AppNavigation()
                }
            }
        }
    }
}
