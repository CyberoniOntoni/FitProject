package com.fitproject.droid.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// BWS+-inspired dark palette with FitPros integration accents
object BWSColors {
    val Background = Color(0xFF0D0D0F)
    val Surface = Color(0xFF1A1A1E)
    val SurfaceElevated = Color(0xFF242428)
    val SurfaceHighlight = Color(0xFF2E2E34)

    val Accent = Color(0xFF00C9B7)
    val AccentSecondary = Color(0xFF3B82F6)
    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00C9B7), Color(0xFF3B82F6))
    )

    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFA0A0A8)
    val TextTertiary = Color(0xFF6B6B73)

    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
    val PrGold = Color(0xFFFFD700)

    // Metric colors (FitPros standard)
    val RepsColor = Color(0xFF513BD1)
    val WeightColor = Color(0xFFFC4747)
    val RestColor = Color(0xFF4BD685)
    val RpeColor = Color(0xFFF5A623)
    val TempoColor = Color(0xFF9B59B6)
    val TimeColor = Color(0xFF3B86D1)

    val CardRadius = 16f
    val ButtonRadius = 12f
    val TabBarHeight = 84f
}

fun Color.Companion.fromHex(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return Color(0xFF000000L or cleanHex.toLong(16))
}

fun metricColor(name: String): Color = when (name) {
    "Reps" -> BWSColors.RepsColor
    "Weight" -> BWSColors.WeightColor
    "RPE" -> BWSColors.RpeColor
    "Rest" -> BWSColors.RestColor
    "Tempo" -> BWSColors.TempoColor
    "Time" -> BWSColors.TimeColor
    else -> BWSColors.TextSecondary
}