package com.example.taskmanager

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun HomeScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onOpenTask: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenFocus: () -> Unit
) {
    val p = LocalPalette.current
    val now = vm.now
    val today = now.toLocalDate().toEpochDay()

    var mode by rememberSaveable { mutableIntStateOf(0) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    val dueHabits = vm.habits
        .filter { it.isDue(today) }
        .sortedWith(compareBy({ it.timeMinute ?: Int.MAX_VALUE }, { it.name }))
    val doneCount = dueHabits.count { today in it.doneDays }
    val fraction = if (dueHabits.isEmpty()) 0f else doneCount.toFloat() / dueHabits.size

    // Share of due habit days completed from Monday up to today.
    val weekStart = today - (LocalDate.ofEpochDay(today).dayOfWeek.value - 1)
    var weekDue = 0
    var weekDone = 0
    for (d in weekStart..today) {
        for (h in vm.habits) {
            if (d >= h.createdDay && h.isDue(d)) {
                weekDue++
                if (d in h.doneDays) weekDone++
            }
        }
    }
    val weekPercent = if (weekDue == 0) 0 else weekDone * 100 / weekDue
    val streak = maxOf(vm.habits.maxOfOrNull { it.currentStreak(today) } ?: 0, completionStreak(vm.tasks, today))

    val selected = vm.selectedDay
    val order = compareBy<Task>({ it.done }, { it.dueMinute ?: Int.MAX_VALUE }, { -it.priority }, { it.createdAt })
    val dayTasks = vm.tasks.filter { it.dueDay == selected }.sortedWith(order)
    val overdueList = if (selected == today) {
        vm.tasks.filter { !it.done && (it.dueDay ?: Long.MAX_VALUE) < today }.sortedBy { it.dueDay }
    } else {
        emptyList()
    }
    val anytime = if (selected == today) {
        vm.tasks.filter { !it.done && it.dueDay == null }.sortedWith(order)
    } else {
        emptyList()
    }
    val pendingTasks = dayTasks.count { !it.done } + overdueList.size + anytime.size

    val habitHits = if (query.isBlank()) emptyList() else vm.habits.filter { it.name.contains(query, ignoreCase = true) }
    val taskHits = if (query.isBlank()) {
        emptyList()
    } else {
        vm.tasks.filter { it.title.contains(query, ignoreCase = true) || it.notes.contains(query, ignoreCase = true) }
    }

    val title = when {
        dueHabits.isNotEmpty() && doneCount == dueHabits.size -> "Perfect day."
        fraction >= 0.5f -> "Consistent today."
        doneCount > 0 -> "Keep going today."
        else -> "Let's start today."
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {

            // Top bar: menu, search, theme switch.
            item {
                if (searching) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundIconButton(Ic.BACK, onClick = {
                            searching = false
                            query = ""
                        })
                        Spacer(Modifier.width(12.dp))
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(p.field)
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(Ic.SEARCH, p.muted, size = 20.dp)
                            Spacer(Modifier.width(10.dp))
                            PlainField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = "Search habits and tasks",
                                singleLine = true,
                                autoFocus = true
                            )
                        }
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            RoundIconButton(Ic.MENU, onClick = { menuOpen = true }, bordered = false, bg = Color.Transparent)
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("All habits") }, onClick = {
                                    menuOpen = false
                                    onOpenHabits()
                                })
                                DropdownMenuItem(text = { Text("Focus timer") }, onClick = {
                                    menuOpen = false
                                    onOpenFocus()
                                })
                                DropdownMenuItem(text = { Text("Clear completed tasks") }, onClick = {
                                    menuOpen = false
                                    vm.clearCompleted()
                                })
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        RoundIconButton(Ic.SEARCH, onClick = { searching = true }, bordered = false, bg = Color.Transparent)
                        Spacer(Modifier.width(8.dp))
                        ThemePill(dark = p.dark) { vm.setTheme(if (p.dark) "light" else "dark") }
                    }
                }
            }

            if (searching) {
                if (query.isBlank()) {
                    item {
                        Txt(
                            "Type to search your habits and tasks.",
                            Modifier.fillMaxWidth().padding(24.dp),
                            size = 14.sp,
                            color = p.muted,
                            align = TextAlign.Center
                        )
                    }
                } else if (habitHits.isEmpty() && taskHits.isEmpty()) {
                    item {
                        Txt(
                            "Nothing found for \"$query\".",
                            Modifier.fillMaxWidth().padding(24.dp),
                            size = 14.sp,
                            color = p.muted,
                            align = TextAlign.Center
                        )
                    }
                } else {
                    if (habitHits.isNotEmpty()) {
                        item { SectionLabel("Habits", habitHits.size.toString()) }
                        items(habitHits.size) { i ->
                            val h = habitHits[i]
                            HabitRow(
                                h, today,
                                onToggle = { vm.toggleHabitDay(h.id, today) },
                                onClick = { onOpenHabit(h.id) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (taskHits.isNotEmpty()) {
                        item { SectionLabel("Tasks", taskHits.size.toString()) }
                        items(taskHits.size) { i ->
                            val t = taskHits[i]
                            TaskCard(
                                t, today, now,
                                onToggle = { vm.toggleTask(t.id) },
                                onClick = { onOpenTask(t.id) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            // Greeting and daily ring.
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        val who = if (vm.userName.isBlank()) "" else "  •  Hi, " + vm.userName
                        Txt(fullDateShort(today) + who, size = 14.sp, color = p.muted)
                        Spacer(Modifier.height(6.dp))
                        Txt(title, size = 34.sp, weight = FontWeight.Bold, serif = true, lineHeight = 38.sp)
                    }
                    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                        ProgressRing(fraction, Modifier.fillMaxSize(), thickness = 10.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Txt("$doneCount/${dueHabits.size}", size = 22.sp, weight = FontWeight.Bold)
                            Txt("habits", size = 12.sp, color = p.muted)
                        }
                    }
                }
            }

            // Stat tiles.
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(Ic.FLAME, Color(0xFFE2864E), streak.toString(), "day streak", Modifier.weight(1f))
                    StatTile(Ic.CHART, p.accent, "$weekPercent%", "this week", Modifier.weight(1f))
                    StatTile(Ic.STAR, Color(0xFFE3B65B), vm.habits.size.toString(), "total habits", Modifier.weight(1f))
                }
            }

            // Habits | Tasks switch.
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    SegmentedControl(
                        listOf("Habits ${dueHabits.size}", "Tasks $pendingTasks"),
                        mode,
                        { mode = it }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (mode == 0) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Txt("Today", size = 18.sp, weight = FontWeight.Bold, serif = true, modifier = Modifier.weight(1f))
                        Txt(
                            "View all",
                            Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpenHabits).padding(6.dp),
                            size = 13.sp,
                            weight = FontWeight.SemiBold,
                            color = p.accentText
                        )
                    }
                }
                if (dueHabits.isEmpty()) {
                    item {
                        EmptyNote(
                            if (vm.habits.isEmpty()) "No habits yet. Tap + to add your first one."
                            else "Nothing due today. Enjoy the rest."
                        )
                    }
                } else {
                    items(dueHabits.size) { i ->
                        val h = dueHabits[i]
                        HabitRow(
                            h, today,
                            onToggle = { vm.toggleHabitDay(h.id, today) },
                            onClick = { onOpenHabit(h.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
            } else {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Txt(
                            formatDay(selected, today),
                            size = 18.sp,
                            weight = FontWeight.Bold,
                            serif = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected != today) {
                            Txt(
                                "Back to today",
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { vm.selectedDay = today }
                                    .padding(6.dp),
                                size = 13.sp,
                                weight = FontWeight.SemiBold,
                                color = p.accentText
                            )
                        }
                    }
                }
                if (overdueList.isNotEmpty()) {
                    item { SectionLabel("Overdue", overdueList.size.toString(), StatusRed) }
                    items(overdueList.size) { i ->
                        val t = overdueList[i]
                        TaskCard(
                            t, today, now,
                            onToggle = { vm.toggleTask(t.id) },
                            onClick = { onOpenTask(t.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
                if (dayTasks.isNotEmpty()) {
                    items(dayTasks.size) { i ->
                        val t = dayTasks[i]
                        TaskCard(
                            t, today, now,
                            onToggle = { vm.toggleTask(t.id) },
                            onClick = { onOpenTask(t.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
                if (anytime.isNotEmpty()) {
                    item { SectionLabel("Anytime", anytime.size.toString()) }
                    items(anytime.size) { i ->
                        val t = anytime[i]
                        TaskCard(
                            t, today, now,
                            onToggle = { vm.toggleTask(t.id) },
                            onClick = { onOpenTask(t.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
                if (dayTasks.isEmpty() && overdueList.isEmpty() && anytime.isEmpty()) {
                    item { EmptyNote("No tasks for this day. Tap + to add one.") }
                }
            }
        }
    }
}

private fun fullDateShort(day: Long): String =
    shortDow(day) + ", " + dayOfMonth(day) + " " + shortMonth(day)

@Composable
private fun SectionLabel(text: String, count: String, color: Color = LocalPalette.current.muted) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(text, size = 14.sp, weight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f))
        Txt(count, size = 13.sp, color = LocalPalette.current.muted)
    }
}

@Composable
private fun EmptyNote(text: String) {
    Txt(
        text,
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 28.dp),
        size = 14.sp,
        color = LocalPalette.current.muted,
        align = TextAlign.Center
    )
}

@Composable
private fun StatTile(ic: Ic, tint: Color, value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Row(
        modifier.cardStyle(p, 18.dp).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(ic, tint, size = 22.dp, filled = ic == Ic.FLAME || ic == Ic.STAR)
        Spacer(Modifier.width(8.dp))
        Column {
            Txt(value, size = 18.sp, weight = FontWeight.Bold, maxLines = 1)
            Txt(label, size = 10.sp, color = p.muted, maxLines = 1)
        }
    }
}

/** Sun / moon switch like the reference. */
@Composable
private fun ThemePill(dark: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(p.field)
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (!dark) p.card else Color.Transparent),
            contentAlignment = Alignment.Center
        ) { AppIcon(Ic.SUN, if (!dark) p.accent else p.muted, size = 18.dp) }
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (dark) p.card else Color.Transparent),
            contentAlignment = Alignment.Center
        ) { AppIcon(Ic.MOON, if (dark) p.accent else p.muted, size = 18.dp) }
    }
}
