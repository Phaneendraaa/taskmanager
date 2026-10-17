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
import androidx.compose.ui.graphics.SolidColor

// Status colors are deliberately muted. They never shout.
val StatusGreen = Color(0xFF3E8E78)
val StatusBlue = Color(0xFF5877A6)
val StatusRed = Color(0xFFB4554B)
val Amber = Color(0xFFB88A3E)

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
    /** Flat on purpose: no gradients anywhere in the app. */
    val gradient: Brush get() = SolidColor(accent)
}

private fun pal(
    dark: Boolean, bg: Long, card: Long, border: Long, field: Long, text: Long, muted: Long,
    faint: Long, accent: Long, accent2: Long, accentText: Long, inverse: Long, onInverse: Long
) = Palette(
    dark = dark, bg = Color(bg), glow = Color(bg), card = Color(card), cardBorder = Color(border),
    field = Color(field), text = Color(text), muted = Color(muted), faint = Color(faint),
    accent = Color(accent), accent2 = Color(accent2), accentText = Color(accentText),
    inverse = Color(inverse), onInverse = Color(onInverse)
)

// Paper and ink (light) and graphite (dark). The accent is the only color.
private fun light(accent: Long, accent2: Long) = pal(
    false, 0xFFF5F4F0, 0xFFFFFFFF, 0xFFE4E1D9, 0xFFEDEBE5, 0xFF17191C, 0xFF6C7077,
    0xFFDAD7CE, accent, accent2, accent, 0xFF17191C, 0xFFF5F4F0
)

private fun dark(accent: Long, accent2: Long, accentText: Long) = pal(
    true, 0xFF0F1012, 0xFF17191C, 0xFF2A2D31, 0xFF1E2124, 0xFFECEDEE, 0xFF8E9399,
    0xFF2D3035, accent, accent2, accentText, 0xFFECEDEE, 0xFF0F1012
)

val LightPalette = light(0xFF22705F, 0xFF1A5A4C)
val DarkPalette = dark(0xFF4DB398, 0xFF3A9A81, 0xFF6CCBB2)

val LocalPalette = staticCompositionLocalOf { LightPalette }

class AccentOption(val key: String, val name: String, val vibe: String, val swatch: Color)

val ACCENTS = listOf(
    AccentOption("sage", "Verdigris", "Calm and clear", Color(0xFF22705F)),
    AccentOption("slate", "Slate", "Cool and precise", Color(0xFF3F5E8C)),
    AccentOption("brass", "Brass", "Warm and refined", Color(0xFF8E6B2F))
)

fun paletteFor(dark: Boolean, accent: String): Palette = when (accent) {
    "slate" -> if (dark) dark(0xFF6F93C9, 0xFF5B7FB6, 0xFF9DB9E3) else light(0xFF3F5E8C, 0xFF314B72)
    "brass" -> if (dark) dark(0xFFC9A05B, 0xFFB38A45, 0xFFE0BE82) else light(0xFF8E6B2F, 0xFF745623)
    else -> if (dark) DarkPalette else LightPalette
}

@Composable
fun AppTheme(dark: Boolean, accent: String = "sage", scale: Float = 0.88f, content: @Composable () -> Unit) {
    val p = paletteFor(dark, accent)
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = Color.White, primaryContainer = p.field,
            onPrimaryContainer = p.text, secondary = p.accent, onSecondary = Color.White,
            background = p.bg, onBackground = p.text, surface = p.card, onSurface = p.text,
            surfaceVariant = p.field, onSurfaceVariant = p.muted, outline = p.muted,
            outlineVariant = p.faint, surfaceTint = p.accent, inverseSurface = p.inverse,
            inverseOnSurface = p.onInverse, inversePrimary = p.accent2,
            surfaceContainerLowest = p.bg, surfaceContainerLow = p.card, surfaceContainer = p.card,
            surfaceContainerHigh = p.card, surfaceContainerHighest = p.field
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = Color.White, primaryContainer = p.field,
            onPrimaryContainer = p.text, secondary = p.accent, onSecondary = Color.White,
            background = p.bg, onBackground = p.text, surface = p.card, onSurface = p.text,
            surfaceVariant = p.field, onSurfaceVariant = p.muted, outline = p.muted,
            outlineVariant = p.faint, surfaceTint = p.accent, inverseSurface = p.inverse,
            inverseOnSurface = p.onInverse, inversePrimary = p.accent2,
            surfaceContainerLowest = p.bg, surfaceContainerLow = p.card, surfaceContainer = p.card,
            surfaceContainerHigh = p.card, surfaceContainerHighest = p.field
        )
    }
    // Display size: shrinks the whole layout a little so more fits on screen without scrolling.
    val base = androidx.compose.ui.platform.LocalDensity.current
    val scaled = androidx.compose.ui.unit.Density(base.density * scale, base.fontScale)
    CompositionLocalProvider(LocalPalette provides p, androidx.compose.ui.platform.LocalDensity provides scaled) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
