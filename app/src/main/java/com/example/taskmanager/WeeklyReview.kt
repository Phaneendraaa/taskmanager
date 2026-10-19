package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** What happened to one habit during the week: due days and done days. */
private class HabitWeek(val habit: Habit, val due: Int, val done: Int) {
    val ratio: Float get() = if (due == 0) 0f else done.toFloat() / due
}

private fun habitWeeks(habits: List<Habit>, from: Long, to: Long): List<HabitWeek> =
    habits.mapNotNull { h ->
        var due = 0
        var done = 0
        for (d in from..to) {
            if (d < h.createdDay || !h.isDue(d)) continue
            due++
            if (d in h.doneDays) done++
        }
        if (due == 0) null else HabitWeek(h, due, done)
    }

private fun daysText(done: Int, due: Int): String = "$done of $due " + if (due == 1) "day" else "days"

private fun percentOf(list: List<HabitWeek>): Int? {
    val due = list.sumOf { it.due }
    return if (due == 0) null else list.sumOf { it.done } * 100 / due
}

private fun rangeText(from: Long, to: Long): String =
    if (shortMonth(from) == shortMonth(to)) "${dayOfMonth(from)} to ${dayOfMonth(to)} ${shortMonth(to)}"
    else "${dayOfMonth(from)} ${shortMonth(from)} to ${dayOfMonth(to)} ${shortMonth(to)}"

private fun minutesText(m: Int): String = if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"

/**
 * The weekly review: how the week went in numbers, what went well, what slipped,
 * and one question that carries into next week.
 */
