package com.fitproject.droid.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Apple Human Interface Guidelines — light mode palette */
object BWSColors {
    val Background = Color(0xFFF2F2F7)
    val Surface = Color.White
    val SurfaceElevated = Color.White
    val SurfaceHighlight = Color(0xFFE5E5EA)
    val Separator = Color(0xFF3C3C43).copy(alpha = 0.12f)
    val Fill = Color(0xFF787880).copy(alpha = 0.12f)

    val Accent = Color(0xFF007AFF)
    val AccentSecondary = Color(0xFF5856D6)
    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF0056CC))
    )

    val TextPrimary = Color(0xFF000000)
    val TextSecondary = Color(0xFF3C3C43).copy(alpha = 0.6f)
    val TextTertiary = Color(0xFF3C3C43).copy(alpha = 0.3f)

    val Success = Color(0xFF34C759)
    val Warning = Color(0xFFFF9500)
    val Error = Color(0xFFFF3B30)
    val PrGold = Color(0xFFFF9500)

    // Metric colors — softened for light backgrounds
    val RepsColor = Color(0xFF5856D6)
    val WeightColor = Color(0xFFFF3B30)
    val RestColor = Color(0xFF34C759)
    val RpeColor = Color(0xFFFF9500)
    val TempoColor = Color(0xFFAF52DE)
    val TimeColor = Color(0xFF007AFF)

    val CardRadius = 12f
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