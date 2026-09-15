package com.personal.fittrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Minimal dependency-free line chart drawn with Compose Canvas. */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    if (values.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("No data yet", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    if (values.size == 1) {
        Box(modifier = modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text(values.first().toString(), style = MaterialTheme.typography.headlineMedium)
        }
        return
    }

    val minValue = values.min()
    val maxValue = values.max()
    val range = (maxValue - minValue).let { if (it == 0f) 1f else it }

    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - minValue) / range
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        points.forEach { point ->
            drawCircle(color = lineColor, radius = 8f, center = point)
        }
    }
}
