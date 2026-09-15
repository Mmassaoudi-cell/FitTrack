package com.personal.fittrack.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.personal.fittrack.FitTrackApp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutHubScreen(
    onStartWorkout: (sessionId: Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExercises: () -> Unit
) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("Workout") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        val name = "Workout - " + DateTimeFormatter.ofPattern("MMM d").format(LocalDate.now())
                        val id = app.container.workoutRepository.startSession(name)
                        onStartWorkout(id)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start Workout") }

            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text("History")
            }
            OutlinedButton(onClick = onOpenExercises, modifier = Modifier.fillMaxWidth()) {
                Text("Exercises")
            }
        }
    }
}
