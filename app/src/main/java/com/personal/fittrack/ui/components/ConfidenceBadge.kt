package com.personal.fittrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personal.fittrack.data.repository.EstimateConfidence

@Composable
fun ConfidenceBadge(confidence: EstimateConfidence, modifier: Modifier = Modifier) {
    val (color, label) = when (confidence) {
        EstimateConfidence.HIGH -> Color(0xFF2E7D32) to "High confidence"
        EstimateConfidence.MEDIUM -> Color(0xFFEF6C00) to "Medium confidence"
        EstimateConfidence.LOW -> Color(0xFFC62828) to "Low confidence"
    }
    Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
