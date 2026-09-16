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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun WorkoutHistoryScreen(onOpenSession: (Long) -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val settings by app.container.userPreferences.settings.collectAsStateWithLifecycle(AppSettings())
    val viewModel: WorkoutHistoryViewModel = viewModel(factory = viewModelFactory {
        initializer { WorkoutHistoryViewModel(app.container.workoutRepository) }
    })
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Workout history") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            if (summaries.isEmpty()) item { Text("Your completed workouts will appear here. Start a workout to begin your history.", Modifier.padding(16.dp)) }
            items(summaries, key = { it.session.id }) { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { onOpenSession(summary.session.id) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        val date = Instant.ofEpochMilli(summary.session.startTimeEpochMillis).atZone(ZoneId.systemDefault()).format(formatter)
                        Text(summary.session.name, style = MaterialTheme.typography.titleMedium)
                        Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.exerciseCount} exercises · ${summary.totalSets} sets · ${settings.weightUnit.format(summary.totalVolumeKg)}·reps volume")
                    }
                }
            }
        }
    }
}
