package com.personal.fittrack.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.personal.fittrack.data.prefs.AppTheme
import com.personal.fittrack.domain.*
import com.personal.fittrack.ui.components.NumberField
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(onRestored: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FitTrackApp
    val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(app.container.userPreferences, app.container.exportImportManager) } })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val recovery by vm.recovery.collectAsStateWithLifecycle()
    var age by rememberSaveable(settings.profileAgeYears) { mutableStateOf(settings.profileAgeYears.toString()) }
    var height by rememberSaveable(settings.profileHeightCm) { mutableStateOf(settings.profileHeightCm.toString()) }
    var sex by rememberSaveable(settings.profileSex) { mutableStateOf(settings.profileSex) }
    var activity by rememberSaveable(settings.profileActivityLevel) { mutableStateOf(settings.profileActivityLevel) }
    var goal by rememberSaveable(settings.profileGoal) { mutableStateOf(settings.profileGoal) }
    var increment by rememberSaveable(settings.weightIncrementKg, settings.weightUnit) { mutableStateOf("%.2f".format(settings.weightUnit.display(settings.weightIncrementKg))) }
    var target by rememberSaveable(settings.calorieTargetOverride) { mutableStateOf(settings.calorieTargetOverride?.toString() ?: "") }
    fun share(file: File) {
        val uri = app.container.exportImportManager.shareUri(file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Save FitTrack backup"))
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::inspect) }
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            message?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item {
                Text("Make it yours", style = MaterialTheme.typography.titleLarge)
                Text("Weight units", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { WeightUnit.entries.forEach { unit ->
                    FilterChip(settings.weightUnit == unit, { vm.setWeightUnit(unit) }, enabled = !busy, label = { Text(unit.name) })
                } }
                val step = InputValidation.number(increment)
                NumberField(increment, { increment = it }, "Weight step (${settings.weightUnit.name.lowercase()})", Modifier.fillMaxWidth(), step == null || step <= 0)
                TextButton(onClick = { vm.setWeightIncrement(settings.weightUnit.toKg(step!!)) }, enabled = !busy && step != null && step > 0) { Text("Save weight step") }
                Text("Appearance", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AppTheme.entries.forEach { theme ->
                    FilterChip(settings.theme == theme, { vm.setTheme(theme) }, enabled = !busy, label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) })
                } }
            }
            item {
                HorizontalDivider()
                Text("Your profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                Text("Optional. Used for an estimated calorie target; you can override it below.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { BiologicalSex.entries.forEach { value ->
                    FilterChip(sex == value, { sex = value }, label = { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) })
                } }
                NumberField(age, { age = it }, "Age (years)", Modifier.fillMaxWidth())
                NumberField(height, { height = it }, "Height (cm)", Modifier.fillMaxWidth())
            }
            item {
                Text("Activity level", style = MaterialTheme.typography.labelLarge)
                ActivityLevel.entries.forEach { value ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(activity == value, onClick = { activity = value })
                        Text(value.label)
                    }
                }
                Text("Goal", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FitnessGoal.entries.forEach { value ->
                    FilterChip(goal == value, { goal = value }, label = { Text(value.label) })
                } }
                val a = age.toIntOrNull(); val h = InputValidation.number(height)
                val valid = a != null && a in 1..120 && h != null && h > 0
                if (!valid) Text("Enter an age from 1–120 and a height greater than zero.", color = MaterialTheme.colorScheme.error)
                Button(onClick = { vm.setProfile(sex, a!!, h!!, activity, goal) }, enabled = !busy && valid, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
            }
            item {
                val number = target.toIntOrNull()
                NumberField(target, { target = it }, "Daily calorie target (blank = automatic)", Modifier.fillMaxWidth(), target.isNotBlank() && (number == null || number <= 0))
                TextButton(onClick = { vm.setCalorieOverride(number) }, enabled = !busy && (target.isBlank() || (number != null && number > 0))) { Text("Save calorie target") }
                Text("Targets and calorie-burn values are estimates, not medical advice.", style = MaterialTheme.typography.bodySmall)
            }
            item {
                HorizontalDivider()
                Text("Your data, your backup", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                Text("Save workouts, routines, food logs, body weight and settings to a JSON file. Keep a copy outside this app before changing phones or uninstalling.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { vm.export(::share) }, enabled = !busy) { Text("Export backup") }
                    OutlinedButton(onClick = { importPicker.launch(arrayOf("application/json", "text/plain")) }, enabled = !busy) { Text("Restore backup") }
                }
                Text("Automatic Android cloud backup and device transfer are excluded. Food photos are processed on device and removed after analysis.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    preview?.let { data ->
        AlertDialog(onDismissRequest = { if (!busy) vm.cancelImport() }, title = { Text("Replace current data?") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup from " + Instant.ofEpochMilli(data.exportedAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")))
                Text("${data.sessions.size} workouts · ${data.sets.size} sets\n${data.foodLogEntries.size} food entries · ${data.bodyWeights.size} weigh-ins\n${data.routines.size} routines")
                Text("This replaces your current records. A recovery copy of your existing data will be saved first.")
                if (data.settings == null) Text("This older backup has no profile or settings. Your current settings will be kept.")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = vm::restore, enabled = !busy) { Text("Replace and restore") } }, dismissButton = { TextButton(onClick = vm::cancelImport, enabled = !busy) { Text("Cancel") } })
    }
    recovery?.let { file ->
        AlertDialog(onDismissRequest = onRestored, title = { Text("Backup restored") }, text = { Text("Your records are ready. A recovery copy of the previous data is saved on this device. Export it now if you want to keep it outside the app.") },
            confirmButton = { TextButton(onClick = onRestored) { Text("Done") } }, dismissButton = { TextButton(onClick = { share(file) }) { Text("Export previous data") } })
    }
}
