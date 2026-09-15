package com.personal.fittrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Quick weight adjustment control. [increments] are shown as both negative and positive
 * buttons around the current value, e.g. [2.5, 5.0, 10.0] renders -10 -5 -2.5 | +2.5 +5 +10.
 */
@Composable
fun WeightStepper(
    weightLabel: String,
    unitLabel: String,
    increments: List<Double>,
    onAdjust: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("WEIGHT", style = MaterialTheme.typography.labelLarge)
        Text(
            text = "$weightLabel $unitLabel",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            increments.reversed().forEach { inc ->
                OutlinedButton(onClick = { onAdjust(-inc) }) {
                    Text("-${formatIncrement(inc)}")
                }
            }
            increments.forEach { inc ->
                OutlinedButton(onClick = { onAdjust(inc) }) {
                    Text("+${formatIncrement(inc)}")
                }
            }
        }
    }
}

private fun formatIncrement(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
