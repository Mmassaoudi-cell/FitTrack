package com.personal.fittrack.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.db.entity.FoodItemEntity
import com.personal.fittrack.data.repository.MealType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AddFoodScreen(onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val repo = app.container.nutritionRepository
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodItemEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<FoodItemEntity?>(null) }
    var quantity by remember { mutableStateOf(100.0) }
    var mealExpanded by remember { mutableStateOf(false) }
    var meal by remember { mutableStateOf(MealType.LUNCH) }

    // Manual custom entry fields
    var customName by remember { mutableStateOf("") }
    var customCalories by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Add Food") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ExposedDropdownMenuBox(expanded = mealExpanded, onExpandedChange = { mealExpanded = it }) {
                OutlinedTextField(
                    value = meal.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Meal") },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = mealExpanded, onDismissRequest = { mealExpanded = false }) {
                    MealType.entries.forEach { m ->
                        DropdownMenuItem(text = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = {
                            meal = m
                            mealExpanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    scope.launch { results = if (it.length >= 2) repo.searchFoodItems(it) else emptyList() }
                },
                label = { Text("Search food database") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(results, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { selected = item; query = item.name; results = emptyList() }
                    ) {
                        Text(
                            "${item.name} – ${item.caloriesPer100g.roundToInt()} kcal/100g",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            selected?.let { item ->
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Quantity (g)", style = MaterialTheme.typography.labelLarge)
                    Text("${quantity.roundToInt()} g", style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(-50.0, -10.0, 10.0, 50.0).forEach { delta ->
                            Button(onClick = { quantity = (quantity + delta).coerceAtLeast(0.0) }) {
                                Text(if (delta > 0) "+${delta.roundToInt()} g" else "${delta.roundToInt()} g")
                            }
                        }
                    }
                    val estCalories = item.caloriesPer100g * quantity / 100.0
                    Text("Estimated: ${estCalories.roundToInt()} kcal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                repo.logFood(
                                    mealType = meal,
                                    foodItem = item,
                                    foodName = item.name,
                                    quantityGrams = quantity,
                                    caloriesPer100g = item.caloriesPer100g,
                                    proteinPer100g = item.proteinPer100g,
                                    carbsPer100g = item.carbsPer100g,
                                    fatPer100g = item.fatPer100g
                                )
                                onDone()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) { Text("Add to log") }
                }
            }

            if (selected == null) {
                Text("Or enter manually", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 20.dp))
                OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = customCalories, onValueChange = { customCalories = it }, label = { Text("Calories (total for this serving)") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        val cals = customCalories.toDoubleOrNull() ?: return@Button
                        if (customName.isBlank()) return@Button
                        scope.launch {
                            repo.logFood(
                                mealType = meal,
                                foodItem = null,
                                foodName = customName,
                                quantityGrams = 100.0,
                                caloriesPer100g = cals
                            )
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Add custom food") }
            }
        }
    }
}
