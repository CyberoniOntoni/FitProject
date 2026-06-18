package com.fitproject.droid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BwsDarkColorScheme = darkColorScheme(
    primary = BWSColors.Accent,
    onPrimary = BWSColors.TextPrimary,
    primaryContainer = BWSColors.SurfaceHighlight,
    onPrimaryContainer = BWSColors.TextPrimary,
    secondary = BWSColors.AccentSecondary,
    onSecondary = BWSColors.TextPrimary,
    background = BWSColors.Background,
    onBackground = BWSColors.TextPrimary,
    surface = BWSColors.Surface,
    onSurface = BWSColors.TextPrimary,
    surfaceVariant = BWSColors.SurfaceElevated,
    onSurfaceVariant = BWSColors.TextSecondary,
    error = BWSColors.Error,
    onError = BWSColors.TextPrimary,
    outline = BWSColors.SurfaceHighlight
)

object BWSTypography {
    val Title = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif
    )
    val Headline = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif
    )
    val Body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
    val Caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
    val Metric = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun FitProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use BWS dark theme to match iOS
    MaterialTheme(
        colorScheme = BwsDarkColorScheme,
        typography = androidx.compose.material3.Typography(
            headlineLarge = BWSTypography.Title,
            headlineMedium = BWSTypography.Headline,
            bodyLarge = BWSTypography.Body,
            labelMedium = BWSTypography.Caption,
            bodyMedium = BWSTypography.Metric
        ),
        content = content
    )
}