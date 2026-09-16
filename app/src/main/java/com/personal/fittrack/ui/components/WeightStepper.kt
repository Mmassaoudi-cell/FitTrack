package com.personal.fittrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("WEIGHT", style = MaterialTheme.typography.labelLarge)
        Text(
            text = "$weightLabel $unitLabel",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        listOf(increments.reversed().map { -it }, increments).forEach { steps ->
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEach { increment ->
                    OutlinedButton(onClick = { onAdjust(increment) }, modifier = Modifier.weight(1f)) {
                        Text((if (increment > 0) "+" else "") + formatIncrement(increment))
                    }
                }
            }
        }
    }
}

private fun formatIncrement(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
