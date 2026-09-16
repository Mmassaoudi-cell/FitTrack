package com.personal.fittrack.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.repository.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutHubViewModel(private val repository: WorkoutRepository) : ViewModel() {
    val sessions = repository.observeSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val routines = repository.observeRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val exercises = repository.observeExercises().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    private fun run(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Please try again." }
            finally { busy.value = false }
        }
    }
    fun start(name: String = "Workout", plan: String = "", onStarted: (Long) -> Unit) = run {
        onStarted(repository.startSession(name, plan))
    }
    fun repeatLast(onStarted: (Long) -> Unit) = run {
        val last = sessions.value.firstOrNull { it.endTimeEpochMillis != null } ?: return@run
        val plan = repository.getSetsForSessionSync(last.id).sortedBy { it.orderInSession }.map { it.exerciseId }.distinct().joinToString(",")
        onStarted(repository.startSession(last.name, plan))
    }
    fun saveRoutine(name: String, ids: List<Long>, onSaved: () -> Unit) = run { repository.saveRoutine(name, ids); onSaved() }
    fun deleteRoutine(id: Long) = run { repository.deleteRoutine(id) }
}
