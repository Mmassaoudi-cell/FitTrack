package com.personal.fittrack.ui.nutrition.vision

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.personal.fittrack.data.repository.EstimateConfidence
import kotlinx.coroutines.tasks.await

data class RecognizedLabel(val text: String, val confidence: Float)

/**
 * On-device food recognition using ML Kit's bundled Image Labeling model
 * (Apache 2.0, runs fully offline, no account or API key). This is a general-purpose
 * object/scene classifier, not a dish-specific model, so results are mapped to our
 * local nutrition database via keyword matching and must always be confirmed by the user.
 */
class FoodRecognizer {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): List<RecognizedLabel> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labels = labeler.process(image).await()
        return labels
            .sortedByDescending { it.confidence }
            .map { RecognizedLabel(it.text, it.confidence) }
    }

    fun confidenceBucket(confidence: Float): EstimateConfidence = when {
        confidence >= 0.75f -> EstimateConfidence.HIGH
        confidence >= 0.45f -> EstimateConfidence.MEDIUM
        else -> EstimateConfidence.LOW
    }
}

/** Maps ML Kit's generic labels to the names used in our local food database. */
object FoodLabelMapper {
    private val keywordToFoodName = mapOf(
        "pizza" to "Pizza",
        "hamburger" to "Burger",
        "cheeseburger" to "Burger",
        "burger" to "Burger",
        "sandwich" to "Sandwich",
        "sushi" to "Sushi (mixed roll)",
        "hot dog" to "Hot dog",
        "french fries" to "French fries",
        "fries" to "French fries",
        "rice" to "Rice, cooked",
        "pasta" to "Pasta, cooked",
        "spaghetti" to "Pasta, cooked",
        "bread" to "Bread, white",
        "baked goods" to "Bread, white",
        "egg" to "Eggs",
        "cheese" to "Cheese, cheddar",
        "banana" to "Banana",
        "apple" to "Apple",
        "orange" to "Orange",
        "avocado" to "Avocado",
        "broccoli" to "Broccoli",
        "salad" to "Salad, mixed greens",
        "vegetable" to "Salad, mixed greens",
        "yogurt" to "Yogurt, plain",
        "oatmeal" to "Oatmeal, cooked",
        "peanut butter" to "Peanut butter",
        "coffee" to "Coffee, black",
        "ice cream" to "Ice cream",
        "dessert" to "Ice cream",
        "seafood" to "Fish, white (cooked)",
        "fish" to "Fish, white (cooked)",
        "salmon" to "Salmon (cooked)",
        "tuna" to "Tuna (canned in water)",
        "steak" to "Beef, ground (cooked)",
        "beef" to "Beef, ground (cooked)",
        "chicken" to "Chicken breast (cooked)",
        "potato" to "Potatoes, boiled",
        "pastry" to "French toast / pastry",
        "pancake" to "French toast / pastry"
    )

    fun bestMatch(labels: List<RecognizedLabel>): Pair<RecognizedLabel, String>? {
        for (label in labels) {
            val key = label.text.lowercase()
            keywordToFoodName[key]?.let { return label to it }
            keywordToFoodName.entries.firstOrNull { key.contains(it.key) }?.let { return label to it.value }
        }
        return null
    }
}
