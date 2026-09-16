package com.personal.fittrack.ui.progress

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.personal.fittrack.FitTrackApp
import com.personal.fittrack.data.prefs.AppSettings
import com.personal.fittrack.domain.*
import com.personal.fittrack.ui.components.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProgressScreen(initialTab: Int = 0, openWeightDialog: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Your progress") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Training", "Body weight").forEachIndexed { i, title -> Tab(tab == i, { tab = i }, text = { Text(title) }) }
            }
            if (tab == 0) WorkoutProgressTab() else BodyWeightProgressTab(openWeightDialog)
        }
    }
}

@Composable
private fun PeriodPicker(period: Int, onChange: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30, 90, 0).forEach { days -> FilterChip(period == days, { onChange(days) }, label = { Text(if (days == 0) "All time" else "$days days") }) }
    }
}

@Composable
private fun WorkoutProgressTab() {
    val container = (LocalContext.current.applicationContext as FitTrackApp).container
    val exercises by container.workoutRepository.observeExercises().collectAsStateWithLifecycle(emptyList())
    val sets by container.workoutRepository.observeAllSets().collectAsStateWithLifecycle(emptyList())
    val settings by container.userPreferences.settings.collectAsStateWithLifecycle(AppSettings())
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var period by rememberSaveable { mutableIntStateOf(90) }
    val unit = settings.weightUnit
    val cutoff = if (period == 0) 0L else LocalDate.now().minusDays(period.toLong() - 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val history = sets.filter { it.exerciseId == selected && it.completedAtEpochMillis >= cutoff }.sortedBy { it.completedAtEpochMillis }
    val sessions = history.groupBy { it.sessionId }.values.sortedBy { it.first().completedAtEpochMillis }
    val dates = sessions.map { it.first().completedAtEpochMillis }
    fun date(value: Long?) = value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yy")) }.orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { OutlinedButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth()) { Text(exercises.find { it.id == selected }?.name ?: "Choose an exercise") }; PeriodPicker(period) { period = it } }
        if (selected == null) item { Text("Choose an exercise to compare your sessions and see how your training changes.") }
        else {
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${sessions.size} sessions · ${history.size} sets", style = MaterialTheme.typography.titleMedium)
                Text("Best weight: ${unit.format(history.maxOfOrNull { it.weightKg } ?: 0.0)}")
                Text("Most reps in a set: ${history.maxOfOrNull { it.reps } ?: 0}")
            } } }
            item { Text("Heaviest weight per session", style = MaterialTheme.typography.titleMedium); SimpleLineChart(sessions.map { unit.display(it.maxOf { set -> set.weightKg }).toFloat() }, dates = dates, startLabel = date(dates.firstOrNull()), endLabel = date(dates.lastOrNull()), unit = unit.name.lowercase()) }
            item { Text("Total reps per session", style = MaterialTheme.typography.titleMedium); SimpleLineChart(sessions.map { it.sumOf { set -> set.reps }.toFloat() }, dates = dates, startLabel = date(dates.firstOrNull()), endLabel = date(dates.lastOrNull()), unit = "reps") }
            item { Text("Training volume per session", style = MaterialTheme.typography.titleMedium); Text("Weight × reps, summed across your sets.", style = MaterialTheme.typography.bodySmall); SimpleLineChart(sessions.map { unit.display(it.sumOf { set -> set.weightKg * set.reps }).toFloat() }, dates = dates, startLabel = date(dates.firstOrNull()), endLabel = date(dates.lastOrNull()), unit = "${unit.name.lowercase()}·reps") }
        }
    }
    if (picker) ExercisePicker(exercises, { selected = it.id; picker = false }, { picker = false })
}

@Composable
private fun BodyWeightProgressTab(openDialog: Boolean) {
    val container = (LocalContext.current.applicationContext as FitTrackApp).container
    val vm: BodyWeightViewModel = viewModel(factory = viewModelFactory { initializer { BodyWeightViewModel(container.bodyWeightRepository) } })
    val state by vm.uiState.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val settings by container.userPreferences.settings.collectAsStateWithLifecycle(AppSettings())
    val unit = settings.weightUnit
    var show by rememberSaveable { mutableStateOf(openDialog) }
    var period by rememberSaveable { mutableIntStateOf(90) }
    val entries = state.entries.filter { period == 0 || it.dateEpochDay >= LocalDate.now().minusDays(period.toLong() - 1).toEpochDay() }
    fun format(value: Double?) = value?.let(unit::format) ?: "—"
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current ${format(state.currentWeightKg)}", style = MaterialTheme.typography.headlineSmall)
                Text("Starting ${format(state.startingWeightKg)} · Change ${format(state.differenceKg)}")
                Button(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("Log body weight") }
            } }
            PeriodPicker(period) { period = it }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { SimpleLineChart(entries.map { unit.display(it.weightKg).toFloat() }, dates = entries.map { it.dateEpochDay }, startLabel = entries.firstOrNull()?.let { LocalDate.ofEpochDay(it.dateEpochDay).toString() }.orEmpty(), endLabel = entries.lastOrNull()?.let { LocalDate.ofEpochDay(it.dateEpochDay).toString() }.orEmpty(), unit = unit.name.lowercase()) }
        item { Text("Weigh-ins", style = MaterialTheme.typography.titleMedium); TextButton(onClick = vm::undo, enabled = !busy) { Text("Undo last deletion") } }
        items(entries.reversed(), key = { it.id }) { entry ->
            OutlinedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(unit.format(entry.weightKg), style = MaterialTheme.typography.titleMedium); Text(LocalDate.ofEpochDay(entry.dateEpochDay).toString()) }
                TextButton(onClick = { vm.delete(entry) }, enabled = !busy) { Text("Delete") }
            } }
        }
    }
    if (show) {
        var input by rememberSaveable { mutableStateOf("") }
        var date by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
        val number = InputValidation.number(input)
        AlertDialog(onDismissRequest = { if (!busy) show = false }, title = { Text("Log body weight") }, text = { Column {
            DayPicker(LocalDate.ofEpochDay(date), { date = it.toEpochDay() })
            NumberField(input, { input = it }, "Weight (${unit.name.lowercase()})", Modifier.fillMaxWidth())
            if (input.isNotEmpty() && (number == null || number <= 0)) Text("Enter a weight greater than zero.", color = MaterialTheme.colorScheme.error)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { TextButton(enabled = !busy && number != null && number > 0, onClick = { vm.logWeight(unit.toKg(number!!), LocalDate.ofEpochDay(date)) { show = false } }) { Text(if (busy) "Saving…" else "Save weight") } }, dismissButton = { TextButton(onClick = { show = false }, enabled = !busy) { Text("Cancel") } })
    }
}
