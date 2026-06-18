package com.fitproject.droid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppleLightColorScheme = lightColorScheme(
    primary = BWSColors.Accent,
    onPrimary = BWSColors.Surface,
    primaryContainer = BWSColors.Fill,
    onPrimaryContainer = BWSColors.Accent,
    secondary = BWSColors.AccentSecondary,
    onSecondary = BWSColors.Surface,
    background = BWSColors.Background,
    onBackground = BWSColors.TextPrimary,
    surface = BWSColors.Surface,
    onSurface = BWSColors.TextPrimary,
    surfaceVariant = BWSColors.SurfaceHighlight,
    onSurfaceVariant = BWSColors.TextSecondary,
    error = BWSColors.Error,
    onError = BWSColors.Surface,
    outline = BWSColors.Separator
)

object BWSTypography {
    val LargeTitle = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        lineHeight = 41.sp
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
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun FitProjectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppleLightColorScheme,
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