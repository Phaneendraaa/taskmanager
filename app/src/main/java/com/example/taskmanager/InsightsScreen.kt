package com.example.taskmanager

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** Due and done habit-days between two epoch days, inclusive. */
private fun habitDays(habits: List<Habit>, from: Long, to: Long, filter: (Long) -> Boolean = { true }, pick: (Habit) -> Boolean = { true }): Pair<Int, Int> {
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

private fun minutesText(m: Int): String = if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"

private fun dow(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value - 1

@Composable
fun InsightsScreen(vm: AppViewModel, padding: PaddingValues, onPickDay: (Long) -> Unit = {}) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    val habits = vm.habits.toList()

    // Last 7 days against the 7 before.
    val thisWeek = habitDays(habits, today - 6, today)
    val lastWeek = habitDays(habits, today - 13, today - 7)
    val weekPct = pct(thisWeek)
    val lastPct = pct(lastWeek)

    val focusDay = { d: Long -> vm.focusSessions.filter { epochDayOf(it.endedAt) == d }.sumOf { it.minutes } }
    val focusThis = (today - 6..today).sumOf { focusDay(it) }
    val focusLast = (today - 13..today - 7).sumOf { focusDay(it) }
    val ratings = vm.reviews.filter { it.day in (today - 6)..today }
    val tasksDone = vm.tasks.count { t -> t.done && t.completedAt != null && epochDayOf(t.completedAt!!) in (today - 6)..today }

    // 30 day consistency against the 30 before.
    val c30 = pct(habitDays(habits, today - 29, today))
    val c30prev = pct(habitDays(habits, today - 59, today - 30))

    // Patterns over 60 days.
    val weekdayPct = (0..6).map { w -> pct(habitDays(habits, today - 59, today, filter = { dow(it) == w })) }
    val periodPct = Period.values().filter { it != Period.ANYTIME }.map { per ->
        per to pct(habitDays(habits, today - 29, today, pick = { it.period() == per }))
    }
    val bestWeekday = weekdayPct.withIndex().filter { it.value != null }.maxByOrNull { it.value!! }
    val worstWeekday = weekdayPct.withIndex().filter { it.value != null }.minByOrNull { it.value!! }
    val bestPeriod = periodPct.filter { it.second != null }.maxByOrNull { it.second!! }
    val worstPeriod = periodPct.filter { it.second != null }.minByOrNull { it.second!! }
    val names = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")

    // Heat map of days where at least half the due habits were done.
    val strongDays = HashSet<Long>()
    for (d in today - 111..today) {
        val (due, done) = habitDays(habits, d, d)
        if (due > 0 && done * 2 >= due) strongDays.add(d)
    }

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
        Spacer(Modifier.height(4.dp))
        Txt("What your last weeks say, in plain words.", size = 14.sp, color = p.muted)
        Spacer(Modifier.height(18.dp))

        if (habits.isEmpty() && vm.focusSessions.isEmpty()) {
            Txt(
                "Insights appear after a few days of using the app. Add a habit and check it off to begin.",
                Modifier.fillMaxWidth().cardStyle(p).padding(18.dp), size = 14.sp, color = p.muted, lineHeight = 20.sp
            )
            return@Column
        }

        // Weekly review
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(18.dp)) {
            Txt("WEEKLY REVIEW", size = 11.sp, weight = FontWeight.SemiBold, color = p.muted)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(if (weekPct == null) "–" else "$weekPct%", size = 40.sp, serif = true, weight = FontWeight.Medium)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.padding(bottom = 7.dp)) {
                    Txt("of habits done, last 7 days", size = 13.sp, color = p.muted, maxLines = 1)
                    val diff = if (weekPct != null && lastPct != null) weekPct - lastPct else null
                    if (diff != null) {
                        Txt(
                            when {
                                diff > 0 -> "Up $diff points on the week before"
                                diff < 0 -> "Down ${-diff} points on the week before"
                                else -> "Same as the week before"
                            },
                            size = 13.sp, color = if (diff >= 0) p.accentText else p.muted, maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Line("Deep work", minutesText(focusThis) + deltaText(focusThis - focusLast))
            Line("Tasks completed", "$tasksDone")
            Line(
                "Average day rating",
                if (ratings.isEmpty()) "No check-ins yet" else String.format("%.1f of 5", ratings.map { it.rating }.average())
            )
            val tip = when {
                worstPeriod != null && bestPeriod != null && worstPeriod.first != bestPeriod.first &&
                    bestPeriod.second!! - worstPeriod.second!! >= 15 ->
                    "${worstPeriod.first.label} is your weakest slot (${worstPeriod.second}%). Try attaching those habits to something you already do."
                worstWeekday != null && bestWeekday != null && bestWeekday.value!! - worstWeekday.value!! >= 15 ->
                    "${names[worstWeekday.index]} are your weak spot. Plan an easier version for that day."
                weekPct != null && weekPct >= 80 -> "Strong week. Keep the bar where it is rather than raising it."
                else -> "Pick one habit to protect this week and let the rest be imperfect."
            }
            Spacer(Modifier.height(10.dp))
            Txt(tip, size = 13.sp, lineHeight = 19.sp, weight = FontWeight.Medium)
        }

        // 30 day consistency
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Consistency, 30 days", size = 15.sp, weight = FontWeight.SemiBold, serif = true, modifier = Modifier.weight(1f))
                Txt(if (c30 == null) "–" else "$c30%", size = 18.sp, serif = true, weight = FontWeight.Medium)
            }
            if (c30 != null && c30prev != null) {
                Spacer(Modifier.height(2.dp))
                val d = c30 - c30prev
                Txt(
                    if (d >= 0) "$d points above the previous 30 days" else "${-d} points below the previous 30 days",
                    size = 12.sp, color = p.muted
                )
            }
            Spacer(Modifier.height(14.dp))
            Heatmap(strongDays, today, 16)
            Spacer(Modifier.height(8.dp))
            Txt("Filled squares are days when at least half your habits were done.", size = 12.sp, color = p.muted, lineHeight = 17.sp)
        }

        // Patterns
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(18.dp)) {
            Txt("Your pattern", size = 15.sp, weight = FontWeight.SemiBold, serif = true)
            Spacer(Modifier.height(4.dp))
            Txt(
                if (bestWeekday == null) "Needs a few more days of data."
                else "Best on ${names[bestWeekday.index]} (${bestWeekday.value}%)" +
                    (if (bestPeriod != null) ", strongest in the ${bestPeriod.first.label.lowercase()}." else "."),
                size = 13.sp, color = p.muted, lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                weekdayPct.forEachIndexed { i, v ->
                    val best = bestWeekday?.index == i
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            Modifier.fillMaxWidth().height((70 * ((v ?: 0) / 100f)).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(4.dp)).background(if (best) p.accent else p.faint)
                        )
                        Spacer(Modifier.height(6.dp))
                        Txt(listOf("M", "T", "W", "T", "F", "S", "S")[i], size = 12.sp, color = if (best) p.text else p.muted)
                    }
                }
            }
        }

        // Per habit
        if (habits.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().cardStyle(p).padding(18.dp)) {
                Txt("By habit, 30 days", size = 15.sp, weight = FontWeight.SemiBold, serif = true)
                Spacer(Modifier.height(12.dp))
                habits.sortedByDescending { it.consistency(today) }.forEach { h ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(h.name, size = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        if (h.isAutomatic(today)) {
                            Txt("Automatic", size = 11.sp, color = p.accentText, weight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                        Txt("${h.consistency(today)}%", size = 13.sp, color = p.muted, maxLines = 1)
                    }
                    Spacer(Modifier.height(6.dp))
                    GradientProgress(h.consistency(today) / 100f, Modifier.fillMaxWidth(), 5.dp)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        // Focus, last 7 days
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Deep work, 7 days", size = 15.sp, weight = FontWeight.SemiBold, serif = true, modifier = Modifier.weight(1f))
                Txt(minutesText(focusThis), size = 13.sp, color = p.muted)
            }
            Spacer(Modifier.height(14.dp))
            val maxMin = maxOf(vm.focusTarget, (today - 6..today).maxOf { focusDay(it) }, 1)
            Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                for (d in today - 6..today) {
                    val m = focusDay(d)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            Modifier.fillMaxWidth().height((70 * (m / maxMin.toFloat())).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(4.dp)).background(if (m >= vm.focusTarget) p.accent else p.faint)
                        )
                        Spacer(Modifier.height(6.dp))
                        Txt(shortDow(d).take(1), size = 12.sp, color = if (d == today) p.text else p.muted)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Txt("Dark bars reached your ${vm.focusTarget} minute target.", size = 12.sp, color = p.muted)
        }
    }
}

private fun deltaText(diff: Int): String = when {
    diff > 0 -> "  (+${minutesText(diff)})"
    diff < 0 -> "  (-${minutesText(-diff)})"
    else -> ""
}

@Composable
private fun Line(label: String, value: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, size = 14.sp, color = p.muted, modifier = Modifier.weight(1f))
        Txt(value, size = 14.sp, weight = FontWeight.Medium, maxLines = 1)
    }
}
