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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.personal.fittrack.domain.format
import com.personal.fittrack.data.prefs.AppSettings
import kotlin.math.roundToInt

@Composable
fun WorkoutHistoryDetailScreen(sessionId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val settings by app.container.userPreferences.settings.collectAsStateWithLifecycle(AppSettings())
    val viewModel: SessionDetailViewModel = viewModel(factory = viewModelFactory {
        initializer { SessionDetailViewModel(sessionId, app.container.workoutRepository) }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text(state.session?.name ?: "Session") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            item {
                Text("Total volume: ${settings.weightUnit.format(state.totalVolumeKg)}·reps", style = MaterialTheme.typography.titleMedium)
            }
            items(state.setsByExercise.entries.toList(), key = { it.key.id }) { (exercise, sets) ->
                Card(Modifier.fillMaxSize().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        sets.sortedBy { it.setIndex }.forEach { set ->
                            Text("${settings.weightUnit.format(set.weightKg)} x ${set.reps}")
                        }
                    }
                }
            }
        }
    }
}
