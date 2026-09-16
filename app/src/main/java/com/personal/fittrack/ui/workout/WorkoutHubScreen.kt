package com.personal.fittrack.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.personal.fittrack.ui.components.ExercisePicker

@Composable
fun WorkoutHubScreen(onStartWorkout: (Long) -> Unit, onOpenHistory: () -> Unit, onOpenExercises: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val vm: WorkoutHubViewModel = viewModel(factory = viewModelFactory { initializer { WorkoutHubViewModel(app.container.workoutRepository) } })
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val routines by vm.routines.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val active = sessions.firstOrNull { it.endTimeEpochMillis == null }
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var plan by rememberSaveable { mutableStateOf("") }
    var picker by rememberSaveable { mutableStateOf(false) }
    val ids = plan.split(",").mapNotNull { it.toLongOrNull() }
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Workouts") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (active != null) "Pick up where you left off" else "Make time for your next set", style = MaterialTheme.typography.headlineSmall)
                        Text(if (active != null) "Your unfinished workout is saved and ready to resume." else "Start fresh or follow one of your routines.")
                        Button(onClick = { vm.start(onStarted = onStartWorkout) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Opening…" else if (active != null) "Resume workout" else "Start workout") }
                        if (active == null && sessions.any { it.endTimeEpochMillis != null }) {
                            OutlinedButton(onClick = { vm.repeatLast(onStartWorkout) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Repeat last workout") }
                        }
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenHistory) { Text("Workout history") }
                    OutlinedButton(onClick = onOpenExercises) { Text("Exercise library") }
                }
            }
            item {
                Text("Your routines", style = MaterialTheme.typography.titleLarge)
                Text("A reusable exercise order. Previous weights and reps are filled in when you select each exercise.", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { creating = true }) { Text("Create routine") }
            }
            if (routines.isEmpty()) item { Text("Save a routine for your usual gym days.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(routines, key = { it.id }) { routine ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium)
                        Text(routine.exerciseIds.split(",").mapNotNull { id -> exercises.find { it.id.toString() == id }?.name }.joinToString(" · "))
                        FlowRow {
                            TextButton(onClick = { vm.start(routine.name, routine.exerciseIds, onStartWorkout) }, enabled = !busy && active == null) { Text("Start routine") }
                            TextButton(onClick = { vm.deleteRoutine(routine.id) }, enabled = !busy) { Text("Remove routine") }
                        }
                    }
                }
            }
        }
    }
    if (creating) AlertDialog(onDismissRequest = { creating = false }, title = { Text("New routine") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name, e.g. Upper body") }, modifier = Modifier.fillMaxWidth())
            LazyColumn(Modifier.heightIn(max = 200.dp)) {
                items(ids) { id ->
                    TextButton(onClick = { plan = ids.filter { it != id }.joinToString(",") }) { Text("${exercises.find { it.id == id }?.name ?: "Exercise"} · Remove") }
                }
            }
            OutlinedButton(onClick = { picker = true }) { Text("Add exercise") }
        }
    }, confirmButton = { TextButton(enabled = !busy && name.isNotBlank() && ids.isNotEmpty(), onClick = { vm.saveRoutine(name, ids) { creating = false; name = ""; plan = "" } }) { Text("Save routine") } }, dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } })
    if (picker) ExercisePicker(exercises.filter { it.id !in ids }, { plan = (ids + it.id).joinToString(","); picker = false }, { picker = false })
}
