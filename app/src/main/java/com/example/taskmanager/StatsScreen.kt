package com.example.taskmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun StatsScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onPickDay: (Long) -> Unit
) {
    val p = LocalPalette.current
    var range by rememberSaveable { mutableIntStateOf(0) }
    val todayDate = vm.now.toLocalDate()
    val today = todayDate.toEpochDay()
    val weekStart = todayDate.minusDays((todayDate.dayOfWeek.value - 1).toLong()).toEpochDay()

    val from: Long
    val to: Long
    when (range) {
        0 -> { from = weekStart; to = weekStart + 6 }
        1 -> {
            from = todayDate.withDayOfMonth(1).toEpochDay()
            to = todayDate.withDayOfMonth(todayDate.lengthOfMonth()).toEpochDay()
        }
        else -> {
            from = LocalDate.of(todayDate.year, 1, 1).toEpochDay()
            to = LocalDate.of(todayDate.year, 12, 31).toEpochDay()
        }
    }
    val inRange = vm.tasks.filter { val d = it.dueDay; d != null && d in from..to }
    val completed = inRange.count { it.done }
    val overdue = inRange.count { !it.done && it.isOverdue(vm.now) }
    val pending = inRange.size - completed - overdue
    val percent = if (inRange.isEmpty()) 0 else completed * 100 / inRange.size

    val doneToday = vm.tasks.count { val c = it.completedAt; it.done && c != null && epochDayOf(c) == today }
    val goal = vm.dailyGoal

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = padding.calculateBottomPadding() + 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt("Your Progress", size = 28.sp, weight = FontWeight.ExtraBold)
                Txt("Consistency creates freedom.", size = 14.sp, color = p.muted)
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { AppIcon(Ic.CROWN, p.accent, size = 26.dp) }
        }
        Spacer(Modifier.height(18.dp))
        SegmentedControl(listOf("Week", "Month", "Year"), range, { range = it })
        Spacer(Modifier.height(16.dp))

        // Week strip with a status dot per day.
        Row(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(vertical = 12.dp, horizontal = 6.dp)) {
            for (i in 0..6) {
                val day = weekStart + i
                val items = vm.tasks.filter { it.dueDay == day }
                val dot = when {
                    items.isEmpty() -> p.faint
                    items.all { it.done } -> StatusGreen
                    items.any { !it.done && it.isOverdue(vm.now) } -> StatusRed
                    else -> StatusBlue
                }
                Column(
                    Modifier.weight(1f).clickable { onPickDay(day) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Txt(dayInitial(day), size = 12.sp, color = p.muted, weight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Txt(
                        dayOfMonth(day).toString(),
                        size = 15.sp,
                        weight = if (day == today) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (day == today) p.accent else p.text
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Completion ring and counts.
        Row(
            Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Ring(
                    listOf(completed.toFloat() to null),
                    total = inRange.size.toFloat(),
                    track = p.field,
                    thickness = 14.dp,
                    gradient = p.gradient,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("$percent%", size = 28.sp, weight = FontWeight.ExtraBold)
                    Txt("Completed", size = 12.sp, color = p.muted)
                }
            }
            Spacer(Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CountRow("Completed", completed, StatusGreen)
                CountRow("Pending", pending, StatusBlue)
                CountRow("Overdue", overdue, StatusRed)
            }
        }
        Spacer(Modifier.height(16.dp))

        // Daily goal.
        Column(Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(Ic.TARGET, p.accent, size = 22.dp)
                Spacer(Modifier.size(10.dp))
                Txt("Daily Goal", size = 16.sp, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Txt("$doneToday of $goal tasks", size = 14.sp, color = p.muted)
            }
            Spacer(Modifier.height(12.dp))
            GradientProgress(doneToday.toFloat() / goal.toFloat(), height = 10.dp)
        }
        Spacer(Modifier.height(16.dp))

        // Tasks by category.
        val byCat = Category.values().map { c -> c to inRange.count { it.category == c } }.filter { it.second > 0 }
        Column(Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(18.dp)) {
            Txt("Tasks by Category", size = 16.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (byCat.isEmpty()) {
                Txt("No tasks in this period yet.", size = 14.sp, color = p.muted)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                        Ring(
                            byCat.map { (c, n) -> n.toFloat() to c.color },
                            total = inRange.size.toFloat(),
                            track = p.field,
                            thickness = 18.dp,
                            gradient = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        Txt(inRange.size.toString(), size = 22.sp, weight = FontWeight.ExtraBold, align = TextAlign.Center)
                    }
                    Spacer(Modifier.size(20.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        byCat.forEach { (c, n) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(c.color))
                                Spacer(Modifier.size(8.dp))
                                Txt(c.label, size = 14.sp, modifier = Modifier.weight(1f))
                                Txt(n.toString(), size = 14.sp, weight = FontWeight.SemiBold, color = p.muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountRow(label: String, n: Int, color: Color) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(8.dp))
        Txt(label, size = 14.sp, color = p.muted, modifier = Modifier.weight(1f))
        Txt(n.toString(), size = 16.sp, weight = FontWeight.Bold)
    }
}

/** Donut ring. Each slice is (value, color); a null color uses [gradient]. */
@Composable
private fun Ring(
    slices: List<Pair<Float, Color?>>,
    total: Float,
    track: Color,
    thickness: Dp,
    gradient: Brush?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val stroke = thickness.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke)
        )
        if (total <= 0f) return@Canvas
        var start = -90f
        slices.forEach { (value, color) ->
            if (value <= 0f) return@forEach
            val sweep = value / total * 360f
            val style = Stroke(stroke, cap = if (slices.size == 1) StrokeCap.Round else StrokeCap.Butt)
            if (color != null) {
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = (sweep - if (slices.size > 1) 2f else 0f).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style
                )
            } else if (gradient != null) {
                drawArc(
                    brush = gradient,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style
                )
            }
            start += sweep
        }
    }
}
