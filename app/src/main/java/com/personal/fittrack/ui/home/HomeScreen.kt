package com.personal.fittrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.ui.components.StatCard
import kotlin.math.roundToInt

@Composable
fun HomeScreen() {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val container = app.container
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory {
        initializer {
            HomeViewModel(
                container.workoutRepository,
                container.nutritionRepository,
                container.bodyWeightRepository,
                container.userPreferences
            )
        }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Today") }) }) { padding ->
        val cards = listOf(
            "Exercises" to state.totalExercises.toString(),
            "Sets" to state.totalSets.toString(),
            "Reps" to state.totalReps.toString(),
            "Volume" to "${state.totalVolumeKg.roundToInt()} kg",
            "Calories consumed" to "${state.caloriesConsumed.roundToInt()} kcal",
            "Est. calories burned" to "${state.caloriesBurnedEstimate.roundToInt()} kcal",
            "Remaining allowance" to "${state.remainingCalories.roundToInt()} kcal",
            "Calorie target" to "${state.calorieTarget} kcal"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                state.currentWeightKg?.let {
                    StatCard(title = "Current body weight", value = "${it} kg")
                }
            }
            items(cards.chunked(2)) { rowPair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowPair.forEach { (title, value) ->
                        StatCard(title = title, value = value, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Text(
                    "Estimates only – calories burned and calorie targets are approximations, not medical advice.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
