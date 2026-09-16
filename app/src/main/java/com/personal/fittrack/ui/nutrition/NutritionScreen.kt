package com.personal.fittrack.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.db.entity.FoodLogEntryEntity
import com.personal.fittrack.data.repository.MealType
import com.personal.fittrack.domain.InputValidation
import com.personal.fittrack.ui.components.*

@Composable
fun NutritionScreen(onAddFood: (Long) -> Unit, onTakePhoto: () -> Unit) {
    val container = (LocalContext.current.applicationContext as FitTrackApp).container
    val vm: NutritionViewModel = viewModel(factory = viewModelFactory { initializer { NutritionViewModel(container.nutritionRepository, container.bodyWeightRepository, container.userPreferences, createSavedStateHandle()) } })
    val state by vm.uiState.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FoodLogEntryEntity?>(null) }
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Nutrition") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { DayPicker(state.day, vm::selectDay) }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${state.consumed.toInt()} / ${state.calorieTarget} kcal", style = MaterialTheme.typography.headlineMedium)
                    LinearProgressIndicator(progress = { (state.consumed / state.calorieTarget.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(if (state.remaining >= 0) "${state.remaining.toInt()} kcal remaining" else "${(-state.remaining).toInt()} kcal above target")
                    Text("Protein ${state.protein.toInt()} g · Carbs ${state.carbs.toInt()} g · Fat ${state.fat.toInt()} g")
                    Text("Target uses your current profile. Custom entries contain only the nutrition you enter.", style = MaterialTheme.typography.bodySmall)
                } }
            }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onAddFood(state.day.toEpochDay()) }) { Text("Log food") }
                OutlinedButton(onClick = onTakePhoto) { Text("Food photo") }
            } }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (deleted != null) item { TextButton(onClick = vm::undo, enabled = !busy) { Text("Entry deleted · Undo") } }
            if (state.entriesByMeal.isEmpty()) item { Text("No food logged for this day. Add a meal to see your totals.") }
            MealType.entries.forEach { meal ->
                val entries = state.entriesByMeal[meal].orEmpty()
                if (entries.isNotEmpty()) {
                    item { Text("${meal.name.lowercase().replaceFirstChar { it.uppercase() }} · ${state.mealCalories(meal).toInt()} kcal", style = MaterialTheme.typography.titleMedium) }
                    items(entries, key = { it.id }) { entry ->
                        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                            Text(entry.foodName, style = MaterialTheme.typography.titleMedium)
                            Text("${entry.quantityGrams.toInt()} g · ${entry.calories.toInt()} kcal")
                            FlowRow { TextButton(onClick = { editing = entry }, enabled = !busy) { Text("Edit") }; TextButton(onClick = { vm.deleteEntry(entry) }, enabled = !busy) { Text("Delete") } }
                        } }
                    }
                }
            }
        }
    }
    editing?.let { entry ->
        var name by rememberSaveable(entry.id) { mutableStateOf(entry.foodName) }
        var grams by rememberSaveable(entry.id) { mutableStateOf(entry.quantityGrams.toString()) }
        var meal by rememberSaveable(entry.id) { mutableStateOf(entry.mealType) }
        val quantity = InputValidation.number(grams)
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Edit food entry") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth())
            NumberField(grams, { grams = it }, "Portion (grams)", Modifier.fillMaxWidth())
            FlowRow { MealType.entries.forEach { value -> FilterChip(meal == value.name, { meal = value.name }, label = { Text(value.name.lowercase()) }) } }
            Text("Calories and macros scale with the portion.", style = MaterialTheme.typography.bodySmall)
        } }, confirmButton = { TextButton(enabled = name.isNotBlank() && quantity != null && quantity > 0 && entry.quantityGrams > 0, onClick = {
            val ratio = quantity!! / entry.quantityGrams
            vm.update(entry.copy(foodName = name.trim(), mealType = meal, quantityGrams = quantity, calories = entry.calories * ratio, protein = entry.protein * ratio, carbs = entry.carbs * ratio, fat = entry.fat * ratio))
            editing = null
        }) { Text("Save entry") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
}
