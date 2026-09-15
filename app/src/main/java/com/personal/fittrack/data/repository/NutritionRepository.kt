package com.personal.fittrack.data.repository

import com.personal.fittrack.data.db.dao.FoodDao
import com.personal.fittrack.data.db.entity.FoodItemEntity
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import com.personal.fittrack.data.seed.DefaultFoods
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
enum class FoodSource { MANUAL, PHOTO }
enum class EstimateConfidence { HIGH, MEDIUM, LOW }

class NutritionRepository(private val dao: FoodDao) {

    suspend fun ensureSeeded() {
        if (dao.countFoodItems() == 0) {
            dao.insertFoodItems(DefaultFoods.list)
        }
    }

    fun observeAllFoodItems(): Flow<List<FoodItemEntity>> = dao.observeAllFoodItems()

    suspend fun searchFoodItems(query: String): List<FoodItemEntity> = dao.searchFoodItems(query)

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

    fun observeLogForDay(date: LocalDate = LocalDate.now()): Flow<List<FoodLogEntryEntity>> =
        dao.observeLogForDay(date.toEpochDay())

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
        date: LocalDate = LocalDate.now()
    ): Long {
        val factor = quantityGrams / 100.0
        return dao.insertLogEntry(
            FoodLogEntryEntity(
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
        )
    }

    suspend fun deleteLogEntry(id: Long) = dao.deleteLogEntry(id)
}
