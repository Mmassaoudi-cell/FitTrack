package com.personal.fittrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personal.fittrack.data.db.entity.ExerciseEntity
import com.personal.fittrack.data.db.entity.SetEntryEntity
import com.personal.fittrack.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, error: Boolean = false) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        singleLine = true, isError = error, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier)
}

@Composable
fun DayPicker(day: LocalDate, onChange: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        IconButton(onClick = { onChange(day.minusDays(1)) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous day") }
        TextButton(onClick = { onChange(LocalDate.now()) }) {
            Text(if (day == LocalDate.now()) "Today" else day.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")))
        }
        IconButton(onClick = { onChange(day.plusDays(1)) }, enabled = day < LocalDate.now()) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next day") }
    }
}

@Composable
fun ExercisePicker(exercises: List<ExerciseEntity>, onSelect: (ExerciseEntity) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Choose exercise") }, text = {
        Column {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search name or muscle group") }, modifier = Modifier.fillMaxWidth())
            val matches = exercises.filter { it.name.contains(query, true) || it.category.contains(query, true) }
            if (matches.isEmpty()) Text("No matching exercises", Modifier.padding(16.dp))
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(matches, key = { it.id }) { exercise ->
                    TextButton(onClick = { onSelect(exercise) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                            Text(exercise.category, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
fun SetEditor(set: SetEntryEntity, unit: WeightUnit, onSave: (SetEntryEntity) -> Unit, onDismiss: () -> Unit) {
    var weight by rememberSaveable(set.id) { mutableStateOf("%.2f".format(unit.display(set.weightKg))) }
    var reps by rememberSaveable(set.id) { mutableStateOf(set.reps.toString()) }
    val value = InputValidation.number(weight)
    val count = reps.toIntOrNull()
    val valid = value != null && value >= 0 && count != null && count in 1..999
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit set ${set.setIndex}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(weight, { weight = it }, "Weight (${unit.name.lowercase()})", Modifier.fillMaxWidth())
            NumberField(reps, { reps = it }, "Reps", Modifier.fillMaxWidth())
            if (!valid) Text("Use a nonnegative weight and 1–999 reps.", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = { onSave(set.copy(weightKg = unit.toKg(value!!), reps = count!!)) }) { Text("Save set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
