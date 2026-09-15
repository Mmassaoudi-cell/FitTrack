package com.personal.fittrack.data.seed

import com.personal.fittrack.data.db.entity.FoodItemEntity

/**
 * Approximate values per 100g, sourced from typical USDA FoodData Central averages.
 * Intended as a practical offline baseline; users can add/edit their own foods.
 */
object DefaultFoods {
    val list = listOf(
        FoodItemEntity(name = "Chicken breast (cooked)", caloriesPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6),
        FoodItemEntity(name = "Rice, cooked", caloriesPer100g = 130.0, proteinPer100g = 2.7, carbsPer100g = 28.0, fatPer100g = 0.3),
        FoodItemEntity(name = "Pasta, cooked", caloriesPer100g = 158.0, proteinPer100g = 5.8, carbsPer100g = 31.0, fatPer100g = 0.9),
        FoodItemEntity(name = "Bread, white", caloriesPer100g = 265.0, proteinPer100g = 9.0, carbsPer100g = 49.0, fatPer100g = 3.2),
        FoodItemEntity(name = "Eggs", caloriesPer100g = 155.0, proteinPer100g = 13.0, carbsPer100g = 1.1, fatPer100g = 11.0),
        FoodItemEntity(name = "Milk, whole", caloriesPer100g = 61.0, proteinPer100g = 3.2, carbsPer100g = 4.8, fatPer100g = 3.3),
        FoodItemEntity(name = "Cheese, cheddar", caloriesPer100g = 402.0, proteinPer100g = 25.0, carbsPer100g = 1.3, fatPer100g = 33.0),
        FoodItemEntity(name = "Beef, ground (cooked)", caloriesPer100g = 250.0, proteinPer100g = 26.0, carbsPer100g = 0.0, fatPer100g = 17.0),
        FoodItemEntity(name = "Fish, white (cooked)", caloriesPer100g = 105.0, proteinPer100g = 23.0, carbsPer100g = 0.0, fatPer100g = 1.2),
        FoodItemEntity(name = "Salmon (cooked)", caloriesPer100g = 208.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 13.0),
        FoodItemEntity(name = "Tuna (canned in water)", caloriesPer100g = 116.0, proteinPer100g = 26.0, carbsPer100g = 0.0, fatPer100g = 1.0),
        FoodItemEntity(name = "Potatoes, boiled", caloriesPer100g = 87.0, proteinPer100g = 1.9, carbsPer100g = 20.0, fatPer100g = 0.1),
        FoodItemEntity(name = "French fries", caloriesPer100g = 312.0, proteinPer100g = 3.4, carbsPer100g = 41.0, fatPer100g = 15.0),
        FoodItemEntity(name = "Banana", caloriesPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 23.0, fatPer100g = 0.3),
        FoodItemEntity(name = "Apple", caloriesPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 14.0, fatPer100g = 0.2),
        FoodItemEntity(name = "Orange", caloriesPer100g = 47.0, proteinPer100g = 0.9, carbsPer100g = 12.0, fatPer100g = 0.1),
        FoodItemEntity(name = "Avocado", caloriesPer100g = 160.0, proteinPer100g = 2.0, carbsPer100g = 9.0, fatPer100g = 15.0),
        FoodItemEntity(name = "Broccoli", caloriesPer100g = 34.0, proteinPer100g = 2.8, carbsPer100g = 7.0, fatPer100g = 0.4),
        FoodItemEntity(name = "Salad, mixed greens", caloriesPer100g = 20.0, proteinPer100g = 1.5, carbsPer100g = 3.7, fatPer100g = 0.2),
        FoodItemEntity(name = "Yogurt, plain", caloriesPer100g = 61.0, proteinPer100g = 3.5, carbsPer100g = 4.7, fatPer100g = 3.3),
        FoodItemEntity(name = "Oatmeal, cooked", caloriesPer100g = 71.0, proteinPer100g = 2.5, carbsPer100g = 12.0, fatPer100g = 1.5),
        FoodItemEntity(name = "Peanut butter", caloriesPer100g = 588.0, proteinPer100g = 25.0, carbsPer100g = 20.0, fatPer100g = 50.0),
        FoodItemEntity(name = "Pizza", caloriesPer100g = 266.0, proteinPer100g = 11.0, carbsPer100g = 33.0, fatPer100g = 10.0),
        FoodItemEntity(name = "Burger", caloriesPer100g = 295.0, proteinPer100g = 17.0, carbsPer100g = 24.0, fatPer100g = 14.0),
        FoodItemEntity(name = "Sandwich", caloriesPer100g = 250.0, proteinPer100g = 10.0, carbsPer100g = 28.0, fatPer100g = 10.0),
        FoodItemEntity(name = "Coffee, black", caloriesPer100g = 2.0, proteinPer100g = 0.1, carbsPer100g = 0.0, fatPer100g = 0.0),
        FoodItemEntity(name = "Sushi (mixed roll)", caloriesPer100g = 150.0, proteinPer100g = 5.8, carbsPer100g = 27.0, fatPer100g = 2.0),
        FoodItemEntity(name = "Hot dog", caloriesPer100g = 290.0, proteinPer100g = 10.0, carbsPer100g = 18.0, fatPer100g = 20.0),
        FoodItemEntity(name = "Ice cream", caloriesPer100g = 207.0, proteinPer100g = 3.5, carbsPer100g = 24.0, fatPer100g = 11.0),
        FoodItemEntity(name = "French toast / pastry", caloriesPer100g = 350.0, proteinPer100g = 6.0, carbsPer100g = 45.0, fatPer100g = 16.0)
    )
}
