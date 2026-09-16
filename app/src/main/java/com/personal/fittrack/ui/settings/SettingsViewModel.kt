package com.personal.fittrack.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.data.prefs.*
import com.personal.fittrack.data.export.*
import com.personal.fittrack.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class SettingsViewModel(private val preferences: UserPreferences, private val backups: DataExportImportManager) : ViewModel() {
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val preview = MutableStateFlow<BackupData?>(null)
    val recovery = MutableStateFlow<File?>(null)
    private fun run(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; message.value = null
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message.value = e.message ?: "Unable to save. Please try again." }
            finally { busy.value = false }
        }
    }
    fun setWeightUnit(unit: WeightUnit) = run { preferences.setWeightUnit(unit) }
    fun setWeightIncrement(kg: Double) = run { preferences.setWeightIncrementKg(kg); message.value = "Weight increment saved." }
    fun setCalorieOverride(value: Int?) = run { preferences.setCalorieTargetOverride(value); message.value = "Calorie target saved." }
    fun setTheme(theme: AppTheme) = run { preferences.setTheme(theme) }
    fun setProfile(sex: BiologicalSex, age: Int, heightCm: Double, activity: ActivityLevel, goal: FitnessGoal) = run {
        preferences.setProfile(sex, age, heightCm, activity, goal); message.value = "Profile saved. Your target has been updated."
    }
    fun inspect(uri: Uri) = run { preview.value = backups.readBackup(uri) }
    fun cancelImport() { preview.value = null }
    fun restore() {
        val data = preview.value ?: return
        run {
            withContext(NonCancellable) { recovery.value = backups.restore(data) }
            preview.value = null
        }
    }
    fun export(onReady: (File) -> Unit) = run { onReady(backups.exportToFile()) }
}
