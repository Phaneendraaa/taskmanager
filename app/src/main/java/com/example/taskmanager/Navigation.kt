package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class TabSpec(val ic: Ic, val label: String)

private val TABS = listOf(
    TabSpec(Ic.HOME, "Today"),
    TabSpec(Ic.REPEAT, "Habits"),
    TabSpec(Ic.TIMER, "Focus"),
    TabSpec(Ic.CHART, "Insights")
)

/** Flat bottom bar with four tabs. Nothing floats, nothing is raised. */
@Composable
fun BottomTabs(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(p.bg)
    ) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp)
        ) {
            TABS.forEachIndexed { i, spec ->
                if (i == 2) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onAdd),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(p.inverse),
                            contentAlignment = Alignment.Center
                        ) { AppIcon(Ic.PLUS, p.onInverse, size = 24.dp, stroke = 2.4f) }
                    }
                }
                val on = i == tab
                val tint = if (on) p.text else p.muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (on) p.text else p.bg)
                    )
                    Spacer(Modifier.height(9.dp))
                    AppIcon(spec.ic, tint, size = 23.dp, stroke = if (on) 2.1f else 1.7f)
                    Spacer(Modifier.height(3.dp))
                    Txt(
                        spec.label,
                        size = 11.sp,
                        weight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Small sheet over the app that asks what to create. */
@Composable
fun AddSheet(onTask: () -> Unit, onHabit: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(p.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Txt("Create", size = 22.sp, weight = FontWeight.SemiBold, serif = true)
            Spacer(Modifier.height(4.dp))
            Txt("What would you like to add?", size = 14.sp, color = p.muted)
            Spacer(Modifier.height(16.dp))
            AddOption(Ic.NOTES, "New task", "Something to get done, with a date if you like", onTask)
            Spacer(Modifier.height(10.dp))
            AddOption(Ic.REPEAT, "New habit", "Something to repeat, tied to your day", onHabit)
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AddOption(ic: Ic, title: String, sub: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().cardStyle(p).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(p.field),
            contentAlignment = Alignment.Center
        ) { AppIcon(ic, p.text, size = 22.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, size = 16.sp, weight = FontWeight.SemiBold)
            Txt(sub, size = 12.sp, color = p.muted, maxLines = 2)
        }
        AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 18.dp)
    }
}

/**
 * Rest days: pause habits for a day, a few days or a week without breaking any run.
 * [onRest] gets the first day as an offset from today (0 or -1) and the number of days.
 */
@Composable
fun RestSheet(restingUntil: String?, onRest: (Int, Int) -> Unit, onEnd: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(p.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Txt("Take a rest", size = 22.sp, weight = FontWeight.SemiBold, serif = true)
            Spacer(Modifier.height(4.dp))
            Txt(
                if (restingUntil != null) "Habits are paused through $restingUntil. Your streaks are safe."
                else "Sick, travelling or just tired? Habits pause and your streaks stay safe.",
                size = 14.sp, color = p.muted, lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))
            if (restingUntil != null) {
                AddOption(Ic.PLAY, "Resume today", "Habits come back right now", onEnd)
                Spacer(Modifier.height(10.dp))
            } else {
                AddOption(Ic.MOON, "Just today", "Back to normal tomorrow") { onRest(0, 1) }
                Spacer(Modifier.height(10.dp))
            }
            AddOption(Ic.MOON, "Three days", "Today and the next two") { onRest(0, 3) }
            Spacer(Modifier.height(10.dp))
            AddOption(Ic.MOON, "One week", "Seven days, starting today") { onRest(0, 7) }
            if (restingUntil == null) {
                Spacer(Modifier.height(10.dp))
                AddOption(Ic.CLOCK, "Yesterday was a rest day", "Remove yesterday's miss from your run") { onRest(-1, 1) }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** Edit or delete one habit. */
@Composable
fun HabitMenuSheet(name: String, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(p.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Txt(name, size = 20.sp, weight = FontWeight.SemiBold, serif = true, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            AddOption(Ic.EDIT, "Edit habit", "Name, time, days, anchor and reminder", onEdit)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().cardStyle(p).clickable(onClick = onDelete).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(p.field),
                    contentAlignment = Alignment.Center
                ) { AppIcon(Ic.TRASH, StatusRed, size = 22.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Txt("Delete habit", size = 16.sp, weight = FontWeight.SemiBold, color = StatusRed)
                    Txt("You will be asked to confirm", size = 12.sp, color = p.muted)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun ConfirmDeleteHabit(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("Delete \"$name\"?") },
        text = { androidx.compose.material3.Text("Its history and reminders will be removed. You can undo right after.") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                androidx.compose.material3.Text("Delete", color = StatusRed)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Cancel") } }
    )
}

/** Edit, make today's priority, or delete one task. */
@Composable
fun TaskMenuSheet(
    name: String,
    isPriority: Boolean,
    onEdit: () -> Unit,
    onTogglePriority: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(p.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Txt(name, size = 20.sp, weight = FontWeight.SemiBold, serif = true, maxLines = 2)
            Spacer(Modifier.height(16.dp))
            AddOption(Ic.EDIT, "Edit task", "Title, date, time, priority and notes", onEdit)
            Spacer(Modifier.height(10.dp))
            if (isPriority) {
                AddOption(Ic.CLOSE, "Remove from today's priorities", "It stays in your task list", onTogglePriority)
            } else {
                AddOption(Ic.TARGET, "Make it today's priority", "One of the three that make today a good day", onTogglePriority)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().cardStyle(p).clickable(onClick = onDelete).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(p.field),
                    contentAlignment = Alignment.Center
                ) { AppIcon(Ic.TRASH, StatusRed, size = 22.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Txt("Delete task", size = 16.sp, weight = FontWeight.SemiBold, color = StatusRed)
                    Txt("You will be asked to confirm", size = 12.sp, color = p.muted)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun ConfirmDeleteTask(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("Delete \"$name\"?") },
        text = { androidx.compose.material3.Text("The task and its sub-tasks will be removed. You can undo right after.") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                androidx.compose.material3.Text("Delete", color = StatusRed)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Cancel") } }
    )
}
