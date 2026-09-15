package com.personal.fittrack.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import com.personal.fittrack.data.prefs.UserPreferences
import com.personal.fittrack.data.repository.BodyWeightRepository
import com.personal.fittrack.data.repository.MealType
import com.personal.fittrack.data.repository.NutritionRepository
import com.personal.fittrack.domain.CalorieCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutritionUiState(
    val entriesByMeal: Map<MealType, List<FoodLogEntryEntity>> = emptyMap(),
    val calorieTarget: Int = 2000,
    val consumed: Double = 0.0
) {
    val remaining: Double get() = calorieTarget - consumed
    fun mealCalories(meal: MealType): Double = entriesByMeal[meal].orEmpty().sumOf { it.calories }
}

class NutritionViewModel(
    private val nutritionRepository: NutritionRepository,
    bodyWeightRepository: BodyWeightRepository,
    userPreferences: UserPreferences
) : ViewModel() {

    val uiState = combine(
        nutritionRepository.observeLogForDay(),
        bodyWeightRepository.observeLatest(),
        userPreferences.settings
    ) { entries, latestWeight, settings ->
        val calorieTarget = settings.calorieTargetOverride ?: CalorieCalculator.dailyCalorieTarget(
            sex = settings.profileSex,
            weightKg = latestWeight?.weightKg ?: 70.0,
            heightCm = settings.profileHeightCm,
            ageYears = settings.profileAgeYears,
            activityLevel = settings.profileActivityLevel,
            goal = settings.profileGoal
        )
        NutritionUiState(
            entriesByMeal = entries.groupBy { MealType.valueOf(it.mealType) },
            calorieTarget = calorieTarget,
            consumed = entries.sumOf { it.calories }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionUiState())

    fun deleteEntry(id: Long) {
        viewModelScope.launch { nutritionRepository.deleteLogEntry(id) }
    }
}
