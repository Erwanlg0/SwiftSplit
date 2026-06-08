package com.elg.speedruncompanion.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.elg.speedruncompanion.ui.screen.editor.SplitEditorScreen
import com.elg.speedruncompanion.ui.screen.layout.LayoutEditorScreen
import com.elg.speedruncompanion.ui.screen.remote.RemoteScreen
import com.elg.speedruncompanion.ui.screen.runs.RunsListScreen
import com.elg.speedruncompanion.ui.screen.settings.SettingsScreen
import com.elg.speedruncompanion.ui.screen.timer.TimerScreen

private const val TRANSITION_DURATION = 300

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RunsList.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        }
    ) {
        composable(Screen.RunsList.route) {
            RunsListScreen(
                onRunClick = { runId ->
                    navController.navigate(Screen.Timer.createRoute(runId))
                },
                onEditClick = { runId ->
                    navController.navigate(Screen.SplitEditor.createRoute(runId))
                },
                onRemoteClick = {
                    navController.navigate(Screen.Remote.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Timer.route,
            arguments = listOf(navArgument("runId") { type = NavType.StringType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getString("runId") ?: return@composable
            TimerScreen(
                runId = runId,
                onNavigateBack = { navController.popBackStack() },
                onEditSplits = { id ->
                    navController.navigate(Screen.SplitEditor.createRoute(id))
                },
                onEditLayout = {
                    navController.navigate(Screen.LayoutEditor.route)
                }
            )
        }

        composable(
            route = Screen.SplitEditor.route,
            arguments = listOf(navArgument("runId") { type = NavType.StringType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getString("runId") ?: return@composable
            SplitEditorScreen(
                runId = runId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Remote.route) {
            RemoteScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LayoutEditor.route) {
            LayoutEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
