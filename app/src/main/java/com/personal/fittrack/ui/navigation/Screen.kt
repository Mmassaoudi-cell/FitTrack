package com.personal.fittrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Workout : Screen("workout")
    data object ExerciseList : Screen("workout/exercises")
    data object ActiveWorkout : Screen("workout/session/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout/session/$sessionId"
    }
    data object WorkoutHistory : Screen("workout/history")
    data object WorkoutHistoryDetail : Screen("workout/history/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout/history/$sessionId"
    }
    data object Nutrition : Screen("nutrition")
    data object AddFood : Screen("nutrition/add")
    data object FoodPhoto : Screen("nutrition/photo")
    data object Progress : Screen("progress")
    data object ExerciseProgress : Screen("progress/exercise/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "progress/exercise/$exerciseId"
    }
    data object Settings : Screen("settings")
}

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home),
    BottomNavItem(Screen.Workout, "Workout", Icons.Filled.FitnessCenter),
    BottomNavItem(Screen.Nutrition, "Nutrition", Icons.Filled.Restaurant),
    BottomNavItem(Screen.Progress, "Progress", Icons.Filled.ShowChart),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings)
)
