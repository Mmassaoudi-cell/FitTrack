package com.personal.fittrack.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.personal.fittrack.domain.ActivityLevel
import com.personal.fittrack.domain.BiologicalSex
import com.personal.fittrack.domain.FitnessGoal
import com.personal.fittrack.domain.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val weightIncrementKg: Double = 2.5,
    val calorieTargetOverride: Int? = null,
    val theme: AppTheme = AppTheme.SYSTEM,
    val profileSex: BiologicalSex = BiologicalSex.MALE,
    val profileAgeYears: Int = 30,
    val profileHeightCm: Double = 175.0,
    val profileActivityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val profileGoal: FitnessGoal = FitnessGoal.MAINTAIN,
    val hasCompletedProfile: Boolean = false
)

private val Context.dataStore by preferencesDataStore(name = "fittrack_settings")

class UserPreferences(private val context: Context) {
    private object Keys {
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val WEIGHT_INCREMENT_KG = floatPreferencesKey("weight_increment_kg")
        val CALORIE_TARGET_OVERRIDE = intPreferencesKey("calorie_target_override")
        val THEME = stringPreferencesKey("theme")
        val PROFILE_SEX = stringPreferencesKey("profile_sex")
        val PROFILE_AGE = intPreferencesKey("profile_age")
        val PROFILE_HEIGHT_CM = floatPreferencesKey("profile_height_cm")
        val PROFILE_ACTIVITY = stringPreferencesKey("profile_activity")
        val PROFILE_GOAL = stringPreferencesKey("profile_goal")
        val HAS_COMPLETED_PROFILE = booleanPreferencesKey("has_completed_profile")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs -> prefs.toAppSettings() }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        weightUnit = this[Keys.WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.KG,
        weightIncrementKg = this[Keys.WEIGHT_INCREMENT_KG]?.toDouble() ?: 2.5,
        calorieTargetOverride = this[Keys.CALORIE_TARGET_OVERRIDE],
        theme = this[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
        profileSex = this[Keys.PROFILE_SEX]?.let { runCatching { BiologicalSex.valueOf(it) }.getOrNull() } ?: BiologicalSex.MALE,
        profileAgeYears = this[Keys.PROFILE_AGE] ?: 30,
        profileHeightCm = this[Keys.PROFILE_HEIGHT_CM]?.toDouble() ?: 175.0,
        profileActivityLevel = this[Keys.PROFILE_ACTIVITY]?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() } ?: ActivityLevel.MODERATE,
        profileGoal = this[Keys.PROFILE_GOAL]?.let { runCatching { FitnessGoal.valueOf(it) }.getOrNull() } ?: FitnessGoal.MAINTAIN,
        hasCompletedProfile = this[Keys.HAS_COMPLETED_PROFILE] ?: false
    )

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    suspend fun setWeightIncrementKg(increment: Double) {
        context.dataStore.edit { it[Keys.WEIGHT_INCREMENT_KG] = increment.toFloat() }
    }

    suspend fun setCalorieTargetOverride(value: Int?) {
        context.dataStore.edit {
            if (value == null) it.remove(Keys.CALORIE_TARGET_OVERRIDE) else it[Keys.CALORIE_TARGET_OVERRIDE] = value
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setProfile(
        sex: BiologicalSex,
        ageYears: Int,
        heightCm: Double,
        activityLevel: ActivityLevel,
        goal: FitnessGoal
    ) {
        context.dataStore.edit {
            it[Keys.PROFILE_SEX] = sex.name
            it[Keys.PROFILE_AGE] = ageYears
            it[Keys.PROFILE_HEIGHT_CM] = heightCm.toFloat()
            it[Keys.PROFILE_ACTIVITY] = activityLevel.name
            it[Keys.PROFILE_GOAL] = goal.name
            it[Keys.HAS_COMPLETED_PROFILE] = true
        }
    }
}
