package com.personal.fittrack.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.db.entity.ExerciseEntity
import kotlinx.coroutines.launch

@Composable
fun ExerciseListScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val scope = rememberCoroutineScope()
    val exercises by app.container.workoutRepository.observeExercises().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Exercises") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add custom exercise")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            items(exercises, key = { it.id }) { exercise ->
                ExerciseRow(exercise)
            }
        }
    }

    if (showAddDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category ->
                scope.launch { app.container.workoutRepository.addCustomExercise(name, category) }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(exercise.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AddCustomExerciseDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Custom") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Exercise name") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), category.trim().ifBlank { "Custom" }) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
