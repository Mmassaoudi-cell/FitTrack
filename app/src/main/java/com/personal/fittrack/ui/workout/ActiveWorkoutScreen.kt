package com.personal.fittrack.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.personal.fittrack.domain.UnitConverter
import com.personal.fittrack.domain.WeightUnit
import com.personal.fittrack.ui.components.RepCounter
import com.personal.fittrack.ui.components.WeightStepper
import kotlin.math.roundToInt

private val restPresets = listOf(30, 60, 90, 120, 180)

@Composable
fun ActiveWorkoutScreen(sessionId: Long, onEndWorkout: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val container = app.container
    val viewModel: ActiveWorkoutViewModel = viewModel(factory = viewModelFactory {
        initializer { ActiveWorkoutViewModel(sessionId, container.workoutRepository, container.userPreferences) }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExercisePicker by remember { mutableStateOf(false) }
    var progressSummary by remember { mutableStateOf<ExerciseProgressSummary?>(null) }

    LaunchedEffect(state.selectedExerciseId) {
        state.selectedExerciseId?.let { progressSummary = viewModel.getProgressSummary(it) }
    }

    val displayUnit = state.weightUnit
    fun toDisplay(kg: Double) = if (displayUnit == WeightUnit.LB) UnitConverter.kgToLb(kg) else kg
    fun formatWeight(kg: Double): String {
        val v = toDisplay(kg)
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.session?.name ?: "Workout") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box2 {
                    OutlinedButton(onClick = { showExercisePicker = true }) {
                        Text(
                            state.allExercises.firstOrNull { it.id == state.selectedExerciseId }?.name
                                ?: "Choose exercise"
                        )
                    }
                    DropdownMenu(expanded = showExercisePicker, onDismissRequest = { showExercisePicker = false }) {
                        state.allExercises.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise.name) },
                                onClick = {
                                    viewModel.selectExercise(exercise.id)
                                    showExercisePicker = false
                                }
                            )
                        }
                    }
                }
                Button(onClick = {
                    viewModel.endSession()
                    onEndWorkout()
                }) { Text("Finish") }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            if (state.selectedExerciseId == null) {
                Text("Pick an exercise to begin logging sets.", style = MaterialTheme.typography.bodyLarge)
            } else {
                progressSummary?.let { summary ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Progressive overload", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Previous: ${summary.lastWeightKg?.let { formatWeight(it) } ?: "-"} ${displayUnit.name.lowercase()} x ${summary.lastReps ?: "-"}"
                            )
                            Text("Best weight: ${formatWeight(summary.bestWeightKg)} ${displayUnit.name.lowercase()}  |  Best reps: ${summary.bestReps}  |  Best volume: ${summary.bestVolumeKg.roundToInt()} kg")
                        }
                    }
                    Spacer12()
                }

                WeightStepper(
                    weightLabel = formatWeight(state.draft.weightKg),
                    unitLabel = displayUnit.name.lowercase(),
                    increments = state.weightIncrements.map { toDisplay(it) },
                    onAdjust = { deltaDisplay ->
                        val deltaKg = if (displayUnit == WeightUnit.LB) UnitConverter.lbToKg(deltaDisplay) else deltaDisplay
                        viewModel.adjustWeight(deltaKg)
                    }
                )

                Spacer12()

                RepCounter(
                    reps = state.draft.reps,
                    onIncrement = viewModel::incrementReps,
                    onDecrement = viewModel::decrementReps,
                    onReset = viewModel::resetReps,
                    onCopyPrevious = if (state.setsForSelectedExercise.isNotEmpty()) viewModel::copyPreviousSet else null
                )

                Spacer12()

                Button(onClick = { viewModel.completeSet(autoStartRest = true, restSeconds = 90) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Complete Set")
                }

                Spacer12()

                RestTimerCard(
                    secondsRemaining = state.restSecondsRemaining,
                    isRunning = state.restTimerRunning,
                    onStartPreset = { viewModel.startRestTimer(it) },
                    onPause = viewModel::pauseRestTimer,
                    onResume = viewModel::resumeRestTimer,
                    onReset = viewModel::resetRestTimer
                )

                Spacer12()

                Text("Sets this exercise", style = MaterialTheme.typography.labelLarge)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.setsForSelectedExercise, key = { it.id }) { set ->
                        Text("Set ${set.setIndex}: ${formatWeight(set.weightKg)} ${displayUnit.name.lowercase()} x ${set.reps}")
                    }
                }
            }
        }
    }
}

@Composable
private fun Box2(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box { content() }
}

@Composable
private fun Spacer12() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
}

@Composable
private fun RestTimerCard(
    secondsRemaining: Int?,
    isRunning: Boolean,
    onStartPreset: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rest timer", style = MaterialTheme.typography.labelLarge)
            Text(secondsRemaining?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: "--:--", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                restPresets.forEach { preset ->
                    AssistChip(onClick = { onStartPreset(preset) }, label = { Text("${preset}s") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRunning) {
                    OutlinedButton(onClick = onPause) { Text("Pause") }
                } else {
                    OutlinedButton(onClick = onResume, enabled = secondsRemaining != null && secondsRemaining > 0) { Text("Resume") }
                }
                OutlinedButton(onClick = onReset) { Text("Reset") }
            }
        }
    }
}
