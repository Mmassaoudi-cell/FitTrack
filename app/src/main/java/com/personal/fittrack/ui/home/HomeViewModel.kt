package com.personal.fittrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.prefs.UserPreferences
import com.personal.fittrack.data.repository.BodyWeightRepository
import com.personal.fittrack.data.repository.NutritionRepository
import com.personal.fittrack.data.repository.WorkoutRepository
import com.personal.fittrack.domain.CalorieCalculator
import com.personal.fittrack.domain.VolumeCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalExercises: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val caloriesConsumed: Double = 0.0,
    val caloriesBurnedEstimate: Double = 0.0,
    val calorieTarget: Int = 2000,
    val currentWeightKg: Double? = null,
    val weightUnit: com.personal.fittrack.domain.WeightUnit = com.personal.fittrack.domain.WeightUnit.KG,
    val profileComplete: Boolean = false,
    val hasActiveWorkout: Boolean = false,
    val isLoading: Boolean = true
) {
    val remainingCalories: Double get() = calorieTarget - caloriesConsumed
}

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    nutritionRepository: NutritionRepository,
    bodyWeightRepository: BodyWeightRepository,
    userPreferences: UserPreferences
) : ViewModel() {

    val uiState = combine(
        workoutRepository.observeSetsToday(),
        nutritionRepository.observeLogForDay(),
        bodyWeightRepository.observeLatest(),
        userPreferences.settings,
        workoutRepository.observeSessions()
    ) { sets, foodEntries, latestWeight, settings, sessions ->
        val weightForEstimate = latestWeight?.weightKg ?: 70.0
        val durationMinutes = com.personal.fittrack.domain.WorkoutDuration.minutes(sets)
        val met = 6.0 // moderate resistance training
        val caloriesBurned = met * 3.5 * weightForEstimate / 200.0 * durationMinutes

        val calorieTarget = settings.calorieTargetOverride ?: CalorieCalculator.dailyCalorieTarget(
            sex = settings.profileSex,
            weightKg = weightForEstimate,
            heightCm = settings.profileHeightCm,
            ageYears = settings.profileAgeYears,
            activityLevel = settings.profileActivityLevel,
            goal = settings.profileGoal
        )

        HomeUiState(
            totalExercises = VolumeCalculator.distinctExerciseCount(sets),
            totalSets = sets.size,
            totalReps = VolumeCalculator.totalReps(sets),
            totalVolumeKg = VolumeCalculator.totalVolume(sets),
            caloriesConsumed = foodEntries.sumOf { it.calories },
            caloriesBurnedEstimate = caloriesBurned,
            calorieTarget = calorieTarget,
            currentWeightKg = latestWeight?.weightKg,
            weightUnit = settings.weightUnit,
            profileComplete = settings.hasCompletedProfile,
            hasActiveWorkout = sessions.any { it.endTimeEpochMillis == null },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
    val busy = kotlinx.coroutines.flow.MutableStateFlow(false)
    val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun startWorkout(onStarted: (Long) -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { onStarted(workoutRepository.startSession("Workout")) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Unable to open your workout." }
            finally { busy.value = false }
        }
    }
}
