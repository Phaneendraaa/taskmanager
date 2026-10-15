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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun SmallCaps(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
fun ScreenHeader(title: String, now: LocalDateTime) {
    Column(Modifier.padding(top = 20.dp, bottom = 24.dp)) {
        SmallCaps(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.displayMedium)
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallCaps(title, Modifier.weight(1f), MaterialTheme.colorScheme.onSurface)
        SmallCaps(count.toString())
        if (actionLabel != null) {
            Spacer(Modifier.width(16.dp))
            Text(
                actionLabel,
                modifier = Modifier.clickable(onClick = onAction),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(2.dp))
        SmallCaps(label)
    }
}

@Composable
fun CircleCheck(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 24.dp
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
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
                .background(if (checked) ink else Color.Transparent)
                .border(1.5.dp, ink.copy(alpha = if (checked) 1f else 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Canvas(Modifier.size(diameter * 0.5f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val path = Path().apply {
                        moveTo(w * 0.05f, h * 0.55f)
                        lineTo(w * 0.38f, h * 0.88f)
                        lineTo(w * 0.95f, h * 0.15f)
                    }
                    drawPath(
                        path = path,
                        color = paper,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) ink else Color.Transparent)
            .border(1.dp, if (selected) ink else line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) paper else ink
        )
    }
}

@Composable
fun AppFab(onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(ink)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add", tint = paper)
    }
}

@Composable
fun ThinProgress(fraction: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.onSurface)
        )
    }
}

@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    autoFocus: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onDone: () -> Unit = {}
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val hint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
        textStyle = textStyle.copy(color = ink),
        cursorBrush = SolidColor(ink),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle, color = hint)
                }
                inner()
            }
        }
    )
}

/** Last [weeks] weeks as a grid of small squares, filled when the habit was done that day. */
@Composable
fun Heatmap(doneDays: Set<Long>, today: Long, weeks: Int = 16) {
    val ink = MaterialTheme.colorScheme.onSurface
    val soft = MaterialTheme.colorScheme.outlineVariant
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
                        color = if (day in doneDays) ink else soft,
                        topLeft = Offset(col * (c + g), row * (c + g)),
                        size = Size(c, c),
                        cornerRadius = CornerRadius(c * 0.25f)
                    )
                }
            }
        }
    }
}
