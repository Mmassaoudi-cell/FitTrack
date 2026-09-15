package com.personal.fittrack.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import kotlin.math.roundToInt

@Composable
fun WorkoutHistoryDetailScreen(sessionId: Long) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val viewModel: SessionDetailViewModel = viewModel(factory = viewModelFactory {
        initializer { SessionDetailViewModel(sessionId, app.container.workoutRepository) }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(state.session?.name ?: "Session") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            item {
                Text("Total volume: ${state.totalVolumeKg.roundToInt()} kg", style = MaterialTheme.typography.titleMedium)
            }
            items(state.setsByExercise.entries.toList(), key = { it.key.id }) { (exercise, sets) ->
                Card(Modifier.fillMaxSize().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        sets.sortedBy { it.setIndex }.forEach { set ->
                            Text("${set.weightKg} kg x ${set.reps}")
                        }
                    }
                }
            }
        }
    }
}
