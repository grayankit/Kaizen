package me.ankitgrai.kaizen.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.ankitgrai.kaizen.ui.components.CurrentScreen
import me.ankitgrai.kaizen.ui.components.FloatingNavigationBar
import me.ankitgrai.kaizen.ui.screens.AppsScreen
import me.ankitgrai.kaizen.ui.screens.HistoryScreen
import me.ankitgrai.kaizen.ui.screens.OverviewScreen
import me.ankitgrai.kaizen.ui.theme.KaizenTheme

private object Routes{
    const val OVERVIEW = "overview"
    const val APPS = "apps"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation(){
    val navController  = rememberNavController()
    val currentRoute = navController
        .currentBackStackEntryAsState()
        .value?.destination?.route

    val selectedDestination = when(currentRoute){
        Routes.APPS -> CurrentScreen.Apps
        Routes.HISTORY -> CurrentScreen.History
        else -> CurrentScreen.Overview
    }
    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingNavigationBar(
                    selectedDestination = selectedDestination,
                    onScreenSelected = { destination ->
                        val route = when (destination) {
                            CurrentScreen.Overview -> Routes.OVERVIEW
                            CurrentScreen.Apps -> Routes.APPS
                            CurrentScreen.History -> Routes.HISTORY
                        }
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Routes.OVERVIEW) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OVERVIEW,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ){
            composable (Routes.OVERVIEW){
                OverviewScreen()
            }
            composable (Routes.APPS){
                AppsScreen()
            }
            composable (Routes.HISTORY){
                HistoryScreen()
            }
    }
    }
}
@Preview
@Composable
private fun AppNavigationPreview() {
    KaizenTheme(darkTheme = true) {
        AppNavigation()
    }
}

