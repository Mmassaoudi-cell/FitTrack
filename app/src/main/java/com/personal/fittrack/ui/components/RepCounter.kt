package com.personal.fittrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RepCounter(reps: Int, onIncrement: () -> Unit, onDecrement: () -> Unit, onReset: () -> Unit, onCopyPrevious: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onDecrement, modifier = Modifier.size(64.dp), contentPadding = PaddingValues(0.dp)) { Text("-1", fontSize = 20.sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REPS", style = MaterialTheme.typography.labelLarge)
                Text(reps.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onIncrement, modifier = Modifier.size(64.dp), contentPadding = PaddingValues(0.dp)) { Text("+1", fontSize = 20.sp) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onReset) { Text("Reset reps") }
            onCopyPrevious?.let { TextButton(onClick = it) { Text("Copy previous set") } }
        }
    }
}
