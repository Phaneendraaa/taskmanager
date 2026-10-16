package com.example.taskmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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

private class Span(val from: Long, val to: Long)

private val PERIOD_NAMES = listOf("This Week", "This Month", "This Year")

private fun spansFor(kind: Int, today: Long): Pair<Span, Span> {
    val t = LocalDate.ofEpochDay(today)
    return when (kind) {
        0 -> {
            val start = today - (t.dayOfWeek.value - 1)
            Span(start, start + 6) to Span(start - 7, start - 1)
        }
        1 -> {
            val first = t.withDayOfMonth(1)
            val prev = first.minusMonths(1)
            Span(first.toEpochDay(), first.plusMonths(1).toEpochDay() - 1) to
                Span(prev.toEpochDay(), first.toEpochDay() - 1)
        }
        else -> {
            val first = LocalDate.of(t.year, 1, 1)
            val prev = first.minusYears(1)
            Span(first.toEpochDay(), first.plusYears(1).toEpochDay() - 1) to
                Span(prev.toEpochDay(), first.toEpochDay() - 1)
        }
    }
}

/** Share of scheduled habit days completed inside [r], counting days up to today. */
private fun habitPercent(habits: List<Habit>, r: Span, today: Long): Int? {
    var due = 0
    var done = 0
    var d = r.from
    val end = minOf(r.to, today)
    while (d <= end) {
        for (h in habits) {
            if (d >= h.createdDay && h.isDue(d)) {
                due++
                if (d in h.doneDays) done++
            }
        }
        d++
    }
    return if (due == 0) null else done * 100 / due
}

private fun taskPercent(tasks: List<Task>, r: Span): Int? {
    val list = tasks.filter { val d = it.dueDay; d != null && d >= r.from && d <= r.to }
    return if (list.isEmpty()) null else list.count { it.done } * 100 / list.size
}

private fun focusMinutes(sessions: List<FocusSession>, r: Span): Int =
    sessions.filter { epochDayOf(it.endedAt) in r.from..r.to }.sumOf { it.minutes }

private fun minutesText(m: Int): String = when {
    m >= 60 && m % 60 == 0 -> "${m / 60}h"
    m >= 60 -> "${m / 60}h ${m % 60}m"
    else -> "${m}m"
}

private fun deltaText(cur: Int?, prev: Int?): String? {
    if (cur == null || prev == null) return null
    val d = cur - prev
    return if (d >= 0) "+$d%" else "$d%"
}

@Composable
fun StatsScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onPickDay: (Long) -> Unit
) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    var period by rememberSaveable { mutableIntStateOf(1) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    val (cur, prev) = spansFor(period, today)

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = padding.calculateBottomPadding() + 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("Stats", size = 32.sp, weight = FontWeight.Bold, serif = true, modifier = Modifier.weight(1f))
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { menu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Txt(PERIOD_NAMES[period], size = 14.sp, weight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    AppIcon(Ic.CHEVRON_DOWN, p.muted, size = 18.dp)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    PERIOD_NAMES.forEachIndexed { i, name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            period = i
                            menu = false
                        })
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SegmentedControl(listOf("Overview", "Habits", "Tasks", "Focus"), tab, { tab = it })
        Spacer(Modifier.height(16.dp))

        when (tab) {
            0 -> OverviewTab(vm, cur, prev, today, period)
            1 -> HabitsTab(vm, cur, today)
            2 -> TasksTab(vm, cur, today, onPickDay)
            else -> FocusTab(vm, cur, today, period)
        }
    }
}

// ---------------------------------------------------------------- Overview

