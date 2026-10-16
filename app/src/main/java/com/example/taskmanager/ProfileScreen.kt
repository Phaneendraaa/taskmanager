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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onOpenHabits: () -> Unit
) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    val totalDone = vm.tasks.count { it.done }
    val streak = completionStreak(vm.tasks, today)
    val themeIndex = when (vm.themeMode) { "dark" -> 1; "system" -> 2; else -> 0 }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = padding.calculateBottomPadding() + 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Txt("Profile", size = 28.sp, weight = FontWeight.ExtraBold)
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(vm.userName, onClick = {}, size = 60.dp)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Txt("Your name", size = 12.sp, color = p.muted)
                PlainField(
                    value = vm.userName,
                    onValueChange = { vm.updateUserName(it) },
                    placeholder = "Add your name",
                    size = 20.sp,
                    weight = FontWeight.Bold,
                    singleLine = true,
                    imeAction = ImeAction.Done
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox("Completed", totalDone.toString(), Modifier.weight(1f))
            StatBox("Day streak", streak.toString(), Modifier.weight(1f))
            StatBox("Habits", vm.habits.size.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))

        Txt("Appearance", size = 16.sp, weight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        SegmentedControl(
            listOf("Light", "Dark", "System"),
            themeIndex,
            { vm.setTheme(listOf("light", "dark", "system")[it]) }
        )
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(Ic.TARGET, p.accent, size = 22.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Txt("Daily goal", size = 16.sp, weight = FontWeight.SemiBold)
                Txt("Tasks to finish each day", size = 12.sp, color = p.muted)
            }
            Stepper("-") { vm.updateDailyGoal(vm.dailyGoal - 1) }
            Txt(
                vm.dailyGoal.toString(),
                Modifier.padding(horizontal = 14.dp),
                size = 18.sp,
                weight = FontWeight.Bold
            )
            Stepper("+") { vm.updateDailyGoal(vm.dailyGoal + 1) }
        }
        Spacer(Modifier.height(12.dp))

        ActionRow(Ic.REPEAT, "Daily habits", "Track things you do every day", onOpenHabits)
        Spacer(Modifier.height(12.dp))
        ActionRow(Ic.TRASH, "Clear completed tasks", "$totalDone completed") { vm.clearCompleted() }

        Spacer(Modifier.height(24.dp))
        Txt(
            "Everything is stored on this phone only.",
            Modifier.fillMaxWidth(),
            size = 12.sp,
            color = p.muted
        )
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.cardStyle(p, 20.dp).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt(value, size = 26.sp, weight = FontWeight.ExtraBold, color = p.accent)
        Txt(label, size = 12.sp, color = p.muted)
    }
}

@Composable
private fun Stepper(sign: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(p.field).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Txt(sign, size = 20.sp, weight = FontWeight.Bold, color = p.accent)
    }
}

@Composable
private fun ActionRow(ic: Ic, title: String, subtitle: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().cardStyle(p, 22.dp).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(ic, p.accent, size = 22.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, size = 16.sp, weight = FontWeight.SemiBold)
            Txt(subtitle, size = 12.sp, color = p.muted)
        }
        AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 20.dp)
    }
}
