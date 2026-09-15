package com.personal.fittrack.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun WorkoutHistoryScreen(onOpenSession: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val viewModel: WorkoutHistoryViewModel = viewModel(factory = viewModelFactory {
        initializer { WorkoutHistoryViewModel(app.container.workoutRepository) }
    })
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    Scaffold(topBar = { TopAppBar(title = { Text("Workout History") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            items(summaries, key = { it.session.id }) { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { onOpenSession(summary.session.id) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        val date = Instant.ofEpochMilli(summary.session.startTimeEpochMillis).atZone(ZoneId.systemDefault()).format(formatter)
                        Text(summary.session.name, style = MaterialTheme.typography.titleMedium)
                        Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.exerciseCount} exercises · ${summary.totalSets} sets · ${summary.totalVolumeKg.roundToInt()} kg volume")
                    }
                }
            }
        }
    }
}
