package com.personal.fittrack.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.domain.format

@Composable
fun HomeScreen(onWorkout: (Long) -> Unit, onFood: () -> Unit, onWeight: () -> Unit, onSettings: () -> Unit) {
    val container = (LocalContext.current.applicationContext as FitTrackApp).container
    val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container.workoutRepository, container.nutritionRepository, container.bodyWeightRepository, container.userPreferences) } })
    val state by vm.uiState.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Today") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (state.hasActiveWorkout) "Your workout is ready to resume" else "Today's training", style = MaterialTheme.typography.headlineSmall)
                        Text("${state.totalSets} ${if (state.totalSets == 1) "set" else "sets"} · ${state.totalExercises} ${if (state.totalExercises == 1) "exercise" else "exercises"} · ${state.totalReps} reps")
                        if (state.totalSets > 0) Text("${state.weightUnit.format(state.totalVolumeKg)}·reps total volume", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { vm.startWorkout(onWorkout) }, enabled = !busy && !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text(if (busy) "Opening…" else if (state.hasActiveWorkout) "Resume workout" else "Start workout")
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Nutrition", style = MaterialTheme.typography.titleMedium)
                        Text("${state.caloriesConsumed.toInt()} / ${state.calorieTarget} kcal", style = MaterialTheme.typography.headlineMedium)
                        LinearProgressIndicator(progress = { (state.caloriesConsumed / state.calorieTarget.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text(if (state.remainingCalories >= 0) "${state.remainingCalories.toInt()} kcal remaining" else "${(-state.remainingCalories).toInt()} kcal above target")
                        OutlinedButton(onClick = onFood, modifier = Modifier.fillMaxWidth()) { Text("Log food") }
                    }
                }
            }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Body weight", style = MaterialTheme.typography.titleMedium)
                        Text(state.currentWeightKg?.let { state.weightUnit.format(it) } ?: "Add your first weigh-in", style = MaterialTheme.typography.headlineSmall)
                        TextButton(onClick = onWeight) { Text("Log body weight") }
                    }
                }
            }
            if (!state.profileComplete) item {
                Text("Personalize your calorie target", style = MaterialTheme.typography.titleMedium)
                Text("Your target currently uses a default profile. Add your details or set your own target.", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onSettings) { Text("Set up profile") }
            }
            item {
                Text("Estimated workout burn: ${state.caloriesBurnedEstimate.toInt()} kcal. Calorie targets and burn values are estimates, not medical advice.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