@Composable
private fun OverviewTab(vm: AppViewModel, cur: Span, prev: Span, today: Long, period: Int) {
    val p = LocalPalette.current
    val habitNow = habitPercent(vm.habits, cur, today)
    val habitBefore = habitPercent(vm.habits, prev, today)
    val taskNow = taskPercent(vm.tasks, cur)
    val taskBefore = taskPercent(vm.tasks, prev)
    val focusNow = focusMinutes(vm.focusSessions, cur)
    val focusBefore = focusMinutes(vm.focusSessions, prev)
    val focusDelta = if (focusBefore > 0) {
        val d = (focusNow - focusBefore) * 100 / focusBefore
        if (d >= 0) "+$d%" else "$d%"
    } else {
        null
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RingTile("Habit consistency", habitNow, deltaText(habitNow, habitBefore), Modifier.weight(1f))
        RingTile("Task completion", taskNow, deltaText(taskNow, taskBefore), Modifier.weight(1f))
        Column(
            Modifier.weight(1f).cardStyle(p, 20.dp).padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Txt(minutesText(focusNow), size = 20.sp, weight = FontWeight.Bold, serif = true)
            }
            Txt("Focus time", size = 11.sp, color = p.muted, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            DeltaLabel(focusDelta)
        }
    }

    Spacer(Modifier.height(20.dp))
    Txt("Activity heatmap", size = 18.sp, weight = FontWeight.Bold, serif = true)
    Spacer(Modifier.height(10.dp))
    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp)) {
        ActivityGrid(vm, today)
    }

    Spacer(Modifier.height(20.dp))
    Txt("Habit categories", size = 18.sp, weight = FontWeight.Bold, serif = true)
    Spacer(Modifier.height(10.dp))
    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp)) {
        val counts = HabitCategory.values().map { c ->
            c to vm.habits.filter { it.category == c }.sumOf { h ->
                h.doneDays.count { it in cur.from..cur.to }
            }
        }.filter { it.second > 0 }
        val byHabits = if (counts.isEmpty()) {
            HabitCategory.values().map { c -> c to vm.habits.count { it.category == c } }.filter { it.second > 0 }
        } else {
            counts
        }
        if (byHabits.isEmpty()) {
            Txt("Add habits to see how they split.", size = 14.sp, color = p.muted)
        } else {
            val total = byHabits.sumOf { it.second }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Ring(
                        byHabits.map { (c, n) -> n.toFloat() to c.color },
                        total = total.toFloat(),
                        track = p.field,
                        thickness = 18.dp,
                        gradient = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt(vm.habits.size.toString(), size = 22.sp, weight = FontWeight.Bold, serif = true)
                        Txt("habits", size = 11.sp, color = p.muted)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    byHabits.forEach { (c, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(c.color))
                            Spacer(Modifier.width(8.dp))
                            Txt(c.label, size = 14.sp, modifier = Modifier.weight(1f))
                            Txt("${n * 100 / total}%", size = 14.sp, weight = FontWeight.SemiBold, color = p.muted)
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    Txt("Focus timeline", size = 18.sp, weight = FontWeight.Bold, serif = true)
    Spacer(Modifier.height(10.dp))
    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp)) {
        val (values, labels) = focusBuckets(vm.focusSessions, cur, period)
        if (values.all { it == 0f }) {
            Txt("Finish a focus session to see your timeline.", size = 14.sp, color = p.muted)
        } else {
            BarChart(values, labels)
        }
    }
}

@Composable
private fun RingTile(label: String, percent: Int?, delta: String?, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.cardStyle(p, 20.dp).padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            ProgressRing((percent ?: 0) / 100f, Modifier.fillMaxSize(), thickness = 6.dp)
            Txt(if (percent == null) "-" else "$percent%", size = 16.sp, weight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Txt(label, size = 11.sp, color = p.muted, maxLines = 1, align = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        DeltaLabel(delta)
    }
}

@Composable
private fun DeltaLabel(delta: String?) {
    val p = LocalPalette.current
    if (delta == null) {
        Txt("no data yet", size = 11.sp, color = p.muted)
    } else {
        Txt(
            delta,
            size = 12.sp,
            weight = FontWeight.SemiBold,
            color = if (delta.startsWith("-")) StatusRed else StatusGreen
        )
    }
}

/** Last 12 weeks, one column per week, rows Monday to Sunday. */
@Composable
private fun ActivityGrid(vm: AppViewModel, today: Long) {
    val p = LocalPalette.current
    val weeks = 12
    val gap = 4.dp
    val weekStart = today - (LocalDate.ofEpochDay(today).dayOfWeek.value - 1)
    val first = weekStart - (weeks - 1) * 7L

    fun score(d: Long): Float? {
        var due = 0
        var done = 0
        for (h in vm.habits) {
            if (d >= h.createdDay && h.isDue(d)) {
                due++
                if (d in h.doneDays) done++
            }
        }
        for (t in vm.tasks) {
            if (t.dueDay == d) {
                due++
                if (t.done) done++
            }
        }
        return if (due == 0) null else done.toFloat() / due
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val totalW = maxWidth
        val labelW = 18.dp
        val cell = (totalW - labelW - gap * (weeks - 1)) / weeks
        Row {
            Column(Modifier.width(labelW), verticalArrangement = Arrangement.spacedBy(gap)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { l ->
                    Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                        Txt(l, size = 9.sp, color = p.muted)
                    }
                }
            }
            val faint = p.faint
            val accent = p.accent
            val data = ArrayList<Float?>()
            for (w in 0 until weeks) {
                for (r in 0..6) {
                    val d = first + w * 7L + r
                    data.add(if (d > today) null else score(d))
                }
            }
            Canvas(Modifier.width(totalW - labelW).height(cell * 7 + gap * 6)) {
                val c = cell.toPx()
                val g = gap.toPx()
                for (w in 0 until weeks) {
                    for (r in 0..6) {
                        val v = data[w * 7 + r]
                        val color = when {
                            v == null -> faint.copy(alpha = 0.4f)
                            v <= 0f -> faint
                            v < 0.34f -> accent.copy(alpha = 0.35f)
                            v < 0.67f -> accent.copy(alpha = 0.6f)
                            v < 1f -> accent.copy(alpha = 0.8f)
                            else -> accent
                        }
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(w * (c + g), r * (c + g)),
                            size = Size(c, c),
                            cornerRadius = CornerRadius(c * 0.28f)
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Txt("Less", size = 11.sp, color = p.muted)
        Spacer(Modifier.width(6.dp))
        listOf(0.25f, 0.35f, 0.6f, 0.8f, 1f).forEach { a ->
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(11.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (a == 0.25f) p.faint else p.accent.copy(alpha = a))
            )
        }
        Spacer(Modifier.width(6.dp))
        Txt("More", size = 11.sp, color = p.muted)
    }
}

// ---------------------------------------------------------------- Habits tab

@Composable
private fun HabitsTab(vm: AppViewModel, cur: Span, today: Long) {
    val p = LocalPalette.current
    val overall = habitPercent(vm.habits, cur, today)

    Row(
        Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            ProgressRing((overall ?: 0) / 100f, Modifier.fillMaxSize(), thickness = 9.dp)
            Txt(if (overall == null) "-" else "$overall%", size = 20.sp, weight = FontWeight.Bold, serif = true)
        }
        Spacer(Modifier.width(18.dp))
        Column {
            Txt("Habit consistency", size = 16.sp, weight = FontWeight.Bold)
            val best = vm.habits.maxByOrNull { it.currentStreak(today) }
            Txt(
                if (best == null || best.currentStreak(today) == 0) "Complete a habit to start a streak."
                else "Best streak: ${best.name}, ${best.currentStreak(today)} days",
                size = 13.sp,
                color = p.muted
            )
        }
    }
    Spacer(Modifier.height(16.dp))

    if (vm.habits.isEmpty()) {
        Txt("No habits yet.", Modifier.fillMaxWidth().padding(20.dp), size = 14.sp, color = p.muted, align = TextAlign.Center)
    }
    val rows = vm.habits.map { h -> h to (habitPercent(listOf(h), cur, today) ?: 0) }.sortedByDescending { it.second }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (h, pct) ->
            Column(Modifier.fillMaxWidth().cardStyle(p, 20.dp).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(h.category.color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) { AppIcon(h.iconKind(), h.category.color, size = 20.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Txt(h.name, size = 15.sp, weight = FontWeight.SemiBold, maxLines = 1)
                        Txt("${h.currentStreak(today)} day streak  •  best ${h.bestStreak()}", size = 12.sp, color = p.muted)
                    }
                    Txt("$pct%", size = 16.sp, weight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                GradientProgress(
                    pct / 100f,
                    height = 8.dp,
                    brush = Brush.horizontalGradient(listOf(p.accent, p.accent2))
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Tasks tab

@Composable
private fun TasksTab(vm: AppViewModel, cur: Span, today: Long, onPickDay: (Long) -> Unit) {
    val p = LocalPalette.current
    val inRange = vm.tasks.filter { val d = it.dueDay; d != null && d >= cur.from && d <= cur.to }
    val completed = inRange.count { it.done }
    val overdue = inRange.count { !it.done && it.isOverdue(vm.now) }
    val pending = inRange.size - completed - overdue
    val percent = if (inRange.isEmpty()) 0 else completed * 100 / inRange.size
    val doneToday = vm.tasks.count { val c = it.completedAt; it.done && c != null && epochDayOf(c) == today } +
        vm.habits.count { today in it.doneDays }
    val goal = vm.dailyGoal

    Row(
        Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            ProgressRing(percent / 100f, Modifier.fillMaxSize(), thickness = 12.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt("$percent%", size = 24.sp, weight = FontWeight.Bold, serif = true)
                Txt("Completed", size = 11.sp, color = p.muted)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CountRow("Completed", completed, StatusGreen)
            CountRow("Pending", pending, StatusBlue)
            CountRow("Overdue", overdue, StatusRed)
        }
    }
    Spacer(Modifier.height(14.dp))

    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(Ic.TARGET, p.accent, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Txt("Daily goal", size = 16.sp, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Txt("$doneToday of $goal done today", size = 14.sp, color = p.muted)
        }
        Spacer(Modifier.height(12.dp))
        GradientProgress(
            doneToday.toFloat() / goal.toFloat(),
            height = 10.dp,
            brush = Brush.horizontalGradient(listOf(p.accent, p.accent2))
        )
    }
    Spacer(Modifier.height(14.dp))

    val byCat = Category.values().map { c -> c to inRange.count { it.category == c } }.filter { it.second > 0 }
    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(18.dp)) {
        Txt("Tasks by category", size = 16.sp, weight = FontWeight.Bold)
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
                    Txt(inRange.size.toString(), size = 22.sp, weight = FontWeight.Bold, serif = true)
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    byCat.forEach { (c, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(c.color))
                            Spacer(Modifier.width(8.dp))
                            Txt(c.label, size = 14.sp, modifier = Modifier.weight(1f))
                            Txt(n.toString(), size = 14.sp, weight = FontWeight.SemiBold, color = p.muted)
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Txt(
        "Open a day on the calendar to see its tasks.",
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPickDay(today) }
            .padding(10.dp),
        size = 13.sp,
        color = p.accentText,
        align = TextAlign.Center
    )
}

@Composable
private fun CountRow(label: String, n: Int, color: Color) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Txt(label, size = 14.sp, color = p.muted, modifier = Modifier.weight(1f))
        Txt(n.toString(), size = 16.sp, weight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- Focus tab

private fun focusBuckets(sessions: List<FocusSession>, r: Span, period: Int): Pair<List<Float>, List<String>> {
    val values = ArrayList<Float>()
    val labels = ArrayList<String>()
    if (period == 2) {
        val year = LocalDate.ofEpochDay(r.from).year
        val letters = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        for (m in 1..12) {
            val start = LocalDate.of(year, m, 1)
            val span = Span(start.toEpochDay(), start.plusMonths(1).toEpochDay() - 1)
            values.add(focusMinutes(sessions, span).toFloat())
            labels.add(letters[m - 1])
        }
    } else {
        var d = r.from
        var i = 0
        while (d <= r.to) {
            values.add(focusMinutes(sessions, Span(d, d)).toFloat())
            labels.add(
                if (period == 0) dayInitial(d)
                else if (i == 0 || (i + 1) % 5 == 0) dayOfMonth(d).toString() else ""
            )
            d++
            i++
        }
    }
    return values to labels
}

@Composable
private fun FocusTab(vm: AppViewModel, cur: Span, today: Long, period: Int) {
    val p = LocalPalette.current
    val inRange = vm.focusSessions.filter { epochDayOf(it.endedAt) in cur.from..cur.to }
    val total = inRange.sumOf { it.minutes }
    val activeDays = inRange.map { epochDayOf(it.endedAt) }.distinct().size
    val avg = if (activeDays == 0) 0 else total / activeDays

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SimpleTile(minutesText(total), "Focus time", Modifier.weight(1f))
        SimpleTile(inRange.size.toString(), "Sessions", Modifier.weight(1f))
        SimpleTile(minutesText(avg), "Daily average", Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp)) {
        Txt("Focus timeline", size = 16.sp, weight = FontWeight.Bold, serif = true)
        Spacer(Modifier.height(12.dp))
        val (values, labels) = focusBuckets(vm.focusSessions, cur, period)
        if (values.all { it == 0f }) {
            Txt("No focus sessions in this period yet.", size = 14.sp, color = p.muted)
        } else {
            BarChart(values, labels)
        }
    }
    Spacer(Modifier.height(16.dp))
    Txt("Recent sessions", size = 16.sp, weight = FontWeight.Bold, serif = true)
    Spacer(Modifier.height(8.dp))
    val recent = vm.focusSessions.sortedByDescending { it.endedAt }.take(6)
    if (recent.isEmpty()) {
        Txt("Start the timer from the menu on Home.", size = 14.sp, color = p.muted)
    } else {
        Column(Modifier.fillMaxWidth().cardStyle(p, 20.dp)) {
            recent.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(Ic.TIMER, p.accent, size = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Txt(s.label, size = 15.sp, weight = FontWeight.Medium, maxLines = 1)
                        Txt(stampOf(s.endedAt), size = 12.sp, color = p.muted)
                    }
                    Txt("${s.minutes} min", size = 14.sp, weight = FontWeight.SemiBold, color = p.muted)
                }
            }
        }
    }
}

@Composable
private fun SimpleTile(value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.cardStyle(p, 20.dp).padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt(value, size = 22.sp, weight = FontWeight.Bold, serif = true, maxLines = 1)
        Txt(label, size = 11.sp, color = p.muted, maxLines = 1)
    }
}

@Composable
private fun BarChart(values: List<Float>, labels: List<String>) {
    val p = LocalPalette.current
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Row(
        Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { v ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight((v / max).coerceIn(0.03f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (v > 0f) p.accent else p.faint)
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEach { l ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Txt(l, size = 10.sp, color = p.muted, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Txt("Peak: ${minutesText(max.toInt())}", size = 11.sp, color = p.muted)
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
