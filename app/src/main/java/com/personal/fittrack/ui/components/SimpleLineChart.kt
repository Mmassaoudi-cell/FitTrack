package com.personal.fittrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Time spacing is preserved; the accompanying summary is accessible without seeing the line. */
@Composable
fun SimpleLineChart(values: List<Float>, modifier: Modifier = Modifier, dates: List<Long> = emptyList(), startLabel: String = "", endLabel: String = "", unit: String = "") {
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    if (values.isEmpty()) { Text("No entries in this period", modifier.padding(vertical = 24.dp)); return }
    val low = values.min(); val high = values.max()
    val description = "${values.size} entries. First ${values.first()} $unit, latest ${values.last()} $unit. Minimum $low, maximum $high. $startLabel to $endLabel."
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${"%.1f".format(low)}–${"%.1f".format(high)} $unit", style = MaterialTheme.typography.labelSmall)
            Text("Latest ${"%.1f".format(values.last())}", style = MaterialTheme.typography.labelSmall)
        }
        Canvas(Modifier.fillMaxWidth().height(140.dp).padding(8.dp)) {
            val range = (high - low).takeIf { it > 0f }
            val timeSpan = if (dates.size == values.size) (dates.last() - dates.first()).toDouble() else 0.0
            val points = values.mapIndexed { i, value ->
                val x = if (values.size == 1) size.width / 2 else if (timeSpan > 0) ((dates[i] - dates.first()) / timeSpan * size.width).toFloat() else i.toFloat() / (values.size - 1) * size.width
                val y = if (range == null) size.height / 2 else size.height * (1 - (value - low) / range)
                Offset(x, y)
            }
            listOf(0f, 0.5f, 1f).forEach { drawLine(grid, Offset(0f, size.height * it), Offset(size.width, size.height * it), 1.dp.toPx()) }
            points.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, 2.dp.toPx(), StrokeCap.Round) }
            points.forEach { drawCircle(color, 3.dp.toPx(), it) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(startLabel, style = MaterialTheme.typography.labelSmall); Text(endLabel, style = MaterialTheme.typography.labelSmall) }
    }
}
