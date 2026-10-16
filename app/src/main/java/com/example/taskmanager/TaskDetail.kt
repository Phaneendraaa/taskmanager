package com.example.taskmanager

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun TaskDetail(
    vm: AppViewModel,
    taskId: String,
    today: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val task = vm.taskById(taskId)
    var menuOpen by remember { mutableStateOf(false) }
    var addingSub by remember { mutableStateOf(false) }
    var newSub by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.addAttachment(taskId, readAttachment(context, uri))
    }

    // The task can only disappear through delete, which already closes this screen.
    if (task == null) return

    val subDone = task.subtasks.count { it.done }
    val dayText = task.dueDay.let { if (it == null) "No date" else fullDate(it) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Ic.BACK, onClick = onBack)
            Spacer(Modifier.weight(1f))
            RoundIconButton(Ic.EDIT, onClick = onEdit)
            Spacer(Modifier.width(10.dp))
            Box {
                RoundIconButton(Ic.MORE_V, onClick = { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = {
                        menuOpen = false
                        vm.duplicateTask(task.id)
                        onBack()
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        menuOpen = false
                        onDelete()
                    })
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                CheckCircle(task.done, onClick = { vm.toggleTask(task.id) }, diameter = 30.dp)
                Spacer(Modifier.width(6.dp))
                Txt(
                    task.title,
                    size = 26.sp,
                    weight = FontWeight.ExtraBold,
                    strike = task.done,
                    color = if (task.done) p.muted else p.text,
                    lineHeight = 32.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp)
                )
                if (task.priority == 3) {
                    Spacer(Modifier.width(8.dp))
                    AppIcon(Ic.FLAG, StatusRed, size = 24.dp, modifier = Modifier.padding(top = 10.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.padding(start = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoChip(task.category.label, task.category.color, task.category.icon)
                if (task.priority > 0) {
                    val (label, color) = when (task.priority) {
                        3 -> Pair("High Priority", StatusRed)
                        2 -> Pair("Medium Priority", Amber)
                        else -> Pair("Low Priority", p.muted)
                    }
                    InfoChip(label, color, Ic.FLAG)
                }
            }

            Spacer(Modifier.height(24.dp))
            InfoRow(Ic.CALENDAR, dayText)
            InfoRow(Ic.CLOCK, timeRange(task.dueMinute, task.endMinute))
            InfoRow(Ic.REPEAT, repeatText(task.repeat))

            if (task.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.field)
                        .padding(16.dp)
                ) {
                    Txt(task.notes, size = 15.sp, lineHeight = 22.sp)
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt(
                    "Sub-tasks (" + subDone + "/" + task.subtasks.size + ")",
                    size = 17.sp,
                    weight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { addingSub = !addingSub },
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(Ic.PLUS, p.text, size = 22.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
            task.subtasks.forEach { s ->
                SubTaskRow(
                    sub = s,
                    onToggle = { vm.toggleSubTask(task.id, s.id) },
                    onRemove = { vm.removeSubTask(task.id, s.id) }
                )
                Hairline()
            }
            if (addingSub || task.subtasks.isEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        PlainField(
                            value = newSub,
                            onValueChange = { newSub = it },
                            placeholder = "Add a sub-task",
                            singleLine = true,
                            size = 15.sp,
                            imeAction = ImeAction.Done,
                            onDone = {
                                vm.addSubTask(task.id, newSub)
                                newSub = ""
                            }
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(p.accent.copy(alpha = 0.14f))
                            .clickable {
                                vm.addSubTask(task.id, newSub)
                                newSub = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(Ic.PLUS, p.accentText, size = 20.dp, stroke = 2.2f)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(Ic.CLIP, p.text, size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Txt(
                    "Attachments (" + task.attachments.size + ")",
                    size = 17.sp,
                    weight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Txt(
                    "+ Add",
                    size = 14.sp,
                    weight = FontWeight.Medium,
                    color = p.accentText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { filePicker.launch(arrayOf("*/*")) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            if (task.attachments.isEmpty()) {
                Txt("No files attached.", size = 14.sp, color = p.muted)
            }
            task.attachments.forEach { a ->
                AttachmentRow(
                    att = a,
                    onOpen = { openAttachment(context, a) },
                    onRemove = { vm.removeAttachment(task.id, a.uri) }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))
            Hairline()
            Spacer(Modifier.height(14.dp))
            Txt("Created " + stampOf(task.createdAt), size = 12.sp, color = p.muted)
            val completed = task.completedAt
            if (completed != null) {
                Spacer(Modifier.height(4.dp))
                Txt("Completed " + stampOf(completed), size = 12.sp, color = p.muted)
            }
            Spacer(Modifier.height(32.dp))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val shape = RoundedCornerShape(50)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(shape)
                    .background(p.inverse)
                    .clickable { vm.toggleTask(task.id) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(Ic.CHECK, p.onInverse, size = 20.dp, stroke = 2.4f)
                Spacer(Modifier.width(10.dp))
                Txt(
                    if (task.done) "Mark as Incomplete" else "Mark as Complete",
                    size = 17.sp,
                    weight = FontWeight.SemiBold,
                    color = p.onInverse
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, color: Color, ic: Ic) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(ic, color, size = 16.dp, filled = true)
        Spacer(Modifier.width(6.dp))
        Txt(label, size = 13.sp, weight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun InfoRow(ic: Ic, text: String) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(ic, p.muted, size = 22.dp)
        Spacer(Modifier.width(16.dp))
        Txt(text, size = 16.sp, weight = FontWeight.Medium)
    }
}

@Composable
private fun SubTaskRow(sub: SubTask, onToggle: () -> Unit, onRemove: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(sub.done, onToggle, diameter = 22.dp)
        Spacer(Modifier.width(4.dp))
        Txt(
            sub.title,
            size = 15.sp,
            color = if (sub.done) p.muted else p.text,
            strike = sub.done,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(Ic.CLOSE, p.muted, size = 16.dp)
        }
    }
}
