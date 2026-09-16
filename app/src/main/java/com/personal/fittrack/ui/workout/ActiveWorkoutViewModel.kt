package com.personal.fittrack.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.db.entity.*
import com.personal.fittrack.data.prefs.UserPreferences
import com.personal.fittrack.data.prefs.WorkoutDraftStore
import com.personal.fittrack.data.repository.WorkoutRepository
import com.personal.fittrack.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SetDraft(val weightKg: Double = 20.0, val reps: Int = 10)
data class ExerciseProgressSummary(val lastWeightKg: Double?, val lastReps: Int?, val bestWeightKg: Double, val bestReps: Int, val bestVolumeKg: Double)
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
    val restTimerRunning: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val finished: Boolean = false
)

class ActiveWorkoutViewModel(
    private val sessionId: Long,
    private val repository: WorkoutRepository,
    userPreferences: UserPreferences,
    private val recovery: WorkoutDraftStore
) : ViewModel() {
    private val selected = MutableStateFlow(recovery.exerciseId)
    private val draft = MutableStateFlow(SetDraft(recovery.weight, recovery.reps))
    private val rest = MutableStateFlow<Pair<Int?, Boolean>>(recovery.remaining.takeIf { it > 0 } to false)
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val finished = MutableStateFlow(false)
    private var restJob: Job? = null
    private var selectionJob: Job? = null
    private var deleted: SetEntryEntity? = null

    private val base = combine(repository.observeSessions(), repository.observeExercises(), repository.observeAllSets(), selected, draft) { sessions, exercises, allSets, id, d ->
        val session = sessions.firstOrNull { it.id == sessionId }
        val sets = allSets.filter { it.sessionId == sessionId }.sortedWith(compareBy({ it.orderInSession }, { it.setIndex }))
        val previous = allSets.filter { it.exerciseId == id && it.sessionId != sessionId }
        val last = previous.maxByOrNull { it.completedAtEpochMillis }
        val plan = session?.exercisePlan.orEmpty().split(",").mapNotNull { it.toLongOrNull() }
        val ids = (plan + sets.map { it.exerciseId } + listOfNotNull(id)).distinct()
        ActiveWorkoutUiState(
            session = session, allExercises = exercises,
            exercisesInSession = ids.mapNotNull { target -> exercises.find { it.id == target } },
            selectedExerciseId = id, setsForSelectedExercise = sets.filter { it.exerciseId == id },
            allSetsInSession = sets, draft = d,
            progressSummary = ExerciseProgressSummary(last?.weightKg, last?.reps,
                previous.maxOfOrNull { it.weightKg } ?: 0.0, previous.maxOfOrNull { it.reps } ?: 0,
                previous.maxOfOrNull { it.weightKg * it.reps } ?: 0.0)
        )
    }
    private val operation = combine(busy, error, finished) { b, e, f -> Triple(b, e, f) }
    val uiState = combine(base, userPreferences.settings, rest, operation) { b, settings, timer, op ->
        b.copy(weightUnit = settings.weightUnit,
            weightIncrements = listOf(settings.weightIncrementKg, settings.weightIncrementKg * 2, settings.weightIncrementKg * 4),
            restSecondsRemaining = timer.first, restTimerRunning = timer.second,
            busy = op.first, error = op.second, finished = op.third)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveWorkoutUiState())

    init {
        if (recovery.deadline > 0) runTimer()
        viewModelScope.launch {
            if (selected.value == null) {
                val session = repository.getSession(sessionId)
                val last = repository.getSetsForSessionSync(sessionId).maxByOrNull { it.completedAtEpochMillis }
                val id = last?.exerciseId ?: session?.exercisePlan?.split(",")?.firstOrNull()?.toLongOrNull()
                id?.let(::selectExercise)
            }
        }
    }

    fun selectExercise(id: Long) {
        selectionJob?.cancel()
        selected.value = id
        recovery.exerciseId = id
        selectionJob = viewModelScope.launch {
            val last = repository.getLastPerformance(id)
            if (selected.value == id) setDraft(last?.weightKg ?: 20.0, last?.reps ?: 10)
        }
    }
    fun setDraft(weightKg: Double, reps: Int) {
        if (!InputValidation.nonNegative(weightKg) || reps !in 0..999) return
        draft.value = SetDraft(weightKg, reps)
        recovery.weight = weightKg
        recovery.reps = reps
    }
    fun incrementReps() = setDraft(draft.value.weightKg, (draft.value.reps + 1).coerceAtMost(999))
    fun decrementReps() = setDraft(draft.value.weightKg, (draft.value.reps - 1).coerceAtLeast(0))
    fun resetReps() = setDraft(draft.value.weightKg, 0)
    fun adjustWeight(deltaKg: Double) = setDraft(UnitConverter.round2((draft.value.weightKg + deltaKg).coerceAtLeast(0.0)), draft.value.reps)
    private fun save(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Could not save. Please try again." }
            finally { busy.value = false }
        }
    }
    fun completeSet(autoStartRest: Boolean, restSeconds: Int) {
        val id = selected.value ?: return
        val d = draft.value
        save {
            repository.appendSet(sessionId, id, d.weightKg, d.reps)
            if (autoStartRest) startRestTimer(restSeconds)
        }
    }
    fun copyPreviousSet() {
        uiState.value.setsForSelectedExercise.maxByOrNull { it.setIndex }?.let { setDraft(it.weightKg, it.reps) }
    }
    fun editSet(set: SetEntryEntity) = save { repository.updateSet(set) }
    fun deleteSet(set: SetEntryEntity) = save { repository.deleteSet(set.id); deleted = set }
    fun undoDelete() = save { deleted?.let { repository.restoreSet(it); deleted = null } }
    fun startRestTimer(seconds: Int) {
        recovery.deadline = System.currentTimeMillis() + seconds * 1000L
        runTimer()
    }
    private fun runTimer() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (true) {
                val seconds = ((recovery.deadline - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
                recovery.remaining = seconds
                rest.value = seconds to (seconds > 0)
                if (seconds == 0) { recovery.deadline = 0; break }
                delay(200)
            }
        }
    }
    fun pauseRestTimer() { restJob?.cancel(); recovery.deadline = 0; rest.value = rest.value.first to false }
    fun resumeRestTimer() { rest.value.first?.takeIf { it > 0 }?.let(::startRestTimer) }
    fun resetRestTimer() { restJob?.cancel(); recovery.deadline = 0; recovery.remaining = 0; rest.value = null to false }
    fun endSession() = save {
        val session = repository.getSession(sessionId) ?: error("Workout not found.")
        repository.endSession(session)
        resetRestTimer()
        recovery.clear()
        finished.value = true
    }
}
