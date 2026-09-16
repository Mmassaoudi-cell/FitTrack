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
    val startingWeightKg: Double? get() = entries.firstOrNull()?.weightKg
    val currentWeightKg: Double? get() = entries.lastOrNull()?.weightKg
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

    val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val busy = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var deleted: BodyWeightEntity? = null
    private fun run(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Unable to save weight." }
            finally { busy.value = false }
        }
    }
    fun logWeight(weightKg: Double, date: java.time.LocalDate, onSaved: () -> Unit) = run { repository.logWeight(weightKg, date); onSaved() }
    fun delete(entry: BodyWeightEntity) = run { repository.delete(entry.id); deleted = entry }
    fun undo() = run { deleted?.let { repository.restore(it); deleted = null } }
}
