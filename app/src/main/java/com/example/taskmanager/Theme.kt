package com.example.taskmanager

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFEFEF),
    onPrimaryContainer = Color(0xFF0A0A0A),
    secondary = Color(0xFF0A0A0A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFEFEF),
    onSecondaryContainer = Color(0xFF0A0A0A),
    tertiary = Color(0xFF0A0A0A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF4F4F4),
    onSurfaceVariant = Color(0xFF6B6B6B),
    outline = Color(0xFF9A9A9A),
    outlineVariant = Color(0xFFE2E2E2),
    surfaceTint = Color(0xFF0A0A0A),
    inverseSurface = Color(0xFF0A0A0A),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFBDBDBD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF7F7F7),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF0F0F0)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFF5F5F5),
    onSecondary = Color(0xFF0A0A0A),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFF5F5F5),
    tertiary = Color(0xFFF5F5F5),
    onTertiary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF9B9B9B),
    outline = Color(0xFF6B6B6B),
    outlineVariant = Color(0xFF2A2A2A),
    surfaceTint = Color(0xFFF5F5F5),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF0A0A0A),
    inversePrimary = Color(0xFF555555),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424)
)

private val Serif = FontFamily.Serif
private val Sans = FontFamily.Default

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp)
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
