package com.personal.fittrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.personal.fittrack.ui.home.HomeScreen
import com.personal.fittrack.ui.nutrition.AddFoodScreen
import com.personal.fittrack.ui.nutrition.FoodPhotoScreen
import com.personal.fittrack.ui.nutrition.NutritionScreen
import com.personal.fittrack.ui.progress.ProgressScreen
import com.personal.fittrack.ui.settings.SettingsScreen
import com.personal.fittrack.ui.workout.ActiveWorkoutScreen
import com.personal.fittrack.ui.workout.ExerciseListScreen
import com.personal.fittrack.ui.workout.WorkoutHistoryDetailScreen
import com.personal.fittrack.ui.workout.WorkoutHistoryScreen
import com.personal.fittrack.ui.workout.WorkoutHubScreen

@Composable
fun FitTrackNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val entry by navController.currentBackStackEntryAsState()
            val route = entry?.destination?.route.orEmpty()
            if (!route.startsWith("workout/session/") && !route.startsWith("nutrition/add") && route != Screen.FoodPhoto.route) FitTrackBottomBar(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) { HomeScreen(
                onWorkout = { id -> navController.navigate(Screen.ActiveWorkout.createRoute(id)) },
                onFood = { navController.navigate(Screen.AddFood.route) },
                onWeight = { navController.navigate("body_weight") },
                onSettings = { navController.navigate(Screen.Settings.route) }
            ) }
            composable("body_weight") { ProgressScreen(initialTab = 1, openWeightDialog = true) }

            composable(Screen.Workout.route) {
                WorkoutHubScreen(
                    onStartWorkout = { sessionId -> navController.navigate(Screen.ActiveWorkout.createRoute(sessionId)) },
                    onOpenHistory = { navController.navigate(Screen.WorkoutHistory.route) },
                    onOpenExercises = { navController.navigate(Screen.ExerciseList.route) }
                )
            }
            composable(Screen.ExerciseList.route) { ExerciseListScreen(onBack = { navController.popBackStack() }) }
            composable(
                Screen.ActiveWorkout.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                ActiveWorkoutScreen(sessionId = sessionId, onEndWorkout = { if (!navController.popBackStack(Screen.Workout.route, false)) navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
            composable(Screen.WorkoutHistory.route) {
                WorkoutHistoryScreen(onOpenSession = { navController.navigate(Screen.WorkoutHistoryDetail.createRoute(it)) }, onBack = { navController.popBackStack() })
            }
            composable(
                Screen.WorkoutHistoryDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                WorkoutHistoryDetailScreen(sessionId = sessionId, onBack = { navController.popBackStack() })
            }

            composable(Screen.Nutrition.route) {
                NutritionScreen(
                    onAddFood = { date -> navController.navigate(Screen.AddFood.route + "?date=$date") },
                    onTakePhoto = { navController.navigate(Screen.FoodPhoto.route) }
                )
            }
            composable(Screen.AddFood.route + "?date={date}", arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })) { entry ->
                AddFoodScreen(onDone = { navController.popBackStack() }, date = entry.arguments?.getString("date")?.toLongOrNull())
            }
            composable(Screen.FoodPhoto.route) { FoodPhotoScreen(onDone = { navController.popBackStack() }) }

            composable(Screen.Progress.route) { ProgressScreen() }

            composable(Screen.Settings.route) { SettingsScreen(onRestored = {
                bottomNavItems.forEach { navController.clearBackStack(it.screen.route) }
                navController.navigate(Screen.Home.route) { popUpTo(navController.graph.id) { inclusive = true } }
            }) }
        }
    }
}

@Composable
private fun FitTrackBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute?.startsWith(item.screen.route) == true || (currentRoute == "body_weight" && item.screen == Screen.Progress),
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
