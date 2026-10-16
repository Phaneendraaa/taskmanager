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

val StatusGreen = Color(0xFF5FA777)
val StatusBlue = Color(0xFF6C8FD0)
val StatusRed = Color(0xFFD9695F)
val Amber = Color(0xFFE3A857)

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

// Sage theme
val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFF7F4EB),
    glow = Color(0xFFE4EADB),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE9ECDF),
    field = Color(0xFFEEF1E6),
    text = Color(0xFF1E2A23),
    muted = Color(0xFF6A786F),
    faint = Color(0xFFDDE3D4),
    accent = Color(0xFF3F6B53),
    accent2 = Color(0xFF2D5642),
    accentText = Color(0xFF3F6B53),
    inverse = Color(0xFF26302B),
    onInverse = Color(0xFFFFFFFF)
)

val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF0E1511),
    glow = Color(0xFF16231B),
    card = Color(0xFF17211B),
    cardBorder = Color(0xFF243029),
    field = Color(0xFF1C2820),
    text = Color(0xFFE9F0E8),
    muted = Color(0xFF93A59A),
    faint = Color(0xFF2B3A31),
    accent = Color(0xFF4F9068),
    accent2 = Color(0xFF3B7852),
    accentText = Color(0xFF8CC7A2),
    inverse = Color(0xFFE9F0E8),
    onInverse = Color(0xFF14201A)
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
