package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

@Composable
fun HomeScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onOpenTask: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHabits: () -> Unit
) {
    val p = LocalPalette.current
    val now = vm.now
    val today = now.toLocalDate().toEpochDay()
    val selected = vm.selectedDay

    var catIndex by rememberSaveable { mutableIntStateOf(-1) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    val all = vm.tasks.toList()
    val byCategory = if (catIndex < 0) all else all.filter { it.category.ordinal == catIndex }
    val order = compareBy<Task>({ it.dueMinute ?: Int.MAX_VALUE }, { -it.priority }, { it.createdAt })

    val dayTasks = byCategory.filter { it.dueDay == selected }.sortedWith(order)
    val overdueList = if (selected == today) {
        byCategory.filter { !it.done && (it.dueDay ?: Long.MAX_VALUE) < today }.sortedBy { it.dueDay }
    } else {
        emptyList()
    }
    val anytime = if (selected == today) {
        byCategory.filter { !it.done && it.dueDay == null }.sortedWith(order)
    } else {
        emptyList()
    }
    val results = if (query.isBlank()) {
        emptyList()
    } else {
        all.filter { it.title.contains(query, ignoreCase = true) || it.notes.contains(query, ignoreCase = true) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {

            if (searching) {
                item {
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
                                placeholder = "Search tasks",
                                singleLine = true,
                                autoFocus = true
                            )
                        }
                    }
                }
                if (query.isBlank()) {
                    item { EmptyNote("Search by title or notes.", "") }
                } else if (results.isEmpty()) {
                    item { EmptyNote("No matches.", "Try a different word.") }
                }
                items(results, key = { it.id }) { t ->
                    TaskCard(
                        task = t,
                        today = today,
                        now = now,
                        onToggle = { vm.toggleTask(t.id) },
                        onClick = { onOpenTask(t.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            } else {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        HeroArt(Modifier.fillMaxSize())
                        Column(
                            Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    RoundIconButton(
                                        Ic.MENU,
                                        onClick = { menuOpen = true },
                                        bg = Color.Transparent,
                                        bordered = false
                                    )
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Daily habits") },
                                            onClick = {
                                                menuOpen = false
                                                onOpenHabits()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (p.dark) "Light mode" else "Dark mode") },
                                            onClick = {
                                                menuOpen = false
                                                vm.setTheme(if (p.dark) "light" else "dark")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Clear completed") },
                                            onClick = {
                                                menuOpen = false
                                                vm.clearCompleted()
                                            }
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                RoundIconButton(
                                    Ic.SEARCH,
                                    onClick = { searching = true },
                                    bg = p.card.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Avatar(vm.userName, onClick = onOpenProfile)
                            }
                            Spacer(Modifier.height(34.dp))
                            Txt(greeting(now.hour) + ",", size = 18.sp, color = p.muted)
                            Spacer(Modifier.height(4.dp))
                            Txt("Let's make", size = 40.sp, weight = FontWeight.ExtraBold, lineHeight = 44.sp)
                            Txt(
                                "it happen.",
                                size = 40.sp,
                                weight = FontWeight.ExtraBold,
                                color = p.accentText,
                                lineHeight = 44.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Txt(
                                "“" + quoteOfTheDay(today) + "”",
                                size = 14.sp,
                                color = p.muted,
                                modifier = Modifier.width(190.dp),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                item { DateStrip(today, selected) { vm.selectedDay = it } }

                item {
                    LazyRow(
                        Modifier.padding(top = 18.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            CategoryChip("All", null, p.text, catIndex < 0) { catIndex = -1 }
                        }
                        items(Category.values().toList()) { c ->
                            CategoryChip(c.label, c.icon, c.color, catIndex == c.ordinal) {
                                catIndex = if (catIndex == c.ordinal) -1 else c.ordinal
                            }
                        }
                    }
                }

                if (overdueList.isNotEmpty()) {
                    item { SectionTitle("Overdue", overdueList.size, StatusRed) }
                    items(overdueList, key = { "o_" + it.id }) { t ->
                        TaskCard(
                            task = t,
                            today = today,
                            now = now,
                            onToggle = { vm.toggleTask(t.id) },
                            onClick = { onOpenTask(t.id) },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
                        )
                    }
                }

                item {
                    SectionTitle(
                        if (selected == today) "Today" else formatDay(selected, today),
                        dayTasks.size,
                        p.text
                    )
                }
                if (dayTasks.isEmpty()) {
                    item {
                        EmptyNote(
                            "Nothing planned.",
                            "Tap + to add a task for this day."
                        )
                    }
                }
                items(dayTasks, key = { it.id }) { t ->
                    TaskCard(
                        task = t,
                        today = today,
                        now = now,
                        onToggle = { vm.toggleTask(t.id) },
                        onClick = { onOpenTask(t.id) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
                    )
                }

                if (anytime.isNotEmpty()) {
                    item { SectionTitle("Anytime", anytime.size, p.text) }
                    items(anytime, key = { "a_" + it.id }) { t ->
                        TaskCard(
                            task = t,
                            today = today,
                            now = now,
                            onToggle = { vm.toggleTask(t.id) },
                            onClick = { onOpenTask(t.id) },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int, color: Color) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(title, size = 24.sp, weight = FontWeight.Bold, color = color, modifier = Modifier.weight(1f))
        Txt(if (count == 1) "1 task" else "$count tasks", size = 14.sp, color = p.muted)
    }
}

@Composable
private fun EmptyNote(title: String, subtitle: String) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt(title, size = 18.sp, weight = FontWeight.SemiBold)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Txt(subtitle, size = 14.sp, color = p.muted)
        }
    }
}

@Composable
private fun DateStrip(today: Long, selected: Long, onSelect: (Long) -> Unit) {
    val p = LocalPalette.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 14)
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(count = 60, key = { it }) { i ->
            val day = today - 14 + i
            val on = day == selected
            val shape = RoundedCornerShape(18.dp)
            if (on) {
                Column(
                    Modifier
                        .width(78.dp)
                        .height(66.dp)
                        .shadow(8.dp, shape, clip = false, ambientColor = p.accent, spotColor = p.accent)
                        .clip(shape)
                        .background(p.gradient)
                        .clickable { onSelect(day) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Txt(shortDow(day), size = 13.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(2.dp))
                    Txt(
                        dayOfMonth(day).toString() + " " + shortMonth(day),
                        size = 16.sp,
                        weight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Column(
                    Modifier
                        .width(56.dp)
                        .height(66.dp)
                        .clip(shape)
                        .background(p.card.copy(alpha = if (p.dark) 1f else 0.85f))
                        .border(1.dp, p.cardBorder, shape)
                        .clickable { onSelect(day) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Txt(shortDow(day), size = 13.sp, color = p.muted)
                    Spacer(Modifier.height(2.dp))
                    Txt(dayOfMonth(day).toString(), size = 17.sp, weight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    today: Long,
    now: LocalDateTime,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val overdue = task.isOverdue(now)
    val minute = task.dueMinute
    val day = task.dueDay
    val whenText = when {
        overdue -> "Overdue"
        minute != null -> formatMinute(minute)
        day != null -> formatDay(day, today)
        else -> "Anytime"
    }
    val subDone = task.subtasks.count { it.done }
    val subText = if (task.subtasks.isNotEmpty()) "  •  $subDone/${task.subtasks.size}" else ""

    Row(
        modifier
            .fillMaxWidth()
            .cardStyle(p, 18.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(task.done, onToggle)
        AppIcon(
            task.iconKind(),
            if (task.done) p.muted else task.category.color,
            size = 22.dp,
            filled = true
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(
                task.title,
                size = 16.sp,
                weight = FontWeight.SemiBold,
                color = if (task.done) p.muted else p.text,
                strike = task.done,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(task.category.color)
                )
                Spacer(Modifier.width(6.dp))
                Txt(
                    task.category.label + "  •  " + whenText + subText,
                    size = 12.sp,
                    color = if (overdue) StatusRed else p.muted,
                    maxLines = 1
                )
            }
        }
        if (task.priority == 3) {
            AppIcon(Ic.FLAG, StatusRed, size = 20.dp)
            Spacer(Modifier.width(8.dp))
        }
    }
}
