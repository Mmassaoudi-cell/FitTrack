package com.personal.fittrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.prefs.AppSettings
import com.personal.fittrack.data.prefs.AppTheme
import com.personal.fittrack.data.prefs.UserPreferences
import com.personal.fittrack.domain.ActivityLevel
import com.personal.fittrack.domain.BiologicalSex
import com.personal.fittrack.domain.FitnessGoal
import com.personal.fittrack.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    val settings: StateFlow<AppSettings> = userPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setWeightUnit(unit: WeightUnit) = viewModelScope.launch { userPreferences.setWeightUnit(unit) }
    fun setWeightIncrement(kg: Double) = viewModelScope.launch { userPreferences.setWeightIncrementKg(kg) }
    fun setCalorieOverride(value: Int?) = viewModelScope.launch { userPreferences.setCalorieTargetOverride(value) }
    fun setTheme(theme: AppTheme) = viewModelScope.launch { userPreferences.setTheme(theme) }
    fun setProfile(sex: BiologicalSex, age: Int, heightCm: Double, activity: ActivityLevel, goal: FitnessGoal) =
        viewModelScope.launch { userPreferences.setProfile(sex, age, heightCm, activity, goal) }
}
