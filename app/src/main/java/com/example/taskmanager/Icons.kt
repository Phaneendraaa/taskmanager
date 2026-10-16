package com.example.taskmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Every icon in the app is drawn here, so no icon library is needed. */
enum class Ic {
    SEARCH, MENU, BACK, CLOSE, PLUS, EDIT, MORE_V, MORE_H, CALENDAR, CLOCK, REPEAT, FLAG, MIC, CLIP,
    CHEVRON_DOWN, CHEVRON_LEFT, CHEVRON_RIGHT, ARROW_RIGHT, CHECK, HOME, PERSON, CHART, BRIEFCASE,
    BOOK, DUMBBELL, PLANE, DOC, CROWN, TRASH, TARGET, DRAG, NOTES, EXCLAIM, COPY,
    SUN, MOON, DROP, FLAME, STAR, LEAF, BELL, PLAY, PAUSE, STOP, MUSIC, TIMER, SLIDERS, GEAR, HEART,
    SPARKLE, FOOD, LAPTOP, CLOUD
}

@Composable
fun AppIcon(
    ic: Ic,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    filled: Boolean = false,
    stroke: Float = 1.8f
) {
    Canvas(modifier.size(size)) {
        drawIcon(ic, tint, filled, stroke)
    }
}

private fun DrawScope.drawIcon(ic: Ic, tint: Color, filled: Boolean, stroke: Float) {
    val s = size.minDimension / 24f
    val sw = stroke * s
    val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun o(x: Float, y: Float) = Offset(x * s, y * s)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = sw) {
        drawLine(tint, o(x1, y1), o(x2, y2), strokeWidth = w, cap = StrokeCap.Round)
    }

    fun poly(vararg pts: Float, close: Boolean = false, fill: Boolean = false) {
        val path = Path()
        var i = 0
        while (i + 1 < pts.size) {
            if (i == 0) path.moveTo(pts[0] * s, pts[1] * s) else path.lineTo(pts[i] * s, pts[i + 1] * s)
            i += 2
        }
        if (close) path.close()
        drawPath(path, tint, style = if (fill) Fill else st)
    }

    fun ring(cx: Float, cy: Float, r: Float) {
        drawCircle(tint, radius = r * s, center = o(cx, cy), style = st)
    }

    fun dot(cx: Float, cy: Float, r: Float) {
        drawCircle(tint, radius = r * s, center = o(cx, cy))
    }

    fun box(x: Float, y: Float, w: Float, h: Float, r: Float) {
        drawRoundRect(tint, topLeft = o(x, y), size = Size(w * s, h * s), cornerRadius = CornerRadius(r * s), style = st)
    }


    fun shape(fill: Boolean, block: Path.() -> Unit) {
        val path = Path()
        path.block()
        drawPath(path, tint, style = if (fill) Fill else st)
    }
    fun Path.mv(x: Float, y: Float) = moveTo(x * s, y * s)
    fun Path.cu(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) =
        cubicTo(a * s, b * s, c * s, d * s, e * s, f * s)

    fun crescent(fill: Boolean) {
        val c1x = 12f; val c1y = 12f; val r1 = 8.8f
        val c2x = 16.4f; val c2y = 8.0f; val r2 = 7.4f
        val dx = c2x - c1x; val dy = c2y - c1y
        val d = sqrt(dx * dx + dy * dy)
        val a = (r1 * r1 - r2 * r2 + d * d) / (2f * d)
        val phi = Math.toDegrees(acos((a / r1).toDouble())).toFloat()
        val theta = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        fun rad(deg: Float) = Math.toRadians(deg.toDouble())
        val sx = c1x + r1 * cos(rad(theta + phi)).toFloat()
        val sy = c1y + r1 * sin(rad(theta + phi)).toFloat()
        val ex = c1x + r1 * cos(rad(theta - phi)).toFloat()
        val ey = c1y + r1 * sin(rad(theta - phi)).toFloat()
        val psiA = Math.toDegrees(atan2((ey - c2y).toDouble(), (ex - c2x).toDouble())).toFloat()
        val psiB = Math.toDegrees(atan2((sy - c2y).toDouble(), (sx - c2x).toDouble())).toFloat()
        var toMid = (theta + 180f - psiA) % 360f
        if (toMid > 180f) toMid -= 360f
        if (toMid < -180f) toMid += 360f
        val dir = if (toMid >= 0f) 1f else -1f
        var sweep2 = ((psiB - psiA) * dir) % 360f
        if (sweep2 < 0f) sweep2 += 360f
        sweep2 *= dir
        shape(fill) {
            moveTo(sx * s, sy * s)
            arcTo(Rect((c1x - r1) * s, (c1y - r1) * s, (c1x + r1) * s, (c1y + r1) * s), theta + phi, 360f - 2f * phi, false)
            arcTo(Rect((c2x - r2) * s, (c2y - r2) * s, (c2x + r2) * s, (c2y + r2) * s), psiA, sweep2, false)
            close()
        }
    }

    when (ic) {
        Ic.SUN -> {
            ring(12f, 12f, 4.2f)
            line(12f, 2.8f, 12f, 5f); line(12f, 19f, 12f, 21.2f)
            line(2.8f, 12f, 5f, 12f); line(19f, 12f, 21.2f, 12f)
            line(5.5f, 5.5f, 7.1f, 7.1f); line(16.9f, 16.9f, 18.5f, 18.5f)
            line(5.5f, 18.5f, 7.1f, 16.9f); line(16.9f, 7.1f, 18.5f, 5.5f)
        }
        Ic.MOON -> crescent(filled)
        Ic.DROP -> shape(filled) {
            mv(12f, 3f)
            cu(12f, 3f, 5.5f, 10f, 5.5f, 14.5f)
            cu(5.5f, 18.1f, 8.4f, 21f, 12f, 21f)
            cu(15.6f, 21f, 18.5f, 18.1f, 18.5f, 14.5f)
            cu(18.5f, 10f, 12f, 3f, 12f, 3f)
            close()
        }
        Ic.FLAME -> shape(filled) {
            mv(12f, 3f)
            cu(13f, 7f, 18f, 9.5f, 18f, 15f)
            cu(18f, 18.3f, 15.3f, 21f, 12f, 21f)
            cu(8.7f, 21f, 6f, 18.3f, 6f, 15f)
            cu(6f, 12.5f, 7.5f, 11f, 8.5f, 9.5f)
            cu(9.2f, 11f, 10f, 11.8f, 10.8f, 12f)
            cu(11f, 9f, 10.5f, 6f, 12f, 3f)
            close()
        }
        Ic.STAR -> poly(
            12f, 3f, 14.4f, 9.2f, 21f, 9.6f, 15.9f, 13.8f, 17.6f, 20.2f,
            12f, 16.7f, 6.4f, 20.2f, 8.1f, 13.8f, 3f, 9.6f, 9.6f, 9.2f,
            close = true, fill = filled
        )
        Ic.LEAF -> {
            shape(filled) {
                mv(5f, 19f)
                cu(5f, 10f, 10f, 5f, 20f, 4f)
                cu(20f, 14f, 15f, 19f, 5f, 19f)
                close()
            }
            line(5f, 19f, 13f, 11f)
        }
        Ic.BELL -> {
            shape(filled) {
                mv(6f, 17f)
                cu(7.5f, 15.5f, 7.5f, 13.5f, 7.5f, 11f)
                cu(7.5f, 8f, 9.5f, 6f, 12f, 6f)
                cu(14.5f, 6f, 16.5f, 8f, 16.5f, 11f)
                cu(16.5f, 13.5f, 16.5f, 15.5f, 18f, 17f)
                close()
            }
            line(9.8f, 20f, 14.2f, 20f)
            line(12f, 3.8f, 12f, 6f)
        }
        Ic.PLAY -> poly(8f, 5f, 19f, 12f, 8f, 19f, close = true, fill = true)
        Ic.PAUSE -> {
            poly(7f, 5f, 10.5f, 5f, 10.5f, 19f, 7f, 19f, close = true, fill = true)
            poly(13.5f, 5f, 17f, 5f, 17f, 19f, 13.5f, 19f, close = true, fill = true)
        }
        Ic.STOP -> poly(6.5f, 6.5f, 17.5f, 6.5f, 17.5f, 17.5f, 6.5f, 17.5f, close = true, fill = true)
        Ic.MUSIC -> {
            ring(7f, 17.5f, 2.8f)
            ring(16f, 15.5f, 2.8f)
            line(9.8f, 17.5f, 9.8f, 6f)
            line(18.8f, 15.5f, 18.8f, 4f)
            line(9.8f, 6f, 18.8f, 4f)
        }
        Ic.TIMER -> {
            ring(12f, 13.5f, 7.5f)
            line(9.5f, 3f, 14.5f, 3f)
            line(12f, 3f, 12f, 6f)
            poly(12f, 9.5f, 12f, 13.5f, 14.5f, 15f)
        }
        Ic.SLIDERS -> {
            line(4f, 8f, 20f, 8f)
            line(4f, 16f, 20f, 16f)
            drawCircle(tint, radius = 2.4f * s, center = o(9f, 8f))
            drawCircle(tint, radius = 2.4f * s, center = o(15f, 16f))
        }
        Ic.GEAR -> {
            ring(12f, 12f, 3.2f)
            ring(12f, 12f, 6.8f)
            line(12f, 2.8f, 12f, 5.2f); line(12f, 18.8f, 12f, 21.2f)
            line(2.8f, 12f, 5.2f, 12f); line(18.8f, 12f, 21.2f, 12f)
            line(5.5f, 5.5f, 7.2f, 7.2f); line(16.8f, 16.8f, 18.5f, 18.5f)
            line(5.5f, 18.5f, 7.2f, 16.8f); line(16.8f, 7.2f, 18.5f, 5.5f)
        }
        Ic.HEART -> shape(filled) {
            mv(12f, 20f)
            cu(4f, 14.5f, 3f, 10f, 3f, 8.5f)
            cu(3f, 6f, 5f, 4.5f, 7.2f, 4.5f)
            cu(9.2f, 4.5f, 11f, 5.8f, 12f, 7.5f)
            cu(13f, 5.8f, 14.8f, 4.5f, 16.8f, 4.5f)
            cu(19f, 4.5f, 21f, 6f, 21f, 8.5f)
            cu(21f, 10f, 20f, 14.5f, 12f, 20f)
            close()
        }
        Ic.SPARKLE -> poly(12f, 3f, 14f, 10f, 21f, 12f, 14f, 14f, 12f, 21f, 10f, 14f, 3f, 12f, 10f, 10f, close = true, fill = filled)
        Ic.FOOD -> {
            line(6f, 3f, 6f, 9f); line(10f, 3f, 10f, 9f)
            poly(6f, 9f, 8f, 11.5f, 10f, 9f)
            line(8f, 11.5f, 8f, 21f)
            line(16f, 3f, 16f, 21f)
            poly(16f, 3f, 19.5f, 6.5f, 19.5f, 12f, 16f, 12f)
        }
        Ic.LAPTOP -> {
            box(5f, 5.5f, 14f, 10f, 2f)
            line(3f, 19f, 21f, 19f)
        }
        Ic.CLOUD -> shape(filled) {
            mv(7f, 18f)
            cu(4f, 18f, 3f, 14.5f, 5.5f, 13f)
            cu(5.5f, 9.5f, 10f, 8f, 12.5f, 10.5f)
            cu(15f, 8.5f, 19f, 10.5f, 18.5f, 14f)
            cu(21f, 14.5f, 21f, 18f, 18f, 18f)
            close()
        }
        else -> Unit
    }

    when (ic) {
        Ic.SEARCH -> {
            ring(11f, 11f, 6.5f)
            line(16f, 16f, 20.5f, 20.5f)
        }
        Ic.MENU -> {
            line(4f, 7f, 20f, 7f)
            line(4f, 12f, 15f, 12f)
            line(4f, 17f, 20f, 17f)
        }
        Ic.BACK -> {
            line(19f, 12f, 5f, 12f)
            poly(11f, 6f, 5f, 12f, 11f, 18f)
        }
        Ic.CLOSE -> {
            line(6f, 6f, 18f, 18f)
            line(18f, 6f, 6f, 18f)
        }
        Ic.PLUS -> {
            line(12f, 5f, 12f, 19f)
            line(5f, 12f, 19f, 12f)
        }
        Ic.EDIT -> {
            poly(4f, 20f, 5.2f, 15.2f, 15.8f, 4.6f, 19.4f, 8.2f, 8.8f, 18.8f, close = true)
            line(13.5f, 6.9f, 17.1f, 10.5f)
        }
        Ic.MORE_V -> {
            dot(12f, 5.5f, 1.7f)
            dot(12f, 12f, 1.7f)
            dot(12f, 18.5f, 1.7f)
        }
        Ic.MORE_H -> {
            dot(5.5f, 12f, 1.7f)
            dot(12f, 12f, 1.7f)
            dot(18.5f, 12f, 1.7f)
        }
        Ic.CALENDAR -> {
            box(4f, 5.5f, 16f, 14.5f, 3f)
            line(4f, 10.5f, 20f, 10.5f)
            line(8.5f, 3.5f, 8.5f, 7.5f)
            line(15.5f, 3.5f, 15.5f, 7.5f)
        }
        Ic.CLOCK -> {
            ring(12f, 12f, 8.5f)
            poly(12f, 7.5f, 12f, 12f, 15.5f, 14f)
        }
        Ic.REPEAT -> {
            poly(4f, 12f, 4f, 9.5f, 7f, 6.5f, 19f, 6.5f)
            poly(16.5f, 3.5f, 19.5f, 6.5f, 16.5f, 9.5f)
            poly(20f, 12f, 20f, 14.5f, 17f, 17.5f, 5f, 17.5f)
            poly(7.5f, 14.5f, 4.5f, 17.5f, 7.5f, 20.5f)
        }
        Ic.FLAG -> {
            line(6f, 3.5f, 6f, 21f)
            poly(6f, 4.5f, 18.5f, 4.5f, 15f, 8.75f, 18.5f, 13f, 6f, 13f, close = true, fill = true)
        }
        Ic.MIC -> {
            box(9f, 3f, 6f, 11f, 3f)
            drawArc(tint, 0f, 180f, false, o(5f, 4f), Size(14f * s, 14f * s), style = st)
            line(12f, 18f, 12f, 21f)
            line(9f, 21f, 15f, 21f)
        }
        Ic.CLIP -> {
            rotate(45f, o(12f, 12f)) {
                drawRoundRect(tint, o(8.5f, 2.5f), Size(7f * s, 19f * s), CornerRadius(3.5f * s), style = st)
                line(12f, 7f, 12f, 15.5f)
            }
        }
        Ic.CHEVRON_DOWN -> poly(7f, 10f, 12f, 15f, 17f, 10f)
        Ic.CHEVRON_LEFT -> poly(14f, 6f, 8f, 12f, 14f, 18f)
        Ic.CHEVRON_RIGHT -> poly(10f, 6f, 16f, 12f, 10f, 18f)
        Ic.ARROW_RIGHT -> {
            line(5f, 12f, 19f, 12f)
            poly(13f, 6f, 19f, 12f, 13f, 18f)
        }
        Ic.CHECK -> poly(5f, 12.5f, 10f, 17.5f, 19f, 7.5f)
        Ic.HOME -> {
            poly(
                4f, 11f, 12f, 4f, 20f, 11f, 20f, 19.5f, 14.5f, 19.5f,
                14.5f, 14f, 9.5f, 14f, 9.5f, 19.5f, 4f, 19.5f,
                close = true, fill = filled
            )
        }
        Ic.PERSON -> {
            ring(12f, 8f, 4f)
            drawArc(tint, 180f, 180f, false, o(5f, 14f), Size(14f * s, 14f * s), style = st)
        }
        Ic.CHART -> {
            line(6f, 20f, 6f, 13f, 3.2f * s)
            line(12f, 20f, 12f, 5f, 3.2f * s)
            line(18f, 20f, 18f, 10f, 3.2f * s)
        }
        Ic.BRIEFCASE -> {
            box(3f, 7.5f, 18f, 12.5f, 2.5f)
            poly(9f, 7.5f, 9f, 5.5f, 15f, 5.5f, 15f, 7.5f)
            line(3f, 13f, 21f, 13f)
        }
        Ic.BOOK -> {
            poly(3.5f, 5.5f, 11.5f, 7f, 11.5f, 19.5f, 3.5f, 18f, close = true, fill = filled)
            poly(20.5f, 5.5f, 12.5f, 7f, 12.5f, 19.5f, 20.5f, 18f, close = true, fill = filled)
        }
        Ic.DUMBBELL -> {
            line(5f, 9f, 5f, 15f, 3f * s)
            line(8.5f, 6.5f, 8.5f, 17.5f, 3f * s)
            line(15.5f, 6.5f, 15.5f, 17.5f, 3f * s)
            line(19f, 9f, 19f, 15f, 3f * s)
            line(8.5f, 12f, 15.5f, 12f, 2f * s)
        }
        Ic.PLANE -> {
            poly(21f, 3f, 3f, 10.5f, 10f, 13.2f, 13f, 20.5f, close = true, fill = filled)
            line(10f, 13.2f, 21f, 3f)
        }
        Ic.DOC -> {
            poly(6f, 3f, 14f, 3f, 19f, 8f, 19f, 21f, 6f, 21f, close = true, fill = filled)
            poly(14f, 3f, 14f, 8f, 19f, 8f)
            if (!filled) {
                line(9f, 13f, 16f, 13f)
                line(9f, 17f, 16f, 17f)
            }
        }
        Ic.CROWN -> {
            poly(4f, 18.5f, 3.5f, 7f, 8.5f, 11.5f, 12f, 5f, 15.5f, 11.5f, 20.5f, 7f, 20f, 18.5f, close = true, fill = filled)
        }
        Ic.TRASH -> {
            line(4f, 6.5f, 20f, 6.5f)
            poly(9f, 6.5f, 9f, 4f, 15f, 4f, 15f, 6.5f)
            box(6f, 8.5f, 12f, 11.5f, 2f)
            line(10f, 11.5f, 10f, 17f)
            line(14f, 11.5f, 14f, 17f)
        }
        Ic.TARGET -> {
            ring(12f, 12f, 8.5f)
            ring(12f, 12f, 4.8f)
            dot(12f, 12f, 1.6f)
        }
        Ic.DRAG -> {
            dot(9f, 6f, 1.3f)
            dot(15f, 6f, 1.3f)
            dot(9f, 12f, 1.3f)
            dot(15f, 12f, 1.3f)
            dot(9f, 18f, 1.3f)
            dot(15f, 18f, 1.3f)
        }
        Ic.NOTES -> {
            line(5f, 7f, 19f, 7f)
            line(5f, 12f, 19f, 12f)
            line(5f, 17f, 13f, 17f)
        }
        Ic.EXCLAIM -> {
            line(12f, 6f, 12f, 13.5f)
            dot(12f, 17.5f, 1.4f)
        }
        Ic.COPY -> {
            box(8f, 8f, 12f, 12f, 2.5f)
            poly(16f, 8f, 16f, 5.5f, 5.5f, 5.5f, 5.5f, 16f, 8f, 16f)
        }
        else -> Unit
    }
}
