package com.example.taskmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Round progress ring in the accent color. */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, thickness: Dp = 10.dp) {
    val p = LocalPalette.current
    val track = p.faint
    val color = p.accent
    val f = fraction.coerceIn(0f, 1f)
    Canvas(modifier) {
        val stroke = thickness.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke)
        )
        if (f > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

fun habitSubtitle(h: Habit): String {
    val time = h.timeMinute?.let { formatMinute(it) } ?: "Anytime"
    val cue = h.cue.trim().trimEnd('.')
    val first = if (cue.isEmpty()) time else (if (cue.startsWith("after", true)) cue else "After I $cue")
    return if (h.daysMask == 127) first else first + "  •  " + h.frequencyText()
}

/**
 * One habit line. [bar] draws the colored edge used on the Habits tab, [checkAtEnd] moves the
 * tick to the right, and [more] shows the "..." button instead of a chevron.
 */
@Composable
fun HabitRow(
    habit: Habit,
    today: Long,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bar: Boolean = false,
    checkAtEnd: Boolean = false,
    subtitle: String = habitSubtitle(habit)
) {
    val p = LocalPalette.current
    val done = today in habit.doneDays
    val tint = p.muted
    Row(
        modifier
            .fillMaxWidth()
            .cardStyle(p, 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (bar) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .background(p.accent)
            )
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.width(4.dp))
        }
        if (!checkAtEnd) CheckCircle(done, onToggle)
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(p.field),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(habit.iconKind(), tint, size = 18.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Txt(
                habit.name,
                size = 15.sp,
                weight = FontWeight.SemiBold,
                color = if (done) p.muted else p.text,
                maxLines = 1
            )
            Txt(subtitle, size = 12.sp, color = p.muted, maxLines = 1)
        }
        if (checkAtEnd) {
            CheckCircle(done, onToggle)
            AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 18.dp)
            Spacer(Modifier.width(10.dp))
        } else {
            AppIcon(Ic.CHEVRON_RIGHT, p.faint, size = 16.dp)
            Spacer(Modifier.width(12.dp))
        }
    }
}
