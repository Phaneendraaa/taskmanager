package com.example.taskmanager

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalPalette.current.text,
    strike: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    serif: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontFamily = if (serif) FontFamily.Serif else null,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
        maxLines = maxLines,
        textAlign = align,
        lineHeight = lineHeight,
        overflow = TextOverflow.Ellipsis
    )
}

/** Flat surface with a hairline border. Radius is capped so every card looks the same family. */
fun Modifier.cardStyle(p: Palette, radius: Dp = 16.dp): Modifier {
    val shape = RoundedCornerShape(minOf(radius, 16.dp))
    return this
        .background(p.card, shape)
        .border(1.dp, p.cardBorder, shape)
        .clip(shape)
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(LocalPalette.current.faint)
    )
}

@Composable
fun CheckCircle(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 26.dp
) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(if (checked) p.accent else Color.Transparent)
                .border(1.5.dp, if (checked) p.accent else p.muted.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                AppIcon(Ic.CHECK, Color.White, size = diameter * 0.62f, stroke = 2.6f)
            }
        }
    }
}

@Composable
fun RoundIconButton(
    ic: Ic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = LocalPalette.current.text,
    bg: Color = LocalPalette.current.card,
    bordered: Boolean = true
) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(if (bordered) Modifier.border(1.dp, p.cardBorder, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(ic, tint, size = size * 0.5f)
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Ic? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    textSize: TextUnit = 16.sp
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    val brush: Brush = if (enabled) SolidColor(p.inverse) else SolidColor(p.faint)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(text, size = textSize, weight = FontWeight.SemiBold, color = if (enabled) p.onInverse else p.muted)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            AppIcon(icon, if (enabled) p.onInverse else p.muted, size = 20.dp, stroke = 2.2f)
        }
    }
}

@Composable
fun Avatar(name: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(p.inverse)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (name.isNotBlank()) {
            Txt(
                name.trim().take(1).uppercase(),
                size = (size.value * 0.42f).sp,
                weight = FontWeight.SemiBold,
                color = p.onInverse,
                serif = true
            )
        } else {
            AppIcon(Ic.PERSON, p.onInverse, size = size * 0.5f)
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    icon: Ic?,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) p.inverse else p.field)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AppIcon(icon, if (selected) p.onInverse else p.muted, size = 17.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(
            label,
            size = 14.sp,
            weight = FontWeight.Medium,
            color = if (selected) p.onInverse else p.text,
            maxLines = 1
        )
    }
}

@Composable
fun GradientProgress(fraction: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, brush: Brush? = null) {
    val p = LocalPalette.current
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "progress")
    val fill: Brush = brush ?: SolidColor(p.accent)
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(p.faint)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(shape)
                .background(fill)
        )
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val outer = RoundedCornerShape(12.dp)
    val inner = RoundedCornerShape(9.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(outer)
            .background(p.field)
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(inner)
                    .background(if (on) p.card else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, p.cardBorder, inner) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Txt(
                    label,
                    size = 14.sp,
                    weight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) p.text else p.muted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SelectPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.card)
            .border(1.dp, p.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(text, size = 13.sp, weight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
        AppIcon(Ic.CHEVRON_DOWN, p.muted, size = 16.dp)
    }
}

@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    singleLine: Boolean = false,
    autoFocus: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onDone: () -> Unit = {}
) {
    val p = LocalPalette.current
    val style = TextStyle(fontSize = size, fontWeight = weight, color = p.text)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (autoFocus) {
            try {
                focus.requestFocus()
            } catch (e: Exception) {
                // field not attached yet; ignore
            }
        }
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focus),
        textStyle = style,
        cursorBrush = SolidColor(p.accent),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = style, color = p.muted.copy(alpha = 0.7f))
                }
                inner()
            }
        }
    )
}

/** Last [weeks] weeks as a grid of small squares, filled when the habit was done that day. */
@Composable
fun Heatmap(doneDays: Set<Long>, today: Long, weeks: Int = 16) {
    val p = LocalPalette.current
    val gap = 4.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = (maxWidth - gap * (weeks - 1)) / weeks
        val totalHeight = cell * 7 + gap * 6
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            val c = cell.toPx()
            val g = gap.toPx()
            val todayDow = LocalDate.ofEpochDay(today).dayOfWeek.value
            val start = today - (todayDow - 1) - 7L * (weeks - 1)
            for (col in 0 until weeks) {
                for (row in 0 until 7) {
                    val day = start + col * 7L + row
                    if (day > today) continue
                    drawRoundRect(
                        color = if (day in doneDays) p.accent else p.faint,
                        topLeft = Offset(col * (c + g), row * (c + g)),
                        size = Size(c, c),
                        cornerRadius = CornerRadius(c * 0.28f)
                    )
                }
            }
        }
    }
}
