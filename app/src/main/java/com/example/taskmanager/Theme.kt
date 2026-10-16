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

class AccentOption(val key: String, val name: String, val vibe: String, val swatch: Color)

val ACCENTS = listOf(
    AccentOption("sage", "Sage", "Clean and calm", Color(0xFF3F6B53)),
    AccentOption("sky", "Sky", "Fresh and modern", Color(0xFF3F73B5)),
    AccentOption("peach", "Peach", "Warm and elegant", Color(0xFFD9774A)),
    AccentOption("lavender", "Lavender", "Minimal and classy", Color(0xFF7D68C4))
)

private fun pal(
    dark: Boolean, bg: Long, glow: Long, card: Long, border: Long, field: Long, text: Long, muted: Long,
    faint: Long, accent: Long, accent2: Long, accentText: Long, inverse: Long, onInverse: Long
) = Palette(
    dark = dark, bg = Color(bg), glow = Color(glow), card = Color(card), cardBorder = Color(border),
    field = Color(field), text = Color(text), muted = Color(muted), faint = Color(faint),
    accent = Color(accent), accent2 = Color(accent2), accentText = Color(accentText),
    inverse = Color(inverse), onInverse = Color(onInverse)
)

private val SkyLight = pal(false, 0xFFF2F6FB, 0xFFDCE8F7, 0xFFFFFFFF, 0xFFE1E9F3, 0xFFE9F0F8, 0xFF1B2733, 0xFF66788A,
    0xFFD5DFEA, 0xFF3F73B5, 0xFF2E5C96, 0xFF3F73B5, 0xFF222E3B, 0xFFFFFFFF)
private val SkyDark = pal(true, 0xFF0D141C, 0xFF14202C, 0xFF162029, 0xFF22303D, 0xFF1B2833, 0xFFE6EEF5, 0xFF8EA2B5,
    0xFF2A3A49, 0xFF4C86C7, 0xFF3A6FA9, 0xFF8DB8E6, 0xFFE6EEF5, 0xFF14202C)
private val PeachLight = pal(false, 0xFFFBF3EC, 0xFFF7E0D0, 0xFFFFFFFF, 0xFFF1E3D8, 0xFFF7ECE2, 0xFF2B2019, 0xFF87715F,
    0xFFEBDCCD, 0xFFD9774A, 0xFFC2623A, 0xFFC2623A, 0xFF33261D, 0xFFFFFFFF)
private val PeachDark = pal(true, 0xFF17110D, 0xFF241A13, 0xFF1F1711, 0xFF33261C, 0xFF2A1F17, 0xFFF3E9E0, 0xFFB09C8B,
    0xFF3D2E22, 0xFFE08A5E, 0xFFC97244, 0xFFF0B08C, 0xFFF3E9E0, 0xFF241A13)
private val LavenderLight = pal(false, 0xFFF5F2FA, 0xFFE7E0F5, 0xFFFFFFFF, 0xFFE8E3F2, 0xFFEFEAF7, 0xFF231F2E, 0xFF756E88,
    0xFFDDD6EC, 0xFF7D68C4, 0xFF6650AE, 0xFF6650AE, 0xFF2B2638, 0xFFFFFFFF)
private val LavenderDark = pal(true, 0xFF120F1A, 0xFF1C1729, 0xFF191522, 0xFF2A2438, 0xFF211C2E, 0xFFECE8F5, 0xFF9F97B5,
    0xFF342D45, 0xFF8F7BD6, 0xFF7A66C2, 0xFFBCAEF0, 0xFFECE8F5, 0xFF1C1729)

fun paletteFor(dark: Boolean, accent: String): Palette = when (accent) {
    "sky" -> if (dark) SkyDark else SkyLight
    "peach" -> if (dark) PeachDark else PeachLight
    "lavender" -> if (dark) LavenderDark else LavenderLight
    else -> if (dark) DarkPalette else LightPalette
}

@Composable
fun AppTheme(dark: Boolean, accent: String = "sage", content: @Composable () -> Unit) {
    val p = paletteFor(dark, accent)
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
