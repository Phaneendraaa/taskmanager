package com.example.taskmanager

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val StatusGreen = Color(0xFF2ECC71)
val StatusBlue = Color(0xFF4C8DFF)
val StatusRed = Color(0xFFFF4D5E)
val Amber = Color(0xFFF5B73B)

@Immutable
class Palette(
    val dark: Boolean,
    val bg: Color,
    val glow: Color,
    val card: Color,
    val cardBorder: Color,
    val field: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accent2: Color,
    val accentText: Color,
    val inverse: Color,
    val onInverse: Color
) {
    val gradient: Brush get() = Brush.linearGradient(listOf(accent, accent2))
}

val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFFAF9FF),
    glow = Color(0xFFEBE6FF),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFECE8F7),
    field = Color(0xFFF2F0FA),
    text = Color(0xFF15122B),
    muted = Color(0xFF6F6B85),
    faint = Color(0xFFE6E2F2),
    accent = Color(0xFF6C4DF6),
    accent2 = Color(0xFFA85CFF),
    accentText = Color(0xFF4F46E5),
    inverse = Color(0xFF15122B),
    onInverse = Color(0xFFFFFFFF)
)

val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF0C0A13),
    glow = Color(0xFF1B1235),
    card = Color(0xFF171421),
    cardBorder = Color(0xFF262236),
    field = Color(0xFF1C1829),
    text = Color(0xFFF4F2FB),
    muted = Color(0xFF9B96B3),
    faint = Color(0xFF2A2639),
    accent = Color(0xFF7C5CFF),
    accent2 = Color(0xFFB45CFF),
    accentText = Color(0xFF8B7CFF),
    inverse = Color(0xFFFFFFFF),
    onInverse = Color(0xFF15122B)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent,
            onPrimary = Color.White,
            primaryContainer = p.field,
            onPrimaryContainer = p.text,
            secondary = p.accent,
            onSecondary = Color.White,
            background = p.bg,
            onBackground = p.text,
            surface = p.card,
            onSurface = p.text,
            surfaceVariant = p.field,
            onSurfaceVariant = p.muted,
            outline = p.muted,
            outlineVariant = p.faint,
            surfaceTint = p.accent,
            inverseSurface = p.inverse,
            inverseOnSurface = p.onInverse,
            inversePrimary = p.accent2,
            surfaceContainerLowest = p.bg,
            surfaceContainerLow = p.card,
            surfaceContainer = p.card,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.field
        )
    } else {
        lightColorScheme(
            primary = p.accent,
            onPrimary = Color.White,
            primaryContainer = p.field,
            onPrimaryContainer = p.text,
            secondary = p.accent,
            onSecondary = Color.White,
            background = p.bg,
            onBackground = p.text,
            surface = p.card,
            onSurface = p.text,
            surfaceVariant = p.field,
            onSurfaceVariant = p.muted,
            outline = p.muted,
            outlineVariant = p.faint,
            surfaceTint = p.accent,
            inverseSurface = p.inverse,
            inverseOnSurface = p.onInverse,
            inversePrimary = p.accent2,
            surfaceContainerLowest = p.bg,
            surfaceContainerLow = p.card,
            surfaceContainer = p.card,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.field
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
