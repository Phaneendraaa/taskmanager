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
import androidx.compose.runtime.mutableStateOf
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
    data object Settings : Screen
    data object NewHabit : Screen
    data class HabitDetail(val id: String) : Screen
    data class EditHabit(val id: String) : Screen
    data class Detail(val id: String) : Screen
    data class EditTask(val id: String) : Screen
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val p = LocalPalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var addMode by rememberSaveable { mutableIntStateOf(1) }
    val stack = remember { mutableStateListOf<Screen>() }
    var showAdd by remember { mutableStateOf(false) }
    var habitMenu by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var taskMenu by remember { mutableStateOf<String?>(null) }
    var confirmDeleteTask by remember { mutableStateOf<String?>(null) }

    fun deleteTaskWithUndo(id: String) {
        val removed = vm.deleteTask(id)
        if (removed != null) {
            scope.launch {
                val r = snackbar.showSnackbar("Task deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) vm.restoreTask(removed)
            }
        }
    }
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
    BackHandler(enabled = showAdd && top == null) { showAdd = false }
    BackHandler(enabled = habitMenu != null) { habitMenu = null }
    BackHandler(enabled = taskMenu != null) { taskMenu = null }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Scaffold(
            containerColor = p.bg,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                BottomTabs(tab, { tab = it }, onAdd = { showAdd = true })
            }
        ) { padding ->
            when (tab) {
                0 -> TodayScreen(
                    vm = vm,
                    padding = padding,
                    onOpenTask = { id -> stack.add(Screen.Detail(id)) },
                    onOpenHabit = { id -> stack.add(Screen.HabitDetail(id)) },
                    onOpenSettings = { stack.add(Screen.Settings) },
                    onAddTask = { stack.add(Screen.NewTask(vm.selectedDay)) },
                    onHabitMore = { habitMenu = it },
                    onTaskMore = { taskMenu = it }
                )
                1 -> Box(Modifier.padding(bottom = padding.calculateBottomPadding())) {
                    HabitsScreen(
                        vm = vm,
                        onBack = null,
                        onOpenHabit = { id -> stack.add(Screen.HabitDetail(id)) },
                        onAdd = { stack.add(Screen.NewHabit) },
                        onHabitMore = { habitMenu = it }
                    )
                }
                2 -> Box(Modifier.padding(bottom = padding.calculateBottomPadding())) {
                    FocusScreen(vm = vm, onClose = null)
                }
                else -> InsightsScreen(
                    vm = vm,
                    padding = padding,
                    onPickDay = { day -> vm.selectedDay = day; tab = 0 }
                )
            }
        }

        if (showAdd && top == null) {
            AddSheet(
                onTask = { showAdd = false; stack.add(Screen.NewTask(today)) },
                onHabit = { showAdd = false; stack.add(Screen.NewHabit) },
                onDismiss = { showAdd = false }
            )
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
                    is Screen.Settings -> ProfileScreen(
                        vm = vm,
                        padding = PaddingValues(0.dp),
                        onOpenHabits = { stack.clear(); tab = 1 },
                        onOpenFocus = { stack.clear(); tab = 2 },
                        onBack = { pop() }
                    )
                    is Screen.HabitDetail -> HabitDetail(
                        vm = vm,
                        habitId = top.id,
                        onBack = { pop() },
                        onEdit = { stack.add(Screen.EditHabit(top.id)) },
                        onDelete = { confirmDelete = top.id }
                    )
                    is Screen.EditHabit -> EditHabitScreen(vm, top.id) { pop() }
                    is Screen.Detail -> TaskDetail(
                        vm = vm,
                        taskId = top.id,
                        today = today,
                        onBack = { pop() },
                        onEdit = { stack.add(Screen.EditTask(top.id)) },
                        onDelete = { confirmDeleteTask = top.id }
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

        val menuHabit = habitMenu?.let { vm.habitById(it) }
        if (menuHabit != null) {
            HabitMenuSheet(
                name = menuHabit.name,
                onEdit = { habitMenu = null; stack.add(Screen.EditHabit(menuHabit.id)) },
                onDelete = { habitMenu = null; confirmDelete = menuHabit.id },
                onDismiss = { habitMenu = null }
            )
        }
        val toDelete = confirmDelete?.let { vm.habitById(it) }
        if (toDelete != null) {
            ConfirmDeleteHabit(
                name = toDelete.name,
                onConfirm = {
                    confirmDelete = null
                    if (top is Screen.HabitDetail && top.id == toDelete.id) pop()
                    val removed = vm.deleteHabit(toDelete.id)
                    if (removed != null) {
                        scope.launch {
                            val r = snackbar.showSnackbar("Habit deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) vm.restoreHabit(removed)
                        }
                    }
                },
                onDismiss = { confirmDelete = null }
            )
        }

        val menuTask = taskMenu?.let { vm.taskById(it) }
        if (menuTask != null) {
            val isPriority = menuTask.id in vm.prioritiesFor(today)
            TaskMenuSheet(
                name = menuTask.title,
                isPriority = isPriority,
                onEdit = { taskMenu = null; stack.add(Screen.EditTask(menuTask.id)) },
                onTogglePriority = {
                    taskMenu = null
                    if (!vm.togglePriority(today, menuTask.id)) {
                        scope.launch { snackbar.showSnackbar("Today already has three priorities. Remove one first.") }
                    }
                },
                onDelete = { taskMenu = null; confirmDeleteTask = menuTask.id },
                onDismiss = { taskMenu = null }
            )
        }
        val taskToDelete = confirmDeleteTask?.let { vm.taskById(it) }
        if (taskToDelete != null) {
            ConfirmDeleteTask(
                name = taskToDelete.title,
                onConfirm = {
                    confirmDeleteTask = null
                    if (top is Screen.Detail && top.id == taskToDelete.id) pop()
                    deleteTaskWithUndo(taskToDelete.id)
                },
                onDismiss = { confirmDeleteTask = null }
            )
        }
    }
}
