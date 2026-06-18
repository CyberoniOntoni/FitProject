package com.fitproject.droid.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class FitnessColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val separator: Color,
    val fill: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSecondary: Color,
    val accentGradient: Brush,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val prGold: Color,
    val ringMove: Color,
    val ringExercise: Color,
    val ringStand: Color,
    val repsColor: Color,
    val weightColor: Color,
    val restColor: Color,
    val rpeColor: Color,
    val tempoColor: Color,
    val timeColor: Color,
    val useCardShadow: Boolean
)

/** Apple Fitness + Health — dark (default) */
val FitnessDarkScheme = FitnessColorScheme(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceElevated = Color(0xFF2C2C2E),
    surfaceHighlight = Color(0xFF3A3A3C),
    separator = Color.White.copy(alpha = 0.08f),
    fill = Color.White.copy(alpha = 0.08f),
    accent = Color(0xFF9DEB00),
    onAccent = Color(0xFF000000),
    accentSecondary = Color(0xFF00D4FF),
    accentGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF9DEB00), Color(0xFF30D158))
    ),
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.6f),
    textTertiary = Color.White.copy(alpha = 0.35f),
    success = Color(0xFF30D158),
    warning = Color(0xFFFF9F0A),
    error = Color(0xFFFF453A),
    prGold = Color(0xFFFFD60A),
    ringMove = Color(0xFFFF2D55),
    ringExercise = Color(0xFF9DEB00),
    ringStand = Color(0xFF00D4FF),
    repsColor = Color(0xFFBF5AF2),
    weightColor = Color(0xFFFF453A),
    restColor = Color(0xFF30D158),
    rpeColor = Color(0xFFFF9F0A),
    tempoColor = Color(0xFFBF5AF2),
    timeColor = Color(0xFF00D4FF),
    useCardShadow = false
)

/** Apple Health — light */
val FitnessLightScheme = FitnessColorScheme(
    isDark = false,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceElevated = Color.White,
    surfaceHighlight = Color(0xFFE5E5EA),
    separator = Color(0xFF3C3C43).copy(alpha = 0.12f),
    fill = Color(0xFF787880).copy(alpha = 0.12f),
    accent = Color(0xFF34C759),
    onAccent = Color.White,
    accentSecondary = Color(0xFF007AFF),
    accentGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF34C759), Color(0xFF007AFF))
    ),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF3C3C43).copy(alpha = 0.6f),
    textTertiary = Color(0xFF3C3C43).copy(alpha = 0.3f),
    success = Color(0xFF34C759),
    warning = Color(0xFFFF9500),
    error = Color(0xFFFF3B30),
    prGold = Color(0xFFFF9500),
    ringMove = Color(0xFFFF2D55),
    ringExercise = Color(0xFF34C759),
    ringStand = Color(0xFF007AFF),
    repsColor = Color(0xFF5856D6),
    weightColor = Color(0xFFFF3B30),
    restColor = Color(0xFF34C759),
    rpeColor = Color(0xFFFF9500),
    tempoColor = Color(0xFFAF52DE),
    timeColor = Color(0xFF007AFF),
    useCardShadow = true
)

fun AppThemeMode.toColorScheme(): FitnessColorScheme = when (this) {
    AppThemeMode.DARK -> FitnessDarkScheme
    AppThemeMode.LIGHT -> FitnessLightScheme
}