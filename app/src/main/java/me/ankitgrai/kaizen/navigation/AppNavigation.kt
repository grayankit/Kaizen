package me.ankitgrai.kaizen.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
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
    val showNavigationBar = currentRoute != Routes.APP_DETAILS && currentRoute != Routes.SETTINGS
    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.navigationBars)
    ) {
        innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        NavHost(
            navController = navController,
            startDestination = Routes.OVERVIEW,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { tabEnter(initialState, targetState) },
            exitTransition = { tabExit(initialState, targetState) },
            popEnterTransition = { tabEnter(initialState, targetState) },
            popExitTransition = { tabExit(initialState, targetState) }
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
        if (showNavigationBar) {
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            )
        }
        }
    }
}
private fun tabIndex(route: String?): Int? = when (route) {
    Routes.OVERVIEW -> 0
    Routes.APPS -> 1
    Routes.HISTORY -> 2
    else -> null
}

private fun tabEnter(
    initial: NavBackStackEntry,
    target: NavBackStackEntry
): EnterTransition {
    val from = tabIndex(initial.destination.route)
    val to = tabIndex(target.destination.route)
    if (from == null || to == null) return fadeIn(tween(220))
    val forward = to > from
    return slideInHorizontally(
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> if (forward) fullWidth / 5 else -fullWidth / 5 }
    ) + fadeIn(tween(200))
}

private fun tabExit(
    initial: NavBackStackEntry,
    target: NavBackStackEntry
): ExitTransition {
    val from = tabIndex(initial.destination.route)
    val to = tabIndex(target.destination.route)
    if (from == null || to == null) return fadeOut(tween(180))
    val forward = to > from
    return slideOutHorizontally(
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> if (forward) -fullWidth / 6 else fullWidth / 6 }
    ) + fadeOut(tween(160))
}

@Preview
@Composable
private fun AppNavigationPreview() {
    KaizenTheme(darkTheme = true) {
        AppNavigation()
    }
}

