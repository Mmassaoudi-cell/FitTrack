package com.personal.fittrack.ui.nutrition

import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.db.entity.FoodItemEntity
import com.personal.fittrack.data.repository.*
import com.personal.fittrack.domain.InputValidation
import com.personal.fittrack.ui.nutrition.vision.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.time.LocalDate

class FoodEntryViewModel(app: FitTrackApp, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = app.container.nutritionRepository
    private val recognizer = FoodRecognizer()
    val foods = repo.observeAllFoodItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val done = MutableStateFlow(false)
    val query = saved.getStateFlow("query", "")
    val selectedId = saved.getStateFlow<Long?>("food", null)
    val grams = saved.getStateFlow("grams", "100")
    val meal = saved.getStateFlow("meal", MealType.LUNCH.name)
    val day = saved.getStateFlow("day", LocalDate.now().toEpochDay())
    val name = saved.getStateFlow("name", "")
    val calories = saved.getStateFlow("calories", "")
    val protein = saved.getStateFlow("protein", "0")
    val carbs = saved.getStateFlow("carbs", "0")
    val fat = saved.getStateFlow("fat", "0")
    val saveCustom = saved.getStateFlow("saveCustom", true)
    val photoResult = saved.getStateFlow("photoResult", false)
    val recognition = saved.getStateFlow("recognition", "")
    fun text(key: String, value: String) { saved[key] = value }
    fun initialDay(value: Long) { if (saved.get<Boolean>("routeDateApplied") != true) { saved["day"] = value; saved["routeDateApplied"] = true } }
    fun setDay(value: Long) { saved["day"] = value }
    fun custom(value: Boolean) { saved["saveCustom"] = value }
    fun select(item: FoodItemEntity) {
        saved["food"] = item.id; saved["query"] = ""
    }
    fun clearSelection() { saved["food"] = null }
    private fun run(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = e.message ?: "Could not save. Please try again." }
            finally { busy.value = false }
        }
    }
    fun favorite(item: FoodItemEntity) = run { repo.setFavorite(item.id, !item.isFavorite) }
    fun save() = run {
        val quantity = InputValidation.number(grams.value) ?: error("Enter a valid portion in grams.")
        var item = foods.value.find { it.id == selectedId.value }
        val foodName = item?.name ?: name.value.trim()
        fun number(text: String) = InputValidation.number(text) ?: error("Enter valid nutrition values.")
        val kcal = item?.caloriesPer100g ?: number(calories.value)
        val p = item?.proteinPer100g ?: number(protein.value)
        val c = item?.carbsPer100g ?: number(carbs.value)
        val f = item?.fatPer100g ?: number(fat.value)
        InputValidation.food(foodName, quantity, kcal, p, c, f)
        repo.logFood(MealType.valueOf(meal.value), item, foodName, quantity, kcal, p, c, f,
            confidence = null, source = if (photoResult.value) FoodSource.PHOTO else FoodSource.MANUAL,
            date = LocalDate.ofEpochDay(day.value), saveToLibrary = saveCustom.value)
        done.value = true
    }
    fun analyze(file: File) = run {
        try {
            val labels = withContext(Dispatchers.IO) { recognizer.recognize(getApplication(), Uri.fromFile(file)) }
            val match = FoodLabelMapper.bestMatch(labels)
            val food = match?.let { repo.searchFoodItems(it.second).firstOrNull { item -> item.name == it.second } }
            saved["food"] = food?.id
            saved["recognition"] = if (food == null) "No reliable match. Choose a food below or enter its nutrition manually." else
                "Suggested: ${food.name}. Recognition confidence: ${recognizer.confidenceBucket(match!!.first.confidence).name.lowercase()}. Confirm the food and portion; calories are not measured from the photo."
            saved["photoResult"] = true
        } finally { withContext(Dispatchers.IO + NonCancellable) { file.delete() } }
    }
    fun retake() { saved["photoResult"] = false; saved["food"] = null; saved["recognition"] = "" }
    fun reportError(message: String) { error.value = message }
    override fun onCleared() { recognizer.close(); super.onCleared() }
}
