package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun HabitDetail(
    vm: AppViewModel,
    habitId: String,
    onBack: () -> Unit,
    onDeleted: (Habit) -> Unit
) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val habit = vm.habitById(habitId)
    val draft = remember(habitId) { HabitDraft(habit) }

    // The habit only disappears through Delete, which closes this screen.
    if (habit == null) return

    val tint = LocalPalette.current.muted
    val streak = habit.currentStreak(today)

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Ic.BACK, onClick = onBack)
            Spacer(Modifier.size(14.dp))
            Txt("Habit details", size = 22.sp, weight = FontWeight.Bold, serif = true)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(LocalPalette.current.field),
                    contentAlignment = Alignment.Center
                ) { AppIcon(habit.iconKind(), tint, size = 30.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Txt(habit.name, size = 20.sp, weight = FontWeight.Bold, serif = true, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(Ic.FLAME, p.muted, size = 16.dp)
                        Spacer(Modifier.width(4.dp))
                        Txt("${habit.graceStreak(today)} day run", size = 13.sp, color = p.muted)
                        Txt("  •  " + habit.category.label, size = 13.sp, color = p.muted)
                    }
                    Txt(habitSubtitle(habit), size = 12.sp, color = p.muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            MakeItStick(vm, habit, today)
            Spacer(Modifier.height(16.dp))
            SegmentedControl(listOf("Overview", "History", "Edit"), tab, { tab = it })
            Spacer(Modifier.height(18.dp))

            when (tab) {
                0 -> Overview(vm, habit, today)
                1 -> History(vm, habit, today)
                else -> {
                    HabitFields(draft)
                    Spacer(Modifier.height(24.dp))
                    GradientButton(
                        text = "Save changes",
                        onClick = {
                            vm.upsertHabit(draft.applyTo(habit))
                            tab = 0
                        },
                        enabled = draft.name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, StatusRed.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .clickable {
                                val removed = vm.deleteHabit(habit.id)
                                if (removed != null) onDeleted(removed)
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(Ic.TRASH, StatusRed, size = 20.dp)
                        Spacer(Modifier.width(8.dp))
                        Txt("Delete habit", size = 15.sp, weight = FontWeight.SemiBold, color = StatusRed)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun Overview(vm: AppViewModel, habit: Habit, today: Long) {
    val p = LocalPalette.current
    val weekStart = today - (LocalDate.ofEpochDay(today).dayOfWeek.value - 1)

    Row(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(vertical = 16.dp, horizontal = 6.dp)) {
        for (i in 0..6) {
            val day = weekStart + i
            val done = day in habit.doneDays
            val due = habit.isDue(day)
            val isToday = day == today
            val future = day > today
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (done) p.accent else Color.Transparent)
                        .border(
                            1.5.dp,
                            when {
                                done -> p.accent
                                isToday -> p.accent
                                else -> p.faint
                            },
                            CircleShape
                        )
                        .clickable(enabled = !future) { vm.toggleHabitDay(habit.id, day) },
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        AppIcon(Ic.CHECK, Color.White, size = 16.dp, stroke = 2.6f)
                    } else {
                        Txt(
                            dayOfMonth(day).toString(),
                            size = 11.sp,
                            color = if (due && !future) p.text else p.muted
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Txt(
                    dayInitial(day),
                    size = 12.sp,
                    weight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) p.accentText else p.muted
                )
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Row(
        Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BigStat("${habit.overallRate(today)}%", "Completion")
        BigStat(habit.doneDays.size.toString(), "Total days")
        BigStat(habit.bestStreak().toString(), "Best run")
    }

    Spacer(Modifier.height(14.dp))
    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp)) {
        Txt("Last 16 weeks", size = 15.sp, weight = FontWeight.Bold, serif = true)
        Spacer(Modifier.height(12.dp))
        Heatmap(habit.doneDays, today)
    }
}

@Composable
private fun MakeItStick(vm: AppViewModel, habit: Habit, today: Long) {
    val p = LocalPalette.current
    val auto = habit.isAutomatic(today)
    val day = (today - habit.createdDay + 1).toInt().coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) {
        val sentence = habit.cueSentence()
        if (sentence != null) {
            Txt(sentence, size = 15.sp, weight = FontWeight.Medium, lineHeight = 21.sp)
            Spacer(Modifier.height(10.dp))
        }
        if (habit.identity.isNotBlank()) {
            Txt("Every rep is a vote for being " + habit.identity.trim().trimEnd('.') + ".", size = 13.sp, color = p.muted, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
        }
        if (habit.minimum.isNotBlank()) {
            Txt("Minimum: " + habit.minimum.trim(), size = 13.sp, color = p.muted)
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(
                if (auto) "Automatic" else "Day ${minOf(day, 66)} of 66",
                size = 13.sp, weight = FontWeight.SemiBold,
                color = if (auto) p.accentText else p.text, modifier = Modifier.weight(1f)
            )
            Txt("${habit.consistency(today)}% over 30 days", size = 13.sp, color = p.muted)
        }
        Spacer(Modifier.height(8.dp))
        GradientProgress(if (auto) 1f else habit.automaticProgress(today), Modifier.fillMaxWidth(), 6.dp)
        Spacer(Modifier.height(8.dp))
        Txt(
            if (auto) "This habit runs on its own now. You can ease off the reminders."
            else "On average a habit takes about 66 days to become automatic. One missed day barely matters.",
            size = 12.sp, color = p.muted, lineHeight = 17.sp
        )
    }
}

@Composable
private fun BigStat(value: String, label: String) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(value, size = 26.sp, weight = FontWeight.Bold, serif = true)
        Txt(label, size = 12.sp, color = p.muted, align = TextAlign.Center)
    }
}

@Composable
private fun History(vm: AppViewModel, habit: Habit, today: Long) {
    val p = LocalPalette.current
    val first = maxOf(habit.createdDay, today - 59)
    Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(vertical = 6.dp)) {
        var d = today
        while (d >= first) {
            val day = d
            val done = day in habit.doneDays
            val due = habit.isDue(day)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.toggleHabitDay(habit.id, day) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt(formatDay(day, today), size = 15.sp, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
                val label = when {
                    done -> "Done"
                    !due -> "Not scheduled"
                    day == today -> "Pending"
                    else -> "Missed"
                }
                val color = when {
                    done -> p.accentText
                    !due -> p.muted
                    day == today -> p.muted
                    else -> StatusRed
                }
                Txt(label, size = 13.sp, weight = FontWeight.SemiBold, color = color)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (done) p.accent else Color.Transparent)
                        .border(1.5.dp, if (done) p.accent else p.faint, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) AppIcon(Ic.CHECK, Color.White, size = 13.dp, stroke = 2.6f)
                }
            }
            d--
        }
    }
    Spacer(Modifier.height(10.dp))
    Txt("Tap a day to mark it done or undo it.", size = 12.sp, color = p.muted)
}
