package com.example.taskmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

// ---------- numbers ----------

/** Due and done habit-days between two epoch days, inclusive. */
private fun habitDays(
    habits: List<Habit>, from: Long, to: Long,
    filter: (Long) -> Boolean = { true }, pick: (Habit) -> Boolean = { true }
): Pair<Int, Int> {
    var due = 0
    var done = 0
    for (d in from..to) {
        if (!filter(d)) continue
        for (h in habits) {
            if (!pick(h) || d < h.createdDay || !h.isDue(d)) continue
            due++
            if (d in h.doneDays) done++
        }
    }
    return due to done
}

private fun pct(p: Pair<Int, Int>): Int? = if (p.first == 0) null else p.second * 100 / p.first

/** Share of due habits done on one day, or null when nothing was due. */
private fun dayFraction(habits: List<Habit>, d: Long): Float? {
    val (due, done) = habitDays(habits, d, d)
    return if (due == 0) null else done.toFloat() / due
}

private fun minutesText(m: Int): String = if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"

private fun dow(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value - 1

private val DAY_NAMES = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")
private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

// ---------- screen ----------

@Composable
fun InsightsScreen(vm: AppViewModel, padding: PaddingValues, onPickDay: (Long) -> Unit = {}) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    val habits = vm.habits.toList()

    val weekPct = pct(habitDays(habits, today - 6, today))
    val lastPct = pct(habitDays(habits, today - 13, today - 7))
    val focusDay = { d: Long -> vm.focusSessions.filter { epochDayOf(it.endedAt) == d }.sumOf { it.minutes } }
    val focusThis = (today - 6..today).sumOf { focusDay(it) }
    val focusLast = (today - 13..today - 7).sumOf { focusDay(it) }
    val tasksDay = { d: Long -> vm.tasks.count { t -> t.done && t.completedAt != null && epochDayOf(t.completedAt!!) == d } }
    val tasksThis = (today - 6..today).sumOf { tasksDay(it) }
    val tasksLast = (today - 13..today - 7).sumOf { tasksDay(it) }
    val ratings = vm.reviews.filter { it.day in (today - 6)..today }

    val daily30 = (today - 29..today).map { dayFraction(habits, it) }
    val c30 = pct(habitDays(habits, today - 29, today))
    val c30prev = pct(habitDays(habits, today - 59, today - 30))

    val weekdayPct = (0..6).map { w -> pct(habitDays(habits, today - 59, today, filter = { dow(it) == w })) }
    val periods = listOf(Period.MORNING, Period.AFTERNOON, Period.EVENING, Period.ANYTIME)
    val periodPct = periods.map { per -> per to pct(habitDays(habits, today - 29, today, pick = { it.period() == per })) }
        .filter { it.second != null }
    val bestWeekday = weekdayPct.withIndex().filter { it.value != null }.maxByOrNull { it.value!! }
    val worstWeekday = weekdayPct.withIndex().filter { it.value != null }.minByOrNull { it.value!! }
    val bestPeriod = periodPct.maxByOrNull { it.second!! }
    val worstPeriod = periodPct.minByOrNull { it.second!! }

    val levels = HashMap<Long, Float?>()
    for (d in today - 7 * 16..today) levels[d] = dayFraction(habits, d)

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = padding.calculateBottomPadding() + 24.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Txt("Insights", size = 28.sp, weight = FontWeight.SemiBold, serif = true)
        Spacer(Modifier.height(16.dp))

        if (habits.isEmpty() && vm.focusSessions.isEmpty() && vm.tasks.none { it.done }) {
            Txt(
                "Your charts appear after a few days. Add a habit or finish a task to begin.",
                Modifier.fillMaxWidth().cardStyle(p).padding(18.dp), size = 14.sp, color = p.muted, lineHeight = 20.sp
            )
            return@Column
        }

        // ---- This week: ring and three numbers with sparklines ----
        Card {
            Label("THIS WEEK")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    ProgressRing((weekPct ?: 0) / 100f, Modifier.fillMaxSize(), thickness = 10.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt(if (weekPct == null) "–" else "$weekPct%", size = 26.sp, serif = true, weight = FontWeight.SemiBold)
                        Txt("habits", size = 11.sp, color = p.muted)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Kpi("Focus", minutesText(focusThis), focusThis - focusLast, (today - 6..today).map { focusDay(it).toFloat() })
                    Kpi("Tasks done", tasksThis.toString(), tasksThis - tasksLast, (today - 6..today).map { tasksDay(it).toFloat() })
                    Kpi(
                        "Day rating",
                        if (ratings.isEmpty()) "–" else String.format("%.1f", ratings.map { it.rating }.average()),
                        null,
                        (today - 6..today).map { d -> (vm.reviewFor(d)?.rating ?: 0).toFloat() }
                    )
                }
            }
            val diff = if (weekPct != null && lastPct != null) weekPct - lastPct else null
            if (diff != null) {
                Spacer(Modifier.height(12.dp))
                Hairline()
                Spacer(Modifier.height(10.dp))
                Txt(
                    when {
                        diff > 0 -> "Up $diff points on the week before."
                        diff < 0 -> "Down ${-diff} points on the week before. One easier week is fine."
                        else -> "Level with the week before."
                    },
                    size = 13.sp, color = p.muted
                )
            }
        }

        // ---- Daily completion, 30 days ----
        Spacer(Modifier.height(12.dp))
        Card {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Label("DAILY COMPLETION")
                    Spacer(Modifier.height(4.dp))
                    Txt("Last 30 days", size = 13.sp, color = p.muted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Txt(if (c30 == null) "–" else "$c30%", size = 24.sp, serif = true, weight = FontWeight.SemiBold)
                    if (c30 != null && c30prev != null) {
                        val d = c30 - c30prev
                        Txt((if (d >= 0) "+$d" else "$d") + " vs previous 30", size = 11.sp, color = p.muted)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            BarChart(
                values = daily30.map { it?.times(100f) },
                max = 100f,
                height = 120.dp,
                gridLabels = listOf("0", "50", "100%")
            )
            Spacer(Modifier.height(6.dp))
            AxisLabels(listOf("${shortMonth(today - 29)} ${dayOfMonth(today - 29)}", "${shortMonth(today - 15)} ${dayOfMonth(today - 15)}", "Today"))
        }

        // ---- Heatmap (kept) with intensity ----
        Spacer(Modifier.height(12.dp))
        Card {
            Label("CONSISTENCY, 16 WEEKS")
            Spacer(Modifier.height(12.dp))
            LevelHeatmap(levels, today, 16)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Darker means more of the day's habits done.", size = 11.sp, color = p.muted, modifier = Modifier.weight(1f))
                Txt("Less", size = 11.sp, color = p.muted)
                Spacer(Modifier.width(4.dp))
                listOf(0f, 0.3f, 0.55f, 0.8f, 1f).forEach { f ->
                    Box(Modifier.padding(horizontal = 1.5.dp).size(10.dp).clip(RoundedCornerShape(2.dp)).background(levelColor(p, f)))
                }
                Spacer(Modifier.width(4.dp))
                Txt("More", size = 11.sp, color = p.muted)
            }
        }

        // ---- When you show up ----
        Spacer(Modifier.height(12.dp))
        Card {
            Label("WHEN YOU SHOW UP")
            Spacer(Modifier.height(4.dp))
            Txt(
                if (bestWeekday == null) "Needs a few more days of data."
                else "Strongest on ${DAY_NAMES[bestWeekday.index]}" +
                    (if (bestPeriod != null) " and in the ${bestPeriod.first.label.lowercase()}." else "."),
                size = 13.sp, color = p.muted
            )
            Spacer(Modifier.height(14.dp))
            BarChart(
                values = weekdayPct.map { it?.toFloat() },
                max = 100f,
                height = 96.dp,
                highlight = bestWeekday?.index,
                gap = 10.dp
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                DAY_LETTERS.forEachIndexed { i, l ->
                    Txt(
                        l, Modifier.weight(1f), size = 11.sp, align = TextAlign.Center,
                        color = if (bestWeekday?.index == i) p.text else p.muted,
                        weight = if (bestWeekday?.index == i) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            if (periodPct.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Hairline()
                Spacer(Modifier.height(14.dp))
                periodPct.forEach { (per, v) ->
                    HBar(per.label, (v ?: 0) / 100f, "${v}%", strong = per == bestPeriod?.first)
                    Spacer(Modifier.height(10.dp))
                }
            }
            val tip = when {
                worstPeriod != null && bestPeriod != null && worstPeriod.first != bestPeriod.first &&
                    bestPeriod.second!! - worstPeriod.second!! >= 15 ->
                    "${worstPeriod.first.label} habits slip most. Anchor them to something you already do then."
                worstWeekday != null && bestWeekday != null && bestWeekday.value!! - worstWeekday.value!! >= 15 ->
                    "${DAY_NAMES[worstWeekday.index]} are your weak spot. Plan the minimum version for that day."
                else -> null
            }
            if (tip != null) {
                Spacer(Modifier.height(4.dp))
                Txt(tip, size = 13.sp, weight = FontWeight.Medium, lineHeight = 19.sp)
            }
        }

        // ---- Focus, 14 days against target ----
        Spacer(Modifier.height(12.dp))
        Card {
            val days14 = (today - 13..today).map { focusDay(it).toFloat() }
            val hit = days14.count { it >= vm.focusTarget }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Label("DEEP WORK")
                    Spacer(Modifier.height(4.dp))
                    Txt("Last 14 days  •  target ${vm.focusTarget}m", size = 13.sp, color = p.muted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Txt("$hit/14", size = 24.sp, serif = true, weight = FontWeight.SemiBold)
                    Txt("days on target", size = 11.sp, color = p.muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            BarChart(
                values = days14,
                max = maxOf(vm.focusTarget.toFloat() * 1.25f, days14.maxOrNull() ?: 0f, 1f),
                height = 110.dp,
                target = vm.focusTarget.toFloat(),
                highlightAtOrAbove = vm.focusTarget.toFloat()
            )
            Spacer(Modifier.height(6.dp))
            AxisLabels(listOf("${shortMonth(today - 13)} ${dayOfMonth(today - 13)}", "Today"))
        }

        // ---- Each habit: 14 day strip ----
        if (habits.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card {
                Label("EACH HABIT, LAST 14 DAYS")
                Spacer(Modifier.height(14.dp))
                habits.sortedByDescending { it.consistency(today) }.forEach { h ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(h.name, size = 14.sp, weight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
                        if (h.isAutomatic(today)) {
                            Txt("Automatic", size = 11.sp, color = p.accentText, weight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                        Txt("${h.consistency(today)}%", size = 13.sp, weight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Spacer(Modifier.height(6.dp))
                    DayStrip(h, today, 14)
                    Spacer(Modifier.height(14.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(p.accent, "Done")
                    Spacer(Modifier.width(14.dp))
                    LegendDot(p.faint, "Missed")
                    Spacer(Modifier.width(14.dp))
                    LegendDot(p.field, "Not due")
                }
            }
        }
    }
}

// ---------- building blocks ----------

@Composable
private fun Card(content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) { content() }
}

@Composable
private fun Label(text: String) {
    Txt(text, size = 11.sp, weight = FontWeight.SemiBold, color = LocalPalette.current.muted)
}

@Composable
private fun Kpi(label: String, value: String, delta: Int?, spark: List<Float>) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(label, size = 11.sp, color = p.muted, maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(value, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 1)
                if (delta != null && delta != 0) {
                    Spacer(Modifier.width(6.dp))
                    Txt(if (delta > 0) "▲" else "▼", size = 10.sp, color = p.muted, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
        Sparkline(spark, Modifier.width(56.dp).height(22.dp))
    }
}

/** Seven tiny bars, oldest to today. */
@Composable
private fun Sparkline(values: List<Float>, modifier: Modifier) {
    val p = LocalPalette.current
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Canvas(modifier) {
        val n = values.size
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        values.forEachIndexed { i, v ->
            val h = (size.height * (v / max)).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                color = if (i == n - 1) p.accent else p.faint,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(1.5.dp.toPx())
            )
        }
    }
}

/**
 * Vertical bars from a zero baseline. Null values draw a short stub (no data that day).
 * The last bar, a [highlight] index, or bars at or above [highlightAtOrAbove] use the full accent.
 */
@Composable
private fun BarChart(
    values: List<Float?>,
    max: Float,
    height: Dp,
    highlight: Int? = null,
    highlightAtOrAbove: Float? = null,
    target: Float? = null,
    gap: Dp = 3.dp,
    gridLabels: List<String>? = null
) {
    val p = LocalPalette.current
    val soft = p.accent.copy(alpha = 0.45f)
    Box(Modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxSize()) {
            val n = values.size
            val g = gap.toPx()
            val w = (size.width - g * (n - 1)) / n
            val hairline = 1.dp.toPx()
            for (k in 0..2) {
                val y = size.height - size.height * k / 2f
                drawLine(p.cardBorder, Offset(0f, y), Offset(size.width, y), hairline)
            }
            val r = minOf(4.dp.toPx(), w / 2f)
            values.forEachIndexed { i, v ->
                val x = i * (w + g)
                if (v == null) {
                    drawRoundRect(p.field, Offset(x, size.height - 3.dp.toPx()), Size(w, 3.dp.toPx()), CornerRadius(1.dp.toPx()))
                    return@forEachIndexed
                }
                val h = (size.height * (v / max).coerceIn(0f, 1f)).coerceAtLeast(2.dp.toPx())
                val strong = (highlight == null && highlightAtOrAbove == null && i == n - 1) ||
                    highlight == i || (highlightAtOrAbove != null && v >= highlightAtOrAbove)
                val color = if (strong) p.accent else soft
                // Rounded top, square bottom on the baseline.
                drawRoundRect(color, Offset(x, size.height - h), Size(w, h), CornerRadius(r))
                if (h > r) drawRect(color, Offset(x, size.height - r), Size(w, r))
            }
            if (target != null) {
                val y = size.height - size.height * (target / max).coerceIn(0f, 1f)
                drawLine(
                    p.text.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                )
            }
        }
        if (gridLabels != null) {
            Txt(gridLabels.last(), Modifier.align(Alignment.TopEnd).background(p.card).padding(start = 4.dp), size = 10.sp, color = p.muted)
        }
    }
}

@Composable
private fun AxisLabels(labels: List<String>) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { Txt(it, size = 11.sp, color = p.muted, maxLines = 1) }
    }
}

@Composable
private fun HBar(label: String, fraction: Float, value: String, strong: Boolean) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Txt(label, Modifier.width(84.dp), size = 13.sp, color = if (strong) p.text else p.muted, maxLines = 1)
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(p.field)) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (strong) p.accent else p.accent.copy(alpha = 0.45f))
            )
        }
        Txt(value, Modifier.width(48.dp), size = 13.sp, weight = FontWeight.SemiBold, align = TextAlign.End, maxLines = 1)
    }
}

