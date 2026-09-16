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
    val summaries: StateFlow<List<SessionSummary>> = combine(
        repository.observeSessions(), repository.observeAllSets()
    ) { sessions, allSets ->
        val grouped = allSets.groupBy { it.sessionId }
        sessions.map { session ->
            val sets = grouped[session.id].orEmpty()
            SessionSummary(session, VolumeCalculator.distinctExerciseCount(sets), sets.size, VolumeCalculator.totalVolume(sets))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
