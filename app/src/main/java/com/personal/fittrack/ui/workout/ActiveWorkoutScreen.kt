package com.personal.fittrack.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.prefs.WorkoutDraftStore
import com.personal.fittrack.domain.*
import com.personal.fittrack.ui.components.*

@Composable
fun ActiveWorkoutScreen(sessionId: Long, onEndWorkout: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as FitTrackApp).container
    val vm: ActiveWorkoutViewModel = viewModel(factory = viewModelFactory {
        initializer { ActiveWorkoutViewModel(sessionId, container.workoutRepository, container.userPreferences, WorkoutDraftStore(context.applicationContext, sessionId)) }
    })
    val state by vm.uiState.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SetEntryEntity?>(null) }
    var restSeconds by rememberSaveable { mutableIntStateOf(90) }
    var autoRest by rememberSaveable { mutableStateOf(true) }
    var weightInput by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.finished) { if (state.finished) onEndWorkout() }
    androidx.activity.compose.BackHandler(enabled = state.busy) {}
    val unit = state.weightUnit
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Active workout") },
        navigationIcon = { IconButton(onClick = onBack, enabled = !state.busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back; workout remains saved") } },
        actions = { TextButton(onClick = { confirmFinish = true }, enabled = !state.busy && state.session != null) { Text("Finish") } }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(onClick = { vm.completeSet(autoRest, restSeconds) },
                    enabled = !state.busy && state.selectedExerciseId != null && state.draft.reps > 0,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(if (state.busy) "Saving…" else "Complete set · ${unit.format(state.draft.weightKg)} × ${state.draft.reps}")
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${state.allSetsInSession.size} ${if (state.allSetsInSession.size == 1) "set" else "sets"} logged • Progress saved on this device", style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = { picker = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(state.allExercises.find { it.id == state.selectedExerciseId }?.name ?: "Choose your first exercise")
            }
            if (state.exercisesInSession.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.exercisesInSession.forEach { exercise ->
                        FilterChip(selected = state.selectedExerciseId == exercise.id, onClick = { vm.selectExercise(exercise.id) }, enabled = !state.busy, label = { Text(exercise.name) })
                    }
                }
            }
            if (state.selectedExerciseId != null) {
                state.progressSummary?.let { summary ->
                    Text(summary.lastWeightKg?.let { "Last workout: ${unit.format(it)} × ${summary.lastReps} · Best ${unit.format(summary.bestWeightKg)}" }
                        ?: "First session with this exercise", style = MaterialTheme.typography.bodyMedium)
                }

                WeightStepper("%.1f".format(unit.display(state.draft.weightKg)), unit.name.lowercase(),
                    state.weightIncrements.map { unit.display(it) }, { vm.adjustWeight(unit.toKg(it)) })
                TextButton(onClick = { weightInput = "%.2f".format(unit.display(state.draft.weightKg)) }, modifier = Modifier.fillMaxWidth()) { Text("Enter exact weight") }
                RepCounter(state.draft.reps, vm::incrementReps, vm::decrementReps, vm::resetReps,
                    if (state.setsForSelectedExercise.isNotEmpty()) vm::copyPreviousSet else null)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rest timer", style = MaterialTheme.typography.titleMedium)
                            Switch(checked = autoRest, onCheckedChange = { autoRest = it })
                        }
                        Text("Start automatically after each set", style = MaterialTheme.typography.bodySmall)
                        Text(state.restSecondsRemaining?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: "Ready when you are", style = MaterialTheme.typography.headlineMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 60, 90, 120, 180).forEach { seconds ->
                                FilterChip(selected = restSeconds == seconds, onClick = { restSeconds = seconds; vm.startRestTimer(seconds) }, label = { Text("${seconds}s") })
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { if (state.restTimerRunning) vm.pauseRestTimer() else vm.resumeRestTimer() }, enabled = (state.restSecondsRemaining ?: 0) > 0) { Text(if (state.restTimerRunning) "Pause" else "Resume") }
                            TextButton(onClick = vm::resetRestTimer) { Text("Reset") }
                        }
                    }
                }
                Text("Completed sets", style = MaterialTheme.typography.titleMedium)
                if (state.setsForSelectedExercise.isEmpty()) Text("Log your first set using the button below.")
                state.setsForSelectedExercise.forEach { set ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Set ${set.setIndex} · ${unit.format(set.weightKg)} × ${set.reps}")
                            FlowRow {
                                TextButton(onClick = { editing = set }, enabled = !state.busy) { Text("Edit") }
                                TextButton(onClick = { vm.deleteSet(set) }, enabled = !state.busy) { Text("Delete") }
                            }
                        }
                    }
                }
                TextButton(onClick = vm::undoDelete, enabled = !state.busy) { Text("Undo last deletion") }
            } else Text("Choose an exercise, adjust your weight and reps, then complete a set. You can leave and resume any time.")
        }
    }
    if (picker) ExercisePicker(state.allExercises, { vm.selectExercise(it.id); picker = false }, { picker = false })
    editing?.let { set -> SetEditor(set, unit, { vm.editSet(it); editing = null }, { editing = null }) }
    weightInput?.let { input ->
        val number = InputValidation.number(input)
        AlertDialog(onDismissRequest = { weightInput = null }, title = { Text("Set weight") }, text = {
            NumberField(input, { weightInput = it }, "Weight (${unit.name.lowercase()})", Modifier.fillMaxWidth())
        }, confirmButton = { TextButton(enabled = number != null && number >= 0, onClick = { vm.setDraft(unit.toKg(number!!), state.draft.reps); weightInput = null }) { Text("Apply") } }, dismissButton = { TextButton(onClick = { weightInput = null }) { Text("Cancel") } })
    }
    if (confirmFinish) AlertDialog(onDismissRequest = { confirmFinish = false }, title = { Text("Finish this workout?") },
        text = { Text("${state.allSetsInSession.size} completed sets will stay in your history. Your next workout starts a new session.") },
        confirmButton = { TextButton(onClick = { confirmFinish = false; vm.endSession() }) { Text("Finish workout") } },
        dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep training") } })
}
