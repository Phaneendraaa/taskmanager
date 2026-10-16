package com.example.taskmanager

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onOpenHabits: () -> Unit,
    onOpenFocus: () -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val today = vm.now.toLocalDate().toEpochDay()
    val totalDone = vm.tasks.count { it.done }
    val streak = maxOf(vm.habits.maxOfOrNull { it.currentStreak(today) } ?: 0, completionStreak(vm.tasks, today))
    var goalDialog by remember { mutableStateOf(false) }
    var focusDialog by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.updateReminders(true) else toast("Allow notifications for forge to get reminders")
    }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportBackup().toByteArray(Charsets.UTF_8)) }
                toast("Backup saved")
            } catch (e: Exception) {
                toast("Could not save the backup")
            }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                if (text != null && vm.importBackup(text)) toast("Backup restored") else toast("That file is not a forge backup")
            } catch (e: Exception) {
                toast("Could not read that file")
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = padding.calculateBottomPadding() + 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Txt("Settings", size = 32.sp, weight = FontWeight.Bold, serif = true)
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(vm.userName, onClick = {}, size = 58.dp)
            Spacer(Modifier.width(14.dp))
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
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("Tasks done", totalDone.toString(), Modifier.weight(1f))
            StatBox("Day streak", streak.toString(), Modifier.weight(1f))
            StatBox("Habits", vm.habits.size.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        Txt("Appearance", size = 16.sp, weight = FontWeight.Bold, serif = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeTile("Light", Ic.SUN, vm.themeMode == "light", Modifier.weight(1f)) { vm.setTheme("light") }
            ModeTile("Dark", Ic.MOON, vm.themeMode == "dark", Modifier.weight(1f)) { vm.setTheme("dark") }
            ModeTile("System", Ic.GEAR, vm.themeMode == "system", Modifier.weight(1f)) { vm.setTheme("system") }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ACCENTS.forEach { a ->
                val on = vm.accentKey == a.key
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) a.swatch.copy(alpha = 0.12f) else Color.Transparent)
                        .border(1.dp, if (on) a.swatch else p.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { vm.updateAccent(a.key) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(a.swatch),
                        contentAlignment = Alignment.Center
                    ) { if (on) AppIcon(Ic.CHECK, Color.White, size = 14.dp, stroke = 2.6f) }
                    Spacer(Modifier.height(6.dp))
                    Txt(a.name, size = 12.sp, weight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SettingsRow(Ic.TARGET, "Daily goal", "${vm.dailyGoal} habits and tasks per day") { goalDialog = true }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().cardStyle(p, 20.dp).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(p.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { AppIcon(Ic.BELL, p.accent, size = 22.dp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Txt("Reminders", size = 16.sp, weight = FontWeight.SemiBold)
                Txt("Notify at each habit's time", size = 12.sp, color = p.muted, maxLines = 1)
            }
            Switch(
                checked = vm.remindersOn,
                onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.updateReminders(on)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = p.accent,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = p.faint,
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = p.faint
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        SettingsRow(Ic.REPEAT, "Habits", "${vm.habits.size} habits, manage and edit", onOpenHabits)
        Spacer(Modifier.height(10.dp))
        SettingsRow(Ic.TIMER, "Focus settings", "Timer ${vm.focusLen} / ${vm.shortLen} / ${vm.longLen} min") { focusDialog = true }
        Spacer(Modifier.height(10.dp))
        SettingsRow(Ic.PLAY, "Open focus timer", "Deep work with breaks", onOpenFocus)

        Spacer(Modifier.height(22.dp))
        Txt("Data and backup", size = 16.sp, weight = FontWeight.Bold, serif = true)
        Spacer(Modifier.height(10.dp))
        SettingsRow(Ic.DOC, "Export backup", "Save everything to a file") { backup.launch("forge-backup.json") }
        Spacer(Modifier.height(10.dp))
        SettingsRow(Ic.CLOUD, "Restore backup", "Load a file saved earlier") { confirmRestore = true }
        Spacer(Modifier.height(10.dp))
        SettingsRow(Ic.TRASH, "Clear completed tasks", "$totalDone completed") { confirmClear = true }

        Spacer(Modifier.height(24.dp))
        Txt(
            "forge 4.0  •  Everything is stored on this phone only.",
            Modifier.fillMaxWidth(),
            size = 12.sp,
            color = p.muted
        )
    }

    if (goalDialog) {
        var goal by remember { mutableIntStateOf(vm.dailyGoal) }
        AlertDialog(
            onDismissRequest = { goalDialog = false },
            title = { Text("Daily goal") },
            text = {
                LenRow(
                    "Per day",
                    goal,
                    { goal = (goal - 1).coerceAtLeast(1) },
                    { goal = (goal + 1).coerceAtMost(30) },
                    unit = "items"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateDailyGoal(goal)
                    goalDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { goalDialog = false }) { Text("Cancel") } }
        )
    }
    if (focusDialog) FocusLengthsDialog(vm) { focusDialog = false }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore backup?") },
            text = { Text("This replaces everything currently on this phone with the contents of the file.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restore.launch(arrayOf("application/json", "text/plain", "*/*"))
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear completed tasks?") },
            text = { Text("Finished tasks will be removed. Habits are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearCompleted()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.cardStyle(p, 20.dp).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt(value, size = 24.sp, weight = FontWeight.Bold, serif = true, color = p.accent)
        Txt(label, size = 11.sp, color = p.muted)
    }
}

@Composable
private fun ModeTile(label: String, ic: Ic, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) p.accent.copy(alpha = 0.12f) else p.card)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) p.accent else p.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(ic, if (selected) p.accent else p.muted, size = 26.dp)
        Spacer(Modifier.height(6.dp))
        Txt(label, size = 13.sp, weight = FontWeight.SemiBold, color = if (selected) p.accentText else p.text)
    }
}

@Composable
private fun SettingsRow(ic: Ic, title: String, subtitle: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().cardStyle(p, 20.dp).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(p.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { AppIcon(ic, p.accent, size = 22.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, size = 16.sp, weight = FontWeight.SemiBold)
            Txt(subtitle, size = 12.sp, color = p.muted, maxLines = 1)
        }
        AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 20.dp)
    }
}
