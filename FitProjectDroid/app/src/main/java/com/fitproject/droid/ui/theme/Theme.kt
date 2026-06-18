package com.fitproject.droid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object BWSTypography {
    val LargeTitle = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp
    )
    val Title = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        lineHeight = 34.sp
    )
    val Headline = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif,
        lineHeight = 22.sp
    )
    val Body = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp
    )
    val Callout = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp
    )
    val Subhead = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    )
    val Caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp
    )
    val Footnote = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    )
    val Metric = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif
    )
    val RingLabel = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun FitProjectTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val fitnessColors = themeMode.toColorScheme()
    val materialScheme = if (fitnessColors.isDark) {
        darkColorScheme(
            primary = fitnessColors.accent,
            onPrimary = fitnessColors.onAccent,
            primaryContainer = fitnessColors.surfaceHighlight,
            onPrimaryContainer = fitnessColors.accent,
            secondary = fitnessColors.accentSecondary,
            onSecondary = fitnessColors.onAccent,
            background = fitnessColors.background,
            onBackground = fitnessColors.textPrimary,
            surface = fitnessColors.surface,
            onSurface = fitnessColors.textPrimary,
            surfaceVariant = fitnessColors.surfaceElevated,
            onSurfaceVariant = fitnessColors.textSecondary,
            error = fitnessColors.error,
            onError = fitnessColors.textPrimary,
            outline = fitnessColors.separator
        )
    } else {
        lightColorScheme(
            primary = fitnessColors.accent,
            onPrimary = fitnessColors.onAccent,
            primaryContainer = fitnessColors.fill,
            onPrimaryContainer = fitnessColors.accent,
            secondary = fitnessColors.accentSecondary,
            onSecondary = fitnessColors.onAccent,
            background = fitnessColors.background,
            onBackground = fitnessColors.textPrimary,
            surface = fitnessColors.surface,
            onSurface = fitnessColors.textPrimary,
            surfaceVariant = fitnessColors.surfaceHighlight,
            onSurfaceVariant = fitnessColors.textSecondary,
            error = fitnessColors.error,
            onError = fitnessColors.surface,
            outline = fitnessColors.separator
        )
    }

    CompositionLocalProvider(LocalFitnessColors provides fitnessColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = androidx.compose.material3.Typography(
                headlineLarge = BWSTypography.LargeTitle,
                headlineMedium = BWSTypography.Title,
                headlineSmall = BWSTypography.Headline,
                bodyLarge = BWSTypography.Body,
                bodyMedium = BWSTypography.Callout,
                labelMedium = BWSTypography.Caption,
                bodySmall = BWSTypography.Footnote
            ),
            content = content
        )
    }
}