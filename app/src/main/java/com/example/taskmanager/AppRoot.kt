package com.example.taskmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Editor {
    data object NewTask : Editor
    data class EditTask(val id: String) : Editor
    data object NewHabit : Editor
    data class EditHabit(val id: String) : Editor
}

@Composable
fun AppRoot(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<Editor?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = vm.now.toLocalDate().toEpochDay()

    // Keep "today" and overdue states fresh while the app stays open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.refreshNow()
        }
    }

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    val current = editor
    if (current != null) {
        BackHandler { editor = null }
        when (current) {
            is Editor.NewTask -> TaskEditor(
                existing = null,
                today = today,
                onSave = { title, notes, day, minute, priority ->
                    vm.addTask(title, notes, day, minute, priority)
                    editor = null
                },
                onDelete = null,
                onClose = { editor = null }
            )

            is Editor.EditTask -> {
                val task = vm.tasks.firstOrNull { it.id == current.id }
                if (task == null) {
                    LaunchedEffect(Unit) { editor = null }
                } else {
                    TaskEditor(
                        existing = task,
                        today = today,
                        onSave = { title, notes, day, minute, priority ->
                            vm.updateTask(task.id, title, notes, day, minute, priority)
                            editor = null
                        },
                        onDelete = {
                            val removed = vm.deleteTask(task.id)
                            editor = null
                            if (removed != null) {
                                showUndo("Task deleted") { vm.restoreTask(removed) }
                            }
                        },
                        onClose = { editor = null }
                    )
                }
            }

            is Editor.NewHabit -> HabitEditor(
                existing = null,
                today = today,
                onSave = { name ->
                    vm.addHabit(name)
                    editor = null
                },
                onDelete = null,
                onClose = { editor = null }
            )

            is Editor.EditHabit -> {
                val habit = vm.habits.firstOrNull { it.id == current.id }
                if (habit == null) {
                    LaunchedEffect(Unit) { editor = null }
                } else {
                    HabitEditor(
                        existing = habit,
                        today = today,
                        onSave = { name ->
                            vm.renameHabit(habit.id, name)
                            editor = null
                        },
                        onDelete = {
                            val removed = vm.deleteHabit(habit.id)
                            editor = null
                            if (removed != null) {
                                showUndo("Habit deleted") { vm.restoreHabit(removed) }
                            }
                        },
                        onClose = { editor = null }
                    )
                }
            }
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (tab != 2) {
                    AppFab {
                        editor = if (tab == 0) Editor.NewTask else Editor.NewHabit
                    }
                }
            },
            bottomBar = { BottomTabs(tab) { tab = it } }
        ) { padding ->
            when (tab) {
                0 -> TasksScreen(vm, padding) { id -> editor = Editor.EditTask(id) }
                1 -> HabitsScreen(vm, padding) { id -> editor = Editor.EditHabit(id) }
                else -> InsightsScreen(vm, padding)
            }
        }
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val labels = listOf("Tasks", "Habits", "Insights")

    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            labels.forEachIndexed { i, label ->
                val isSelected = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) ink else muted
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) ink else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
