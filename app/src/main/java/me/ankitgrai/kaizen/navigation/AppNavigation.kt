package me.ankitgrai.kaizen.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.ankitgrai.kaizen.ui.components.CurrentScreen
import me.ankitgrai.kaizen.ui.components.FloatingNavigationBar
import me.ankitgrai.kaizen.ui.screens.AppDetailsScreen
import me.ankitgrai.kaizen.ui.screens.AppsScreen
import me.ankitgrai.kaizen.ui.screens.HistoryScreen
import me.ankitgrai.kaizen.ui.screens.OverviewScreen
import me.ankitgrai.kaizen.ui.screens.SettingsScreen
import me.ankitgrai.kaizen.ui.theme.KaizenTheme
import me.ankitgrai.kaizen.viewmodel.UsageViewModel

private object Routes{
    const val OVERVIEW = "overview"
    const val APPS = "apps"
    const val HISTORY = "history"
    const val APP_DETAILS = "app_details/{packageName}"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(){
    val navController  = rememberNavController()
    val usageViewModel: UsageViewModel = viewModel()
    val currentRoute = navController
        .currentBackStackEntryAsState()
        .value?.destination?.route

    val selectedDestination = when(currentRoute){
        Routes.APPS -> CurrentScreen.Apps
        Routes.HISTORY -> CurrentScreen.History
        else -> CurrentScreen.Overview
    }
    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            if(currentRoute != Routes.APP_DETAILS && currentRoute != Routes.SETTINGS) {
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
        }
    ) {
        innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OVERVIEW,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(220)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(220)) }
        ){
            composable (Routes.OVERVIEW){
                OverviewScreen(
                    onAppClick = { packageName ->
                        navController.navigate(
                            "app_details/${Uri.encode(packageName)}"
                        )
                    },
                    onOpenSettings = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.APPS) {
                AppsScreen(
                    usageViewModel = usageViewModel,
                    onAppClick = { packageName ->
                        navController.navigate(
                            "app_details/${Uri.encode(packageName)}"
                        )
                    }
                )
            }
            composable(
                route = Routes.APP_DETAILS,
                arguments = listOf(
                    navArgument("packageName") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val packageName = Uri.decode(
                    backStackEntry.arguments?.getString("packageName").orEmpty()
                )

                AppDetailsScreen(
                    packageName = packageName,
                    onBack = {
                        navController.popBackStack()
                    },
                    usageViewModel = usageViewModel
                )
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