@Composable
fun WeeklyReviewScreen(vm: AppViewModel, week: Long, onBack: () -> Unit) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    val end = week + 6
    val last = minOf(end, today)

    val thisWeek = habitWeeks(vm.habits.toList(), week, last)
    val prevWeek = habitWeeks(vm.habits.toList(), week - 7, week - 1)
    val pct = percentOf(thisWeek)
    val prevPct = percentOf(prevWeek)

    val tasksDone = vm.tasks.count { t -> t.completedAt?.let { epochDayOf(it) in week..end } == true }
    val focusMin = vm.focusSessions.filter { epochDayOf(it.endedAt) in week..end }.sumOf { it.minutes }
    val ratings = vm.reviews.filter { it.day in week..end }
    val avgRating = if (ratings.isEmpty()) null else ratings.sumOf { it.rating } / ratings.size.toDouble()

    val well = ArrayList<String>()
    thisWeek.filter { it.done > 0 }.sortedWith(compareByDescending<HabitWeek> { it.ratio }.thenByDescending { it.done })
        .take(2).forEach { well.add("${it.habit.name}: ${daysText(it.done, it.due)}") }
    if (tasksDone > 0) well.add(if (tasksDone == 1) "1 task finished" else "$tasksDone tasks finished")
    if (focusMin > 0) well.add("${minutesText(focusMin)} of focused work")

    val slipped = thisWeek.filter { it.done < it.due }.sortedBy { it.ratio }.take(2)

    val saved = vm.weekReviewFor(week)
    var editing by remember { mutableStateOf(false) }
    var kind by remember(saved) { mutableIntStateOf(if (saved?.kind == "drop") 1 else 0) }
    var note by remember(saved) { mutableStateOf(saved?.note ?: "") }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Ic.BACK, onClick = onBack)
            Spacer(Modifier.width(14.dp))
            Column {
                Txt("Your week", size = 26.sp, weight = FontWeight.SemiBold, serif = true)
                Txt(rangeText(week, end), size = 13.sp, color = p.muted)
            }
        }

        // The week at a glance
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(if (pct == null) "–" else "$pct%", size = 40.sp, weight = FontWeight.SemiBold, serif = true)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.padding(bottom = 6.dp)) {
                    Txt("of your habits done", size = 13.sp, color = p.muted)
                    if (pct != null && prevPct != null) {
                        val diff = pct - prevPct
                        Txt(
                            when {
                                diff > 0 -> "Up $diff points from last week"
                                diff < 0 -> "Down ${-diff} points from last week"
                                else -> "Same as last week"
                            },
                            size = 12.sp, weight = FontWeight.Medium,
                            color = if (diff >= 0) p.accentText else p.muted
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                for (d in week..end) {
                    val hw = habitWeeks(vm.habits.toList(), d, d)
                    val due = hw.sumOf { it.due }
                    val frac = if (d > today || due == 0) null else hw.sumOf { it.done }.toFloat() / due
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(6.dp)).background(p.field),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (frac != null && frac > 0f) {
                                Box(Modifier.fillMaxWidth().fillMaxHeight(frac).background(p.accent))
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Txt(
                            dayInitial(d), size = 11.sp,
                            weight = if (d == today) FontWeight.Bold else FontWeight.Medium,
                            color = if (d == today) p.text else p.muted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().cardStyle(p).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Stat(tasksDone.toString(), "Tasks done", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(34.dp).background(p.cardBorder))
            Stat(if (focusMin == 0) "0m" else minutesText(focusMin), "Focus", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(34.dp).background(p.cardBorder))
            Stat(avgRating?.let { String.format(Locale.US, "%.1f", it) } ?: "–", "Day rating", Modifier.weight(1f))
        }

        // Went well
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) {
            Txt("What went well", size = 16.sp, weight = FontWeight.SemiBold, serif = true)
            Spacer(Modifier.height(8.dp))
            if (well.isEmpty()) {
                Txt("A quiet week. Starting again is the win.", size = 14.sp, color = p.muted, lineHeight = 19.sp)
            } else {
                well.forEach { Line(it, p.accent) }
            }
        }

        // Slipped
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) {
            Txt("What slipped", size = 16.sp, weight = FontWeight.SemiBold, serif = true)
            Spacer(Modifier.height(8.dp))
            if (slipped.isEmpty()) {
                Txt("Nothing slipped. Protect what worked.", size = 14.sp, color = p.muted, lineHeight = 19.sp)
            } else {
                slipped.forEach { Line("${it.habit.name}: ${daysText(it.done, it.due)}", Amber) }
                Spacer(Modifier.height(6.dp))
                Txt(
                    "One miss is fine. Set a tiny minimum version for these and the run holds.",
                    size = 13.sp, color = p.muted, lineHeight = 18.sp
                )
            }
        }

        // The question
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) {
            Txt("Next week", size = 12.sp, weight = FontWeight.SemiBold, color = p.muted)
            Spacer(Modifier.height(4.dp))
            Txt("What will you protect or drop?", size = 20.sp, weight = FontWeight.SemiBold, serif = true, lineHeight = 26.sp)
            Spacer(Modifier.height(12.dp))
            if (saved != null && !editing) {
                Txt(
                    (if (saved.kind == "drop") "Drop: " else "Protect: ") + saved.note.ifBlank { "nothing written" },
                    size = 15.sp, weight = FontWeight.Medium, lineHeight = 21.sp
                )
                Spacer(Modifier.height(4.dp))
                Txt("It will be waiting on Today all week.", size = 13.sp, color = p.muted)
                Spacer(Modifier.height(10.dp))
                Txt(
                    "Edit",
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = true }.padding(vertical = 6.dp, horizontal = 2.dp),
                    size = 13.sp, weight = FontWeight.SemiBold, color = p.accentText
                )
            } else {
                SegmentedControl(listOf("Protect", "Drop"), kind, { kind = it })
                Spacer(Modifier.height(8.dp))
                Txt(
                    if (kind == 0) "One thing that must not slip, like sleep before 11 or the morning walk."
                    else "One thing to let go of, like late-night scrolling or a task you keep avoiding.",
                    size = 12.sp, color = p.muted, lineHeight = 17.sp
                )
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.field).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    PlainField(note, { note = it.take(120) }, "One line", singleLine = true, size = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    "Close the week", enabled = note.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.saveWeekReview(week, if (kind == 1) "drop" else "protect", note)
                        editing = false
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(value, size = 22.sp, weight = FontWeight.SemiBold, serif = true, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Txt(label, size = 12.sp, color = p.muted, maxLines = 1)
    }
}

@Composable
private fun Line(text: String, dot: androidx.compose.ui.graphics.Color) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        Txt(text, size = 14.sp, lineHeight = 20.sp)
    }
}

/** The card on Today that opens the weekly review when it is due. */
@Composable
fun WeeklyReviewCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier.fillMaxWidth().cardStyle(p).clickable(onClick = onStart).padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt("Review your week", size = 15.sp, weight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Txt("Two minutes: what went well, what slipped, what to protect next.", size = 13.sp, color = p.muted, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 20.dp)
    }
}
