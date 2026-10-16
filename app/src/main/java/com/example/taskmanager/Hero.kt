package com.example.taskmanager

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** Sunrise over mountains with a hiker. Drawn in code, adapts to light and dark theme. */
@Composable
fun HeroArt(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val dark = p.dark

        // sky
        val sky = if (dark) {
            listOf(Color(0xFF0E1511), Color(0xFF1A2B22), Color(0xFF3B5A47))
        } else {
            listOf(Color(0xFFDCE6D2), Color(0xFFF1EBD6), Color(0xFFFBE6C4))
        }
        drawRect(Brush.verticalGradient(sky))

        // sun and glow
        val sun = Offset(w * 0.74f, h * 0.47f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x80FFD7B0), Color(0x00FFD7B0)), center = sun, radius = w * 0.40f),
            radius = w * 0.40f,
            center = sun
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFEBD0), Color(0xFFFFB98A)), center = sun, radius = w * 0.12f),
            radius = w * 0.12f,
            center = sun
        )

        fun ridge(color: Color, vararg pts: Float) {
            val path = Path()
            path.moveTo(pts[0] * w, pts[1] * h)
            var i = 2
            while (i + 1 < pts.size) {
                path.lineTo(pts[i] * w, pts[i + 1] * h)
                i += 2
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            drawPath(path, color)
        }

        val far = if (dark) Color(0xFF2F4A3B) else Color(0xFFB9CFB6)
        val mid = if (dark) Color(0xFF243D30) else Color(0xFF93B39A)
        val near = if (dark) Color(0xFF1B3025) else Color(0xFF6E967C)

        ridge(far, 0f, 0.70f, 0.12f, 0.60f, 0.22f, 0.68f, 0.38f, 0.52f, 0.52f, 0.66f, 0.66f, 0.56f, 0.82f, 0.68f, 1f, 0.58f)
        ridge(mid, 0f, 0.80f, 0.15f, 0.70f, 0.30f, 0.80f, 0.50f, 0.66f, 0.70f, 0.82f, 0.85f, 0.74f, 1f, 0.82f)
        ridge(near, 0.50f, 1f, 0.60f, 0.83f, 0.68f, 0.77f, 0.76f, 0.74f, 0.86f, 0.78f, 1f, 0.76f)

        // hiker standing on the cliff
        val ink = if (dark) Color(0xFF0B1410) else Color(0xFF2B3F33)
        val hx = w * 0.77f
        val hy = h * 0.74f
        val u = h * 0.0125f
        drawLine(ink, Offset(hx, hy), Offset(hx, hy - 5f * u), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
        drawLine(ink, Offset(hx, hy - 5f * u), Offset(hx + 0.3f * u, hy - 10f * u), strokeWidth = 2.6f * u, cap = StrokeCap.Round)
        drawRoundRect(ink, Offset(hx - 3.2f * u, hy - 10f * u), Size(2.4f * u, 4.2f * u), CornerRadius(u))
        drawCircle(ink, 1.5f * u, Offset(hx + 0.3f * u, hy - 11.8f * u))
        drawLine(ink, Offset(hx + 0.5f * u, hy - 8.5f * u), Offset(hx + 3f * u, hy - 5.2f * u), strokeWidth = 1.2f * u, cap = StrokeCap.Round)

        // birds
        val birdColor = if (dark) Color(0xFFDDEBDD) else Color(0xFF55725F)
        val bw = 1.4f * density
        fun bird(cx: Float, cy: Float, sz: Float) {
            val path = Path()
            path.moveTo(cx - sz, cy)
            path.quadraticBezierTo(cx - sz * 0.5f, cy - sz * 0.7f, cx, cy)
            path.quadraticBezierTo(cx + sz * 0.5f, cy - sz * 0.7f, cx + sz, cy)
            drawPath(path, birdColor, style = Stroke(width = bw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        bird(w * 0.62f, h * 0.20f, 7f * density)
        bird(w * 0.69f, h * 0.15f, 5f * density)

        // fade into the page background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, p.bg),
                startY = h * 0.58f,
                endY = h
            )
        )
    }
}
