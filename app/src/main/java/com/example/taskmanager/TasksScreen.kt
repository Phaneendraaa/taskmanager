package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    OVERDUE("Overdue"),
    DONE("Done")
}

private data class Section(val title: String, val items: List<Task>)

private fun buildSections(tasks: List<Task>, filter: TaskFilter, now: LocalDateTime): List<Section> {
    val today = now.toLocalDate().toEpochDay()
    val order = compareBy<Task>(
        { it.dueDay ?: Long.MAX_VALUE },
        { it.dueMinute ?: Int.MAX_VALUE },
        { -it.priority },
        { it.createdAt }
    )
    val open = tasks.filter { !it.done }
    val overdue = open.filter { it.isOverdue(now) }.sortedWith(order)
    val rest = open.filter { !it.isOverdue(now) }
    val todayList = rest.filter { it.dueDay == today }.sortedWith(order)

    val sections = when (filter) {
        TaskFilter.ALL -> listOf(
            Section("Overdue", overdue),
            Section("Today", todayList),
            Section("Tomorrow", rest.filter { it.dueDay == today + 1 }.sortedWith(order)),
            Section(
                "Upcoming",
                rest.filter { val d = it.dueDay; d != null && d > today + 1 }.sortedWith(order)
            ),
            Section("Anytime", rest.filter { it.dueDay == null }.sortedWith(order))
        )
        TaskFilter.TODAY -> listOf(Section("Overdue", overdue), Section("Today", todayList))
        TaskFilter.OVERDUE -> listOf(Section("Overdue", overdue))
        TaskFilter.DONE -> listOf(
            Section("Completed", tasks.filter { it.done }.sortedByDescending { it.completedAt ?: 0L })
        )
    }
    return sections.filter { it.items.isNotEmpty() }
}

@Composable
fun TasksScreen(vm: AppViewModel, padding: PaddingValues, onOpen: (String) -> Unit) {
    val now = vm.now
    val today = now.toLocalDate().toEpochDay()
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val filter = TaskFilter.values()[filterIndex]

    val tasks = vm.tasks.toList()
    val open = tasks.filter { !it.done }
    val overdueCount = open.count { it.isOverdue(now) }
    val todayTasks = tasks.filter { it.dueDay == today }
    val dueTodayOpen = todayTasks.count { !it.done }
    val todayDone = todayTasks.count { it.done }
    val sections = buildSections(tasks, filter, now)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
    ) {
        item { ScreenHeader("Tasks", now) }

        item {
            Row(Modifier.fillMaxWidth()) {
                Stat(open.size.toString(), "Open", Modifier.weight(1f))
                Stat(dueTodayOpen.toString(), "Due today", Modifier.weight(1f))
                Stat(overdueCount.toString(), "Overdue", Modifier.weight(1f))
            }
        }

        if (todayTasks.isNotEmpty()) {
            item {
                Column(Modifier.padding(top = 24.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        SmallCaps("Today's progress", Modifier.weight(1f))
                        SmallCaps("$todayDone / ${todayTasks.size}")
                    }
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(todayDone.toFloat() / todayTasks.size)
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                TaskFilter.values().forEachIndexed { i, f ->
                    FilterTab(f.label, i == filterIndex) { filterIndex = i }
                }
            }
            Hairline()
        }

        if (sections.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (filter) {
                            TaskFilter.DONE -> "Nothing completed yet."
                            TaskFilter.OVERDUE -> "Nothing overdue."
                            TaskFilter.TODAY -> "A clear day."
                            TaskFilter.ALL -> "A clean slate."
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (filter == TaskFilter.ALL) "Tap + to add your first task." else "Switch filter to see other tasks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        sections.forEach { section ->
            item(key = "section_" + section.title) {
                SectionHeader(
                    title = section.title,
                    count = section.items.size,
                    actionLabel = if (filter == TaskFilter.DONE) "Clear all" else null,
                    onAction = { vm.clearCompleted() }
                )
            }
            items(section.items, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    now = now,
                    onToggle = { vm.toggleTask(task.id) },
                    onClick = { onOpen(task.id) }
                )
                Hairline()
            }
        }
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick)
            .padding(top = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) ink else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(if (selected) ink else Color.Transparent)
        )
    }
}

@Composable
fun TaskRow(task: Task, now: LocalDateTime, onToggle: () -> Unit, onClick: () -> Unit) {
    val today = now.toLocalDate().toEpochDay()
    val overdue = task.isOverdue(now)
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleCheck(checked = task.done, onClick = onToggle)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.done) muted else ink,
                textDecoration = if (task.done) TextDecoration.LineThrough else null
            )
            val due = task.dueDay
            val showMeta = due != null || task.priority > 0 || task.notes.isNotBlank()
            if (showMeta) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (due != null) {
                        val label = if (overdue) {
                            lateText(task, today) + " · " + dueText(task, today)
                        } else {
                            dueText(task, today)
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (overdue) ink else muted
                        )
                    }
                    if (task.priority > 0) {
                        if (due != null) Spacer(Modifier.width(10.dp))
                        PriorityTag(task.priority)
                    }
                    if (task.notes.isNotBlank()) {
                        if (due != null || task.priority > 0) Spacer(Modifier.width(10.dp))
                        Text("Note", style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityTag(priority: Int) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val high = priority >= 3
    val label = when (priority) {
        3 -> "HIGH"
        2 -> "MED"
        else -> "LOW"
    }
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = label,
        modifier = Modifier
            .background(if (high) ink else Color.Transparent, shape)
            .border(0.8.dp, if (high) ink else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), shape)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (high) paper else ink
    )
}
