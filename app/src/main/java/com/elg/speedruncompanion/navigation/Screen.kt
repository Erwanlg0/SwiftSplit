package com.elg.speedruncompanion.navigation

/**
 * Type-safe navigation destinations for the app.
 */
sealed class Screen(val route: String) {
    data object RunsList : Screen("runs_list")
    data object Timer : Screen("timer/{runId}") {
        fun createRoute(runId: String): String = "timer/$runId"
    }
    data object SplitEditor : Screen("split_editor/{runId}") {
        fun createRoute(runId: String): String = "split_editor/$runId"
    }
    data object Remote : Screen("remote")
    data object Settings : Screen("settings")
    data object LayoutEditor : Screen("layout_editor")
    data object About : Screen("about")
}
