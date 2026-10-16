package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * Today: plan, do, review. Three priorities, the routine grouped by time of day,
 * what is coming up, and a short evening check-in.
 */
@Composable
fun TodayScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onOpenTask: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAddTask: () -> Unit
) {
    val p = LocalPalette.current
    val now = vm.now
    val today = now.toLocalDate().toEpochDay()

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var picker by remember { mutableStateOf(false) }

    val dueHabits = vm.habits
        .filter { it.isDue(today) && it.createdDay <= today }
        .sortedWith(compareBy({ it.timeMinute ?: Int.MAX_VALUE }, { it.name }))
    val doneHabits = dueHabits.count { today in it.doneDays }
    val atRisk = dueHabits.filter { it.atRisk(today) }

    val order = compareBy<Task>({ it.done }, { it.dueMinute ?: Int.MAX_VALUE }, { -it.priority }, { it.createdAt })
    val pickedIds = vm.prioritiesFor(today)
    val picked = pickedIds.mapNotNull { id -> vm.tasks.firstOrNull { it.id == id } }
    val candidates = vm.tasks
        .filter { !it.done && it.id !in pickedIds && (it.dueDay == null || it.dueDay!! <= today + 7) }
        .sortedWith(compareBy({ it.dueDay ?: Long.MAX_VALUE }, { -it.priority }))
    val overdue = vm.tasks
        .filter { !it.done && it.id !in pickedIds && (it.dueDay ?: Long.MAX_VALUE) < today }
        .sortedBy { it.dueDay }
    val todayOther = vm.tasks.filter { it.dueDay == today && it.id !in pickedIds && it.id !in overdue.map { o -> o.id } }.sortedWith(order)
    val upcoming = vm.tasks
        .filter { !it.done && (it.dueDay ?: -1L) > today && (it.dueDay ?: -1L) <= today + 7 && it.id !in pickedIds }
        .sortedBy { it.dueDay }
        .take(4)

    val focusToday = vm.focusSessions.filter { epochDayOf(it.endedAt) == today }.sumOf { it.minutes }

    val hits = if (query.isBlank()) emptyList() else vm.tasks.filter {
        it.title.contains(query, true) || it.notes.contains(query, true)
    }
    val habitHits = if (query.isBlank()) emptyList() else vm.habits.filter { it.name.contains(query, true) }

    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    if (picker) {
        PriorityPicker(
            candidates = candidates,
            onPick = { id ->
                vm.togglePriority(today, id)
                if (vm.prioritiesFor(today).size >= 3) picker = false
            },
            onNew = { picker = false; onAddTask() },
            onDismiss = { picker = false }
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())
            .background(p.bg)
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {

            // Header
            item {
                if (searching) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundIconButton(Ic.BACK, onClick = { searching = false; query = "" })
                        Spacer(Modifier.width(12.dp))
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(p.field)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(Ic.SEARCH, p.muted, size = 19.dp)
                            Spacer(Modifier.width(10.dp))
                            PlainField(
                                value = query, onValueChange = { query = it },
                                placeholder = "Search habits and tasks", singleLine = true, autoFocus = true
                            )
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Txt(
                                shortDow(today).uppercase() + "  " + dayOfMonth(today) + " " + shortMonth(today).uppercase(),
                                size = 12.sp, weight = FontWeight.SemiBold, color = p.muted
                            )
                            Spacer(Modifier.height(6.dp))
                            Txt(
                                if (vm.userName.isBlank()) greeting else "$greeting, ${vm.userName.trim()}",
                                size = 26.sp, weight = FontWeight.SemiBold, serif = true, lineHeight = 32.sp
                            )
                        }
                        RoundIconButton(Ic.SEARCH, onClick = { searching = true }, bordered = false, bg = androidx.compose.ui.graphics.Color.Transparent)
                        RoundIconButton(Ic.PLUS, onClick = onAddTask, bordered = false, bg = androidx.compose.ui.graphics.Color.Transparent)
                        RoundIconButton(Ic.GEAR, onClick = onOpenSettings, bordered = false, bg = androidx.compose.ui.graphics.Color.Transparent)
                    }
                }
            }

            if (searching) {
                if (query.isBlank() || (hits.isEmpty() && habitHits.isEmpty())) {
                    item {
                        Txt(
                            if (query.isBlank()) "Type to search your habits and tasks." else "Nothing found for \"$query\".",
                            Modifier.fillMaxWidth().padding(24.dp), size = 14.sp, color = p.muted, align = TextAlign.Center
                        )
                    }
                }
                items(habitHits.size) { i ->
                    val h = habitHits[i]
                    HabitRow(h, today, { vm.toggleHabitDay(h.id, today) }, { onOpenHabit(h.id) },
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
                items(hits.size) { i ->
                    val t = hits[i]
                    TaskCard(t, today, now, { vm.toggleTask(t.id) }, { onOpenTask(t.id) },
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
                return@LazyColumn
            }

            // Day summary: one flat card, three numbers.
            item {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().cardStyle(p).padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryCell("$doneHabits/${dueHabits.size}", "Habits", Modifier.weight(1f))
                    VDivider()
                    SummaryCell("${picked.count { it.done }}/${picked.size}", "Priorities", Modifier.weight(1f))
                    VDivider()
                    SummaryCell(if (focusToday >= 60) "${focusToday / 60}h ${focusToday % 60}m" else "${focusToday}m", "Focus", Modifier.weight(1f))
                }
            }

            // Don't miss twice.
            if (atRisk.isNotEmpty()) {
                item {
                    Row(
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp).fillMaxWidth().cardStyle(p).height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                    ) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(Amber))
                        Column(Modifier.padding(14.dp)) {
                            Txt("Don't miss twice", size = 14.sp, weight = FontWeight.SemiBold)
                            Spacer(Modifier.height(3.dp))
                            Txt(
                                "One miss is fine. Today brings " + atRisk.take(2).joinToString(" and ") { it.name } +
                                    (if (atRisk.size > 2) " and ${atRisk.size - 2} more" else "") + " back on track.",
                                size = 13.sp, color = p.muted, lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Priorities
            item { Header("Today's three", "${picked.size} of 3") }
            if (picked.isEmpty()) {
                item {
                    Txt(
                        "Choose up to three things that would make today a good day.",
                        Modifier.padding(horizontal = 22.dp).padding(bottom = 10.dp), size = 13.sp, color = p.muted, lineHeight = 18.sp
                    )
                }
            }
            items(picked.size) { i ->
                val t = picked[i]
                Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt("${i + 1}", Modifier.width(18.dp), size = 15.sp, serif = true, weight = FontWeight.SemiBold, color = p.muted)
                    TaskCard(t, today, now, { vm.toggleTask(t.id) }, { onOpenTask(t.id) }, Modifier.weight(1f))
                }
            }
            if (picked.size < 3) {
                item {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth()
                            .cardStyle(p).clickable { picker = true }.padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(Ic.PLUS, p.accentText, size = 19.dp, stroke = 2.2f)
                        Spacer(Modifier.width(12.dp))
                        Txt("Add a priority", size = 15.sp, weight = FontWeight.Medium, color = p.accentText)
                    }
                }
            }

            // Routine
            item { Header("Routine", "$doneHabits of ${dueHabits.size}") }
            if (dueHabits.isEmpty()) {
                item {
                    EmptyNote(if (vm.habits.isEmpty()) "No habits yet. Add your first in the Habits tab." else "Nothing due today.")
                }
            }
            val groups = dueHabits.groupBy { it.period() }
            for (period in listOf(Period.MORNING, Period.AFTERNOON, Period.EVENING, Period.ANYTIME)) {
                val list = groups[period] ?: continue
                item {
                    Txt(period.label.uppercase(), Modifier.padding(start = 22.dp, top = 8.dp, bottom = 4.dp),
                        size = 11.sp, weight = FontWeight.SemiBold, color = p.muted)
                }
                items(list.size) { i ->
                    val h = list[i]
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        HabitRow(h, today, { vm.toggleHabitDay(h.id, today) }, { onOpenHabit(h.id) })
                        if (h.minimum.isNotBlank() && today !in h.doneDays) {
                            Txt(
                                "Short on time? " + h.minimum.trim().trimEnd('.'),
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .clickable { vm.doMinimum(h.id, today) }.padding(start = 6.dp, top = 6.dp, bottom = 2.dp),
                                size = 12.sp, color = p.accentText, weight = FontWeight.Medium, maxLines = 1
                            )
                        } else if (today in h.miniDays) {
                            Txt("Minimum version done. It still counts.", Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp),
                                size = 12.sp, color = p.muted)
                        }
                    }
                }
            }

            // Other tasks
            if (overdue.isNotEmpty()) {
                item { Header("Overdue", overdue.size.toString(), StatusRed) }
                items(overdue.size) { i ->
                    val t = overdue[i]
                    TaskCard(t, today, now, { vm.toggleTask(t.id) }, { onOpenTask(t.id) }, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
            }
            if (todayOther.isNotEmpty()) {
                item { Header("Also today", todayOther.size.toString()) }
                items(todayOther.size) { i ->
                    val t = todayOther[i]
                    TaskCard(t, today, now, { vm.toggleTask(t.id) }, { onOpenTask(t.id) }, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
            }
            if (upcoming.isNotEmpty()) {
                item { Header("Coming up", "next 7 days") }
                items(upcoming.size) { i ->
                    val t = upcoming[i]
                    TaskCard(t, today, now, { vm.toggleTask(t.id) }, { onOpenTask(t.id) }, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
            }

            // Evening check-in
            item { Spacer(Modifier.height(14.dp)); EveningReview(vm, today, now.hour) }
        }
    }
}

@Composable
private fun SummaryCell(value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(value, size = 22.sp, weight = FontWeight.SemiBold, serif = true, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Txt(label, size = 12.sp, color = p.muted, maxLines = 1)
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.dp).height(34.dp).background(LocalPalette.current.cardBorder))
}

@Composable
private fun Header(text: String, trailing: String, trailingColor: androidx.compose.ui.graphics.Color = LocalPalette.current.muted) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(text, size = 19.sp, weight = FontWeight.SemiBold, serif = true, modifier = Modifier.weight(1f))
        Txt(trailing, size = 13.sp, color = trailingColor, maxLines = 1)
    }
}

@Composable
private fun EmptyNote(text: String) {
    Txt(text, Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
        size = 14.sp, color = LocalPalette.current.muted, align = TextAlign.Center)
}

@Composable
private fun EveningReview(vm: AppViewModel, today: Long, hour: Int) {
    val p = LocalPalette.current
    val saved = vm.reviewFor(today)
    var editing by remember { mutableStateOf(false) }
    if (saved == null && hour < 17) return
    var rating by remember(saved) { mutableStateOf(saved?.rating ?: 0) }
    var note by remember(saved) { mutableStateOf(saved?.note ?: "") }
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().cardStyle(p).padding(16.dp)) {
        if (saved != null && !editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt("Day reviewed", size = 15.sp, weight = FontWeight.SemiBold)
                    Txt("${saved.rating} out of 5" + if (saved.note.isNotBlank()) "  •  " + saved.note else "",
                        size = 13.sp, color = p.muted, maxLines = 2)
                }
                Txt("Edit", Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = true }.padding(8.dp),
                    size = 13.sp, weight = FontWeight.SemiBold, color = p.accentText)
            }
        } else {
            Txt("Evening check-in", size = 15.sp, weight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Txt("How did today go? Twenty seconds, then let it go.", size = 13.sp, color = p.muted, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in 1..5) {
                    val on = rating == n
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (on) p.inverse else p.field).clickable { rating = n },
                        contentAlignment = Alignment.Center
                    ) {
                        Txt("$n", size = 16.sp, weight = FontWeight.SemiBold, color = if (on) p.onInverse else p.text)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.field).padding(horizontal = 14.dp, vertical = 12.dp)) {
                PlainField(note, { note = it }, "One line: what worked, what to change", singleLine = true, size = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            GradientButton("Save check-in", enabled = rating > 0, onClick = {
                vm.saveReview(today, rating, note)
                editing = false
            })
        }
    }
}

@Composable
private fun PriorityPicker(
    candidates: List<Task>,
    onPick: (String) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a priority") },
        text = {
            if (candidates.isEmpty()) {
                Text("You have no open tasks yet. Create one and it will be added here.")
            } else {
                Column(Modifier.fillMaxWidth()) {
                    candidates.take(8).forEach { t ->
                        Text(
                            t.title,
                            Modifier.fillMaxWidth().clickable { onPick(t.id) }.padding(vertical = 12.dp),
                            maxLines = 2
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onNew) { Text("New task") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
