package com.personal.fittrack.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.db.entity.ExerciseEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.data.db.entity.WorkoutSessionEntity
import com.personal.fittrack.data.repository.WorkoutRepository
import com.personal.fittrack.domain.VolumeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionSummary(
    val session: WorkoutSessionEntity,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalVolumeKg: Double
)

class WorkoutHistoryViewModel(private val repository: WorkoutRepository) : ViewModel() {
    private val sessionSetsCache = MutableStateFlow<Map<Long, List<SetEntryEntity>>>(emptyMap())

    val summaries: StateFlow<List<SessionSummary>> = combine(
        repository.observeSessions(),
        sessionSetsCache
    ) { sessions, cache ->
        sessions.map { session ->
            val sets = cache[session.id].orEmpty()
            SessionSummary(
                session = session,
                exerciseCount = VolumeCalculator.distinctExerciseCount(sets),
                totalSets = sets.size,
                totalVolumeKg = VolumeCalculator.totalVolume(sets)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.observeSessions().collect { sessions ->
                val updated = sessionSetsCache.value.toMutableMap()
                sessions.forEach { session ->
                    if (session.id !in updated) {
                        updated[session.id] = repository.getSetsForSessionSync(session.id)
                    }
                }
                sessionSetsCache.value = updated
            }
        }
    }
}

data class SessionDetailUiState(
    val session: WorkoutSessionEntity? = null,
    val setsByExercise: Map<ExerciseEntity, List<SetEntryEntity>> = emptyMap(),
    val totalVolumeKg: Double = 0.0
)

class SessionDetailViewModel(
    private val sessionId: Long,
    private val repository: WorkoutRepository
) : ViewModel() {
    private val sessionFlow = MutableStateFlow<WorkoutSessionEntity?>(null)

    val uiState: StateFlow<SessionDetailUiState> = combine(
        sessionFlow,
        repository.observeSetsForSession(sessionId),
        repository.observeExercises()
    ) { session, sets, exercises ->
        val exerciseById = exercises.associateBy { it.id }
        val grouped = sets.groupBy { exerciseById[it.exerciseId] }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
        SessionDetailUiState(
            session = session,
            setsByExercise = grouped,
            totalVolumeKg = VolumeCalculator.totalVolume(sets)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionDetailUiState())

    init {
        viewModelScope.launch {
            sessionFlow.value = repository.getSession(sessionId)
        }
    }
}