private fun levelColor(p: Palette, f: Float?): Color = when {
    f == null -> p.field
    f <= 0f -> p.faint
    f < 0.4f -> p.accent.copy(alpha = 0.3f)
    f < 0.7f -> p.accent.copy(alpha = 0.55f)
    f < 1f -> p.accent.copy(alpha = 0.8f)
    else -> p.accent
}

/** GitHub-style grid: weeks as columns, Monday at the top, colored by share of habits done. */
@Composable
private fun LevelHeatmap(levels: Map<Long, Float?>, today: Long, weeks: Int) {
    val p = LocalPalette.current
    val lastMonday = today - dow(today)
    val firstMonday = lastMonday - 7L * (weeks - 1)
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(14.dp).height(1.dp)) {}
        Canvas(Modifier.weight(1f).height(((weeks * 0) + 7 * 14 + 6 * 3).dp)) {
            val gap = 3.dp.toPx()
            val cell = minOf((size.width - gap * (weeks - 1)) / weeks, (size.height - gap * 6) / 7)
            for (w in 0 until weeks) {
                for (r in 0..6) {
                    val d = firstMonday + w * 7L + r
                    if (d > today) continue
                    drawRoundRect(
                        levelColor(p, levels[d]),
                        Offset(w * (cell + gap), r * (cell + gap)),
                        Size(cell, cell),
                        CornerRadius(2.5.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun DayStrip(h: Habit, today: Long, days: Int) {
    val p = LocalPalette.current
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (days - 1)) / days
        for (i in 0 until days) {
            val d = today - (days - 1 - i)
            val c = when {
                d < h.createdDay || !h.isDue(d) -> p.field
                d in h.doneDays -> p.accent
                else -> p.faint
            }
            drawRoundRect(c, Offset(i * (w + gap), 0f), Size(w, size.height), CornerRadius(2.5.dp.toPx()))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Txt(label, size = 11.sp, color = p.muted)
    }
}
