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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.repository.MealType
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(onAddFood: () -> Unit, onTakePhoto: () -> Unit) {
    val app = LocalContext.current.applicationContext as FitTrackApp
    val container = app.container
    val viewModel: NutritionViewModel = viewModel(factory = viewModelFactory {
        initializer { NutritionViewModel(container.nutritionRepository, container.bodyWeightRepository, container.userPreferences) }
    })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Nutrition") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Daily Goal: ${state.calorieTarget} kcal", style = MaterialTheme.typography.titleMedium)
                        MealType.entries.forEach { meal ->
                            Text("${meal.name.lowercase().replaceFirstChar { it.uppercase() }}: ${state.mealCalories(meal).roundToInt()} kcal")
                        }
                        Text("Consumed: ${state.consumed.roundToInt()} kcal", style = MaterialTheme.typography.titleMedium)
                        Text("Remaining: ${state.remaining.roundToInt()} kcal", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) { Text("Take Food Photo") }
                }
            }
            item {
                OutlinedButton(onClick = onAddFood, modifier = Modifier.fillMaxWidth()) { Text("Add Food") }
            }
            item { Text("Today's log", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp)) }
            state.entriesByMeal.forEach { (meal, entries) ->
                items(entries, key = { it.id }) { entry ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(entry.foodName, style = MaterialTheme.typography.titleMedium)
                                Text("${meal.name.lowercase()} · ${entry.quantityGrams.roundToInt()} g · ${entry.calories.roundToInt()} kcal")
                            }
                            IconButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
