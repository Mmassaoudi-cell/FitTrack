package com.personal.fittrack.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.repository.MealType
import com.personal.fittrack.domain.InputValidation
import com.personal.fittrack.ui.components.*
import java.time.LocalDate

@Composable
fun foodEntryViewModel(): FoodEntryViewModel {
    val app = LocalContext.current.applicationContext as FitTrackApp
    return viewModel(factory = viewModelFactory { initializer { FoodEntryViewModel(app, createSavedStateHandle()) } })
}

@Composable
fun AddFoodScreen(onDone: () -> Unit, date: Long? = null) {
    val vm = foodEntryViewModel()
    LaunchedEffect(date) { date?.let(vm::initialDay) }
    FoodEntryForm(vm, onDone)
}

@Composable
fun FoodEntryForm(vm: FoodEntryViewModel, onDone: () -> Unit, onRetake: (() -> Unit)? = null) {
    val foods by vm.foods.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val grams by vm.grams.collectAsStateWithLifecycle()
    val meal by vm.meal.collectAsStateWithLifecycle()
    val day by vm.day.collectAsStateWithLifecycle()
    val name by vm.name.collectAsStateWithLifecycle()
    val calories by vm.calories.collectAsStateWithLifecycle()
    val protein by vm.protein.collectAsStateWithLifecycle()
    val carbs by vm.carbs.collectAsStateWithLifecycle()
    val fat by vm.fat.collectAsStateWithLifecycle()
    val saveCustom by vm.saveCustom.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    val recognition by vm.recognition.collectAsStateWithLifecycle()
    LaunchedEffect(done) { if (done) onDone() }
    androidx.activity.compose.BackHandler(enabled = busy) {}
    val selected = foods.find { it.id == selectedId }
    val quantity = InputValidation.number(grams)
    val kcal = selected?.caloriesPer100g ?: InputValidation.number(calories)
    val valid = quantity != null && quantity > 0 && (selected != null || (name.isNotBlank() && listOf(calories, protein, carbs, fat).all { InputValidation.number(it)?.let { n -> n >= 0 } == true }))
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text(if (onRetake == null) "Log food" else "Review food photo") }, navigationIcon = { IconButton(onClick = onDone, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }, bottomBar = {
        Surface(tonalElevation = 3.dp) { Button(onClick = vm::save, enabled = valid && !busy && !done, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text(if (busy) "Saving…" else "Save food entry") } }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recognition.isNotBlank()) item {
                Card { Column(Modifier.padding(16.dp)) {
                    Text(recognition)
                    onRetake?.let { TextButton(onClick = it) { Text("Retake photo") } }
                } }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                DayPicker(LocalDate.ofEpochDay(day), { vm.setDay(it.toEpochDay()) })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MealType.entries.forEach { value ->
                    FilterChip(meal == value.name, { vm.text("meal", value.name) }, label = { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) })
                } }
            }
            if (selected == null) {
                item {
                    OutlinedTextField(query, { vm.text("query", it) }, label = { Text("Search foods") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text(if (query.isBlank()) "Favorites and common foods" else "Search results", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                }
                val matches = foods.filter { it.name.contains(query, true) }.sortedByDescending { it.isFavorite }.take(if (query.isBlank()) 6 else 30)
                items(matches, key = { it.id }) { item ->
                    OutlinedCard(onClick = { vm.select(item) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(item.name, style = MaterialTheme.typography.titleMedium); Text("${item.caloriesPer100g.toInt()} kcal / 100 g", style = MaterialTheme.typography.bodySmall) }
                            IconButton(onClick = { vm.favorite(item) }) { Icon(if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder, if (item.isFavorite) "Remove favorite" else "Favorite ${item.name}") }
                        }
                    }
                }
                if (matches.isEmpty()) item { Text("No matches. Add a custom food below.") }
                item {
                    HorizontalDivider()
                    Text("Custom food", style = MaterialTheme.typography.titleMedium)
                    Text("Enter values per 100 g from the food label. Adjust your portion below.")
                    OutlinedTextField(name, { vm.text("name", it) }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth())
                    NumberField(calories, { vm.text("calories", it) }, "Calories / 100 g", Modifier.fillMaxWidth())
                    NumberField(protein, { vm.text("protein", it) }, "Protein (g) / 100 g", Modifier.fillMaxWidth())
                    NumberField(carbs, { vm.text("carbs", it) }, "Carbohydrate (g) / 100 g", Modifier.fillMaxWidth())
                    NumberField(fat, { vm.text("fat", it) }, "Fat (g) / 100 g", Modifier.fillMaxWidth())
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(saveCustom, vm::custom); Text("Save to my food library") }
                }
            } else item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(selected.name, style = MaterialTheme.typography.titleLarge)
                    Text("${selected.caloriesPer100g.toInt()} kcal / 100 g")
                    TextButton(onClick = vm::clearSelection) { Text("Change food") }
                } }
            }
            item {
                NumberField(grams, { vm.text("grams", it) }, "Your portion (grams)", Modifier.fillMaxWidth(), quantity == null || quantity <= 0)
                if (quantity != null && quantity > 0 && kcal != null && kcal >= 0) Text("${(quantity * kcal / 100).toInt()} kcal for this portion", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                Text("Portion and preparation affect nutrition. Check the values before saving.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
