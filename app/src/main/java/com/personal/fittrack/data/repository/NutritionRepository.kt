package com.personal.fittrack.data.repository

import com.personal.fittrack.data.db.dao.FoodDao
import com.personal.fittrack.data.db.entity.FoodItemEntity
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import com.personal.fittrack.data.seed.DefaultFoods
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import com.personal.fittrack.domain.currentDay
import com.personal.fittrack.domain.InputValidation
import java.time.LocalDate

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
enum class FoodSource { MANUAL, PHOTO }
enum class EstimateConfidence { HIGH, MEDIUM, LOW }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NutritionRepository(private val dao: FoodDao) {

    suspend fun ensureSeeded() {
        if (dao.countFoodItems() == 0) {
            dao.insertFoodItems(DefaultFoods.list)
        }
    }

    fun observeAllFoodItems(): Flow<List<FoodItemEntity>> = dao.observeAllFoodItems()

    suspend fun searchFoodItems(query: String): List<FoodItemEntity> = dao.searchFoodItems(query)

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun addCustomFoodItem(
        name: String,
        caloriesPer100g: Double,
        proteinPer100g: Double = 0.0,
        carbsPer100g: Double = 0.0,
        fatPer100g: Double = 0.0
    ): Long = dao.insertFoodItem(
        FoodItemEntity(
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            isCustom = true
        )
    )

    fun observeLogForDay(date: LocalDate? = null): Flow<List<FoodLogEntryEntity>> =
        if (date != null) dao.observeLogForDay(date.toEpochDay()) else
            currentDay().flatMapLatest { (day, _) -> dao.observeLogForDay(day.toEpochDay()) }

    suspend fun restoreEntry(entry: FoodLogEntryEntity) = dao.insertLogEntry(entry)
    suspend fun updateEntry(entry: FoodLogEntryEntity) {
        InputValidation.food(entry.foodName, entry.quantityGrams, entry.calories, entry.protein, entry.carbs, entry.fat)
        dao.updateLogEntry(entry)
    }

    suspend fun logFood(
        mealType: MealType,
        foodItem: FoodItemEntity?,
        foodName: String,
        quantityGrams: Double,
        caloriesPer100g: Double,
        proteinPer100g: Double = 0.0,
        carbsPer100g: Double = 0.0,
        fatPer100g: Double = 0.0,
        confidence: EstimateConfidence? = null,
        source: FoodSource = FoodSource.MANUAL,
        date: LocalDate = LocalDate.now(),
        saveToLibrary: Boolean = false
    ): Long {
        InputValidation.food(foodName, quantityGrams, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g)
        val factor = quantityGrams / 100.0
        require(listOf(caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g).all { (it * factor).isFinite() }) { "Nutrition values are too large." }
        val entry = FoodLogEntryEntity(
                dateEpochDay = date.toEpochDay(),
                mealType = mealType.name,
                foodItemId = foodItem?.id,
                foodName = foodName,
                quantityGrams = quantityGrams,
                calories = caloriesPer100g * factor,
                protein = proteinPer100g * factor,
                carbs = carbsPer100g * factor,
                fat = fatPer100g * factor,
                loggedAtEpochMillis = System.currentTimeMillis(),
                confidence = confidence?.name,
                source = source.name
            )
        return if (saveToLibrary && foodItem == null) dao.insertCustomAndLog(
            FoodItemEntity(name = foodName, caloriesPer100g = caloriesPer100g, proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g, fatPer100g = fatPer100g, isCustom = true), entry
        ) else dao.insertLogEntry(entry)
    }

    suspend fun deleteLogEntry(id: Long) = dao.deleteLogEntry(id)
}
