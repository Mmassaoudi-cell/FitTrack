package com.personal.fittrack.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.ui.components.SimpleLineChart
import kotlin.math.roundToInt

@Composable
fun ProgressScreen() {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Workout", "Body Weight")

    Scaffold(topBar = { TopAppBar(title = { Text("Progress") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
                }
            }
            when (tabIndex) {
                0 -> WorkoutProgressTab()
                1 -> BodyWeightProgressTab()
            }
        }
    }
}

@Composable
private fun WorkoutProgressTab() {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val exercises by app.container.workoutRepository.observeExercises().collectAsState(initial = emptyList())
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(exercises.firstOrNull { it.id == selectedId }?.name ?: "Select exercise")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                exercises.forEach { exercise ->
                    DropdownMenuItem(text = { Text(exercise.name) }, onClick = {
                        selectedId = exercise.id
                        expanded = false
                    })
                }
            }
        }

        selectedId?.let { id ->
            val viewModel: ExerciseProgressViewModel = viewModel(
                key = "exercise_progress_$id",
                factory = viewModelFactory { initializer { ExerciseProgressViewModel(id, app.container.workoutRepository) } }
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Last weight: ${state.lastWeightKg ?: "-"} kg")
                    Text("Maximum weight: ${state.maxWeightKg} kg")
                    Text("Best reps: ${state.bestReps}")
                    Text("Best volume: ${state.bestVolumeKg.roundToInt()} kg")
                }
            }

            Text("Weight over time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            SimpleLineChart(values = state.history.map { it.weightKg.toFloat() })

            Text("Reps over time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            SimpleLineChart(values = state.history.map { it.reps.toFloat() })

            Text("Volume over time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            SimpleLineChart(values = state.history.map { (it.weightKg * it.reps).toFloat() })
        }
    }
}

@Composable
private fun BodyWeightProgressTab() {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val viewModel: BodyWeightViewModel = viewModel(factory = viewModelFactory {
        initializer { BodyWeightViewModel(app.container.bodyWeightRepository) }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Starting: ${state.startingWeightKg ?: "-"} kg")
                    Text("Current: ${state.currentWeightKg ?: "-"} kg")
                    Text("Difference: ${state.differenceKg?.let { "%.1f".format(it) } ?: "-"} kg")
                }
            }
        }
        Button(onClick = { showAddDialog = true }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Log body weight")
        }
        Text("Trend", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
        SimpleLineChart(values = state.entries.map { it.weightKg.toFloat() })

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(state.entries.reversed()) { entry ->
                Text("${java.time.LocalDate.ofEpochDay(entry.dateEpochDay)}: ${entry.weightKg} kg")
            }
        }
    }

    if (showAddDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log body weight") },
            text = {
                OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Weight (kg)") })
            },
            confirmButton = {
                TextButton(onClick = {
                    input.toDoubleOrNull()?.let { viewModel.logWeight(it) }
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
