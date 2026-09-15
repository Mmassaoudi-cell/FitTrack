package com.personal.fittrack.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.db.entity.ExerciseEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity
import com.personal.fittrack.data.prefs.UserPreferences
import com.personal.fittrack.data.repository.WorkoutRepository
import com.personal.fittrack.domain.UnitConverter
import com.personal.fittrack.domain.WeightUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetDraft(val weightKg: Double = 20.0, val reps: Int = 10)

data class ExerciseProgressSummary(
    val lastWeightKg: Double?,
    val lastReps: Int?,
    val bestWeightKg: Double,
    val bestReps: Int,
    val bestVolumeKg: Double
)

data class ActiveWorkoutUiState(
    val session: WorkoutSessionEntity? = null,
    val allExercises: List<ExerciseEntity> = emptyList(),
    val exercisesInSession: List<ExerciseEntity> = emptyList(),
    val selectedExerciseId: Long? = null,
    val setsForSelectedExercise: List<SetEntryEntity> = emptyList(),
    val allSetsInSession: List<SetEntryEntity> = emptyList(),
    val draft: SetDraft = SetDraft(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val weightIncrements: List<Double> = listOf(2.5, 5.0, 10.0),
    val progressSummary: ExerciseProgressSummary? = null,
    val restSecondsRemaining: Int? = null,
    val restTimerRunning: Boolean = false
)

class ActiveWorkoutViewModel(
    private val sessionId: Long,
    private val repository: WorkoutRepository,
    userPreferences: UserPreferences
) : ViewModel() {

    private val orderCounter = mutableMapOf<Long, Int>()
    private val selectedExerciseIdFlow = MutableStateFlow<Long?>(null)
    private val draftFlow = MutableStateFlow(SetDraft())
    private val restState = MutableStateFlow<Pair<Int?, Boolean>>(null to false)
    private var restJob: Job? = null

    private val sessionFlow = MutableStateFlow<WorkoutSessionEntity?>(null)

    init {
        viewModelScope.launch {
            sessionFlow.value = repository.getSession(sessionId)
        }
    }

    private data class BaseCombo(
        val session: WorkoutSessionEntity?,
        val allExercises: List<ExerciseEntity>,
        val setsInSession: List<SetEntryEntity>,
        val selectedId: Long?,
        val draft: SetDraft
    )

    private val baseCombo = combine(
        sessionFlow,
        repository.observeExercises(),
        repository.observeSetsForSession(sessionId),
        selectedExerciseIdFlow,
        draftFlow
    ) { session, allExercises, setsInSession, selectedId, draft ->
        BaseCombo(session, allExercises, setsInSession, selectedId, draft)
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        baseCombo,
        userPreferences.settings,
        restState
    ) { base, settings, rest ->
        val session = base.session
        val allExercises = base.allExercises
        val setsInSession = base.setsInSession
        val selectedId = base.selectedId
        val draft = base.draft

        val exerciseIdsInSession = setsInSession.map { it.exerciseId }.distinct()
        val exercisesInSession = allExercises.filter { it.id in exerciseIdsInSession || it.id == selectedId }
        val setsForSelected = setsInSession.filter { it.exerciseId == selectedId }
        val baseIncrement = settings.weightIncrementKg
        val increments = listOf(baseIncrement, baseIncrement * 2, baseIncrement * 4).distinct()

        ActiveWorkoutUiState(
            session = session,
            allExercises = allExercises,
            exercisesInSession = exercisesInSession,
            selectedExerciseId = selectedId,
            setsForSelectedExercise = setsForSelected,
            allSetsInSession = setsInSession,
            draft = draft,
            weightUnit = settings.weightUnit,
            weightIncrements = increments,
            restSecondsRemaining = rest.first,
            restTimerRunning = rest.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveWorkoutUiState())

    fun selectExercise(exerciseId: Long) {
        selectedExerciseIdFlow.value = exerciseId
        viewModelScope.launch {
            val last = repository.getLastPerformance(exerciseId)
            draftFlow.value = if (last != null) {
                SetDraft(weightKg = last.weightKg, reps = last.reps)
            } else {
                SetDraft()
            }
        }
    }

    suspend fun getProgressSummary(exerciseId: Long): ExerciseProgressSummary {
        val history = repository.getAllSetsForExercise(exerciseId)
        val recent = repository.getLastPerformance(exerciseId)
        return ExerciseProgressSummary(
            lastWeightKg = recent?.weightKg,
            lastReps = recent?.reps,
            bestWeightKg = history.maxOfOrNull { it.weightKg } ?: recent?.weightKg ?: 0.0,
            bestReps = history.maxOfOrNull { it.reps } ?: recent?.reps ?: 0,
            bestVolumeKg = history.maxOfOrNull { it.weightKg * it.reps } ?: ((recent?.weightKg ?: 0.0) * (recent?.reps ?: 0))
        )
    }

    fun incrementReps() = draftFlow.update { it.copy(reps = it.reps + 1) }
    fun decrementReps() = draftFlow.update { it.copy(reps = (it.reps - 1).coerceAtLeast(0)) }
    fun resetReps() = draftFlow.update { it.copy(reps = 0) }
    fun adjustWeight(deltaKg: Double) = draftFlow.update {
        it.copy(weightKg = UnitConverter.round2((it.weightKg + deltaKg).coerceAtLeast(0.0)))
    }
    fun setDraft(weightKg: Double, reps: Int) {
        draftFlow.value = SetDraft(weightKg, reps)
    }

    fun completeSet(autoStartRest: Boolean, restSeconds: Int) {
        val exerciseId = selectedExerciseIdFlow.value ?: return
        val draft = draftFlow.value
        viewModelScope.launch {
            val order = orderCounter.getOrPut(exerciseId) { orderCounter.size }
            val existingCount = uiState.value.setsForSelectedExercise.size
            repository.addSet(
                sessionId = sessionId,
                exerciseId = exerciseId,
                orderInSession = order,
                setIndex = existingCount + 1,
                weightKg = draft.weightKg,
                reps = draft.reps
            )
            if (autoStartRest) startRestTimer(restSeconds)
        }
    }

    fun copyPreviousSet() {
        val sets = uiState.value.setsForSelectedExercise
        val previous = sets.maxByOrNull { it.setIndex } ?: return
        draftFlow.value = SetDraft(previous.weightKg, previous.reps)
    }

    fun startRestTimer(seconds: Int) {
        restJob?.cancel()
        restState.value = seconds to true
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                restState.value = remaining to true
            }
            restState.value = 0 to false
        }
    }

    fun pauseRestTimer() {
        restJob?.cancel()
        restState.update { it.first to false }
    }

    fun resumeRestTimer() {
        val remaining = restState.value.first ?: return
        if (remaining > 0) startRestTimer(remaining)
    }

    fun resetRestTimer() {
        restJob?.cancel()
        restState.value = null to false
    }

    fun endSession() {
        viewModelScope.launch {
            sessionFlow.value?.let { repository.endSession(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        restJob?.cancel()
    }
}
