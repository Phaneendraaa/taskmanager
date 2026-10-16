package com.example.taskmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Full-screen pages that open on top of the tabs. */
sealed interface Screen {
    data class NewTask(val day: Long?) : Screen
    data object Habits : Screen
    data object NewHabit : Screen
    data object Focus : Screen
    data class HabitDetail(val id: String) : Screen
    data class Detail(val id: String) : Screen
    data class EditTask(val id: String) : Screen
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val p = LocalPalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var addMode by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Screen>() }
    val today = vm.now.toLocalDate().toEpochDay()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Keep "today" and overdue states fresh while the app stays open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.refreshNow()
        }
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    val top = stack.lastOrNull()
    BackHandler(enabled = top != null) { pop() }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Scaffold(
            containerColor = p.bg,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                FloatingNav(tab, { tab = it }, onAdd = { stack.add(if (addMode == 0) Screen.NewHabit else Screen.NewTask(vm.selectedDay)) })
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(
                    vm = vm,
                    padding = padding,
                    onOpenTask = { id -> stack.add(Screen.Detail(id)) },
                    onOpenHabit = { id -> stack.add(Screen.HabitDetail(id)) },
                    onOpenHabits = { stack.add(Screen.Habits) },
                    onOpenFocus = { stack.add(Screen.Focus) }
                )
                1 -> CalendarScreen(
                    vm = vm,
                    padding = padding,
                    onOpenTask = { id -> stack.add(Screen.Detail(id)) }
                )
                2 -> StatsScreen(
                    vm = vm,
                    padding = padding,
                    onPickDay = { day -> vm.selectedDay = day; tab = 0 }
                )
                else -> ProfileScreen(
                    vm = vm,
                    padding = padding,
                    onOpenHabits = { stack.add(Screen.Habits) }
                )
            }
        }

        if (top != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(p.bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                when (top) {
                    is Screen.NewHabit -> HabitForm(
                        vm = vm,
                        today = today,
                        onFinish = { pop() },
                        onSwitchToTask = {
                            addMode = 1
                            stack[stack.lastIndex] = Screen.NewTask(vm.selectedDay)
                        }
                    )
                    is Screen.NewTask -> TaskForm(
                        vm = vm,
                        existing = null,
                        prefillDay = top.day,
                        today = today,
                        onFinish = { pop() },
                        onSwitchToHabit = {
                            addMode = 0
                            stack[stack.lastIndex] = Screen.NewHabit
                        }
                    )
                    is Screen.Habits -> HabitsScreen(
                        vm = vm,
                        onBack = { pop() },
                        onOpenHabit = { id -> stack.add(Screen.HabitDetail(id)) },
                        onAdd = { stack.add(Screen.NewHabit) }
                    )
                    is Screen.Focus -> FocusScreen(vm = vm, onClose = { pop() })
                    is Screen.HabitDetail -> HabitDetail(
                        vm = vm,
                        habitId = top.id,
                        onBack = { pop() },
                        onDeleted = { h ->
                            pop()
                            scope.launch {
                                val r = snackbar.showSnackbar(
                                    "Habit deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (r == SnackbarResult.ActionPerformed) vm.restoreHabit(h)
                            }
                        }
                    )
                    is Screen.Detail -> TaskDetail(
                        vm = vm,
                        taskId = top.id,
                        today = today,
                        onBack = { pop() },
                        onEdit = { stack.add(Screen.EditTask(top.id)) },
                        onDelete = {
                            val removed = vm.deleteTask(top.id)
                            pop()
                            if (removed != null) {
                                scope.launch {
                                    val r = snackbar.showSnackbar(
                                        "Task deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (r == SnackbarResult.ActionPerformed) vm.restoreTask(removed)
                                }
                            }
                        }
                    )
                    is Screen.EditTask -> {
                        val task = vm.taskById(top.id)
                        if (task != null) {
                            TaskForm(
                                vm = vm,
                                existing = task,
                                prefillDay = null,
                                today = today,
                                onFinish = { pop() }
                            )
                        }
                    }
                }
            }
        }
    }
}
