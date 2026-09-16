package com.personal.fittrack.ui.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import com.personal.fittrack.data.prefs.UserPreferences
import com.personal.fittrack.data.repository.*
import com.personal.fittrack.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class NutritionUiState(val day: LocalDate = LocalDate.now(), val entriesByMeal: Map<MealType, List<FoodLogEntryEntity>> = emptyMap(), val calorieTarget: Int = 2000, val consumed: Double = 0.0) {
    val remaining get() = calorieTarget - consumed
    val protein get() = entriesByMeal.values.flatten().sumOf { it.protein }
    val carbs get() = entriesByMeal.values.flatten().sumOf { it.carbs }
    val fat get() = entriesByMeal.values.flatten().sumOf { it.fat }
    fun mealCalories(meal: MealType) = entriesByMeal[meal].orEmpty().sumOf { it.calories }
}
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(private val repository: NutritionRepository, bodyWeights: BodyWeightRepository, preferences: UserPreferences, private val saved: SavedStateHandle) : ViewModel() {
    private val selected = saved.getStateFlow<Long?>("selectedDay", null)
    private val day = combine(selected, currentDay()) { selected, today -> selected?.let(LocalDate::ofEpochDay) ?: today.first }
    private val entries = day.flatMapLatest { date -> repository.observeLogForDay(date).map { date to it } }
    val uiState = combine(entries, bodyWeights.observeLatest(), preferences.settings) { (date, foods), weight, settings ->
        val target = settings.calorieTargetOverride ?: CalorieCalculator.dailyCalorieTarget(settings.profileSex, weight?.weightKg ?: 70.0, settings.profileHeightCm, settings.profileAgeYears, settings.profileActivityLevel, settings.profileGoal)
        NutritionUiState(date, foods.groupBy { MealType.valueOf(it.mealType) }, target, foods.sumOf { it.calories })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionUiState())
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val deleted = MutableStateFlow<FoodLogEntryEntity?>(null)
    fun selectDay(date: LocalDate) { saved["selectedDay"] = if (date == LocalDate.now()) null else date.toEpochDay() }
    private fun run(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Could not save." }
            finally { busy.value = false }
        }
    }
    fun deleteEntry(entry: FoodLogEntryEntity) = run { repository.deleteLogEntry(entry.id); deleted.value = entry }
    fun undo() = run { deleted.value?.let { repository.restoreEntry(it); deleted.value = null } }
    fun update(entry: FoodLogEntryEntity) = run { repository.updateEntry(entry) }
}
