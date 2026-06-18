package com.fitproject.droid.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WorkoutMetricFormat {
    val highlightedMetrics = setOf("Reps", "Rest", "RPE", "Tempo")

    fun isHighlighted(name: String): Boolean = name in highlightedMetrics

    fun fieldWidth(name: String): Dp = when (name) {
        "Reps", "Rest", "RPE", "Tempo" -> 68.dp
        else -> 58.dp
    }

    fun formatTempoDisplay(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val digits = value.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return digits.take(3).padStart(3, '0').trimStart('0').ifEmpty { "0" }
    }

    fun sanitizeTempoInput(input: String): String =
        input.filter { it.isDigit() }.take(3)

    fun sanitizeMetricInput(name: String, input: String): String = when (name) {
        "Tempo" -> sanitizeTempoInput(input)
        "Reps", "Rest", "RPE" -> input.filter { it.isDigit() || it == '.' }.take(6)
        else -> input
    }
}