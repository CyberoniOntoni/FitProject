package com.fitproject.droid.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LocalFitnessColors = staticCompositionLocalOf { FitnessDarkScheme }

object BWSColors {
    const val CardRadius = 14f
    const val ButtonRadius = 12f
    const val TabBarHeight = 84f

    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.isDark

    val Background: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.background
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.surface
    val SurfaceElevated: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.surfaceElevated
    val SurfaceHighlight: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.surfaceHighlight
    val Separator: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.separator
    val Fill: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.fill

    val Accent: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.accent
    val OnAccent: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.onAccent
    val AccentSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.accentSecondary
    val AccentGradient: Brush
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.accentGradient

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.textSecondary
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.textTertiary

    val Success: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.success
    val Warning: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.warning
    val Error: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.error
    val PrGold: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.prGold

    val RingMove: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.ringMove
    val RingExercise: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.ringExercise
    val RingStand: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.ringStand

    val RepsColor: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.repsColor
    val WeightColor: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.weightColor
    val RestColor: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.restColor
    val RpeColor: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.rpeColor
    val TempoColor: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.tempoColor
    val TimeColor: Color
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.timeColor

    val UseCardShadow: Boolean
        @Composable @ReadOnlyComposable get() = LocalFitnessColors.current.useCardShadow
}

fun Color.Companion.fromHex(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return Color(0xFF000000L or cleanHex.toLong(16))
}

@Composable
fun metricColor(name: String): Color {
    val c = LocalFitnessColors.current
    return when (name) {
        "Reps" -> c.repsColor
        "Weight" -> c.weightColor
        "RPE" -> c.rpeColor
        "Rest" -> c.restColor
        "Tempo" -> c.tempoColor
        "Time" -> c.timeColor
        else -> c.textSecondary
    }
}