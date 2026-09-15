package com.personal.fittrack.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.db.entity.BodyWeightEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.repository.BodyWeightRepository
import com.personal.fittrack.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseProgressUiState(
    val history: List<SetEntryEntity> = emptyList(),
    val lastWeightKg: Double? = null,
    val maxWeightKg: Double = 0.0,
    val bestReps: Int = 0,
    val bestVolumeKg: Double = 0.0
)

class ExerciseProgressViewModel(
    exerciseId: Long,
    repository: WorkoutRepository
) : ViewModel() {
    val uiState: StateFlow<ExerciseProgressUiState> = repository.observeSetsForExercise(exerciseId)
        .map { sets ->
            val ordered = sets.sortedBy { it.completedAtEpochMillis }
            ExerciseProgressUiState(
                history = ordered,
                lastWeightKg = ordered.lastOrNull()?.weightKg,
                maxWeightKg = ordered.maxOfOrNull { it.weightKg } ?: 0.0,
                bestReps = ordered.maxOfOrNull { it.reps } ?: 0,
                bestVolumeKg = ordered.maxOfOrNull { it.weightKg * it.reps } ?: 0.0
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseProgressUiState())
}

data class BodyWeightUiState(
    val entries: List<BodyWeightEntity> = emptyList()
) {
    val startingWeightKg: Double? get() = entries.minByOrNull { it.dateEpochDay }?.weightKg
    val currentWeightKg: Double? get() = entries.maxByOrNull { it.dateEpochDay }?.weightKg
    val differenceKg: Double? get() {
        val start = startingWeightKg ?: return null
        val current = currentWeightKg ?: return null
        return current - start
    }
}

class BodyWeightViewModel(private val repository: BodyWeightRepository) : ViewModel() {
    val uiState: StateFlow<BodyWeightUiState> = repository.observeAll()
        .map { BodyWeightUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BodyWeightUiState())

    fun logWeight(weightKg: Double) {
        viewModelScope.launch { repository.logWeight(weightKg) }
    }
}
