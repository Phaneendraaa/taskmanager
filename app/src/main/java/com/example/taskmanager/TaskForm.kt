package com.example.taskmanager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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

/** Create / edit screen. Pass [existing] to edit. */
@Composable
fun TaskForm(
    vm: AppViewModel,
    existing: Task?,
    prefillDay: Long?,
    today: Long,
    onFinish: () -> Unit,
    onSwitchToHabit: (() -> Unit)? = null
) {
    val p = LocalPalette.current
    val context = LocalContext.current

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var dueDay by remember { mutableStateOf<Long?>(if (existing != null) existing.dueDay else (prefillDay ?: today)) }
    var startMin by remember { mutableStateOf<Int?>(existing?.dueMinute) }
    var endMin by remember { mutableStateOf<Int?>(existing?.endMinute) }
    var repeatMode by remember { mutableStateOf(existing?.repeat ?: Repeat.NONE) }
    var category by remember { mutableStateOf(existing?.category ?: Category.WORK) }
    var priority by remember { mutableIntStateOf(if (existing != null && existing.priority > 0) existing.priority else 2) }
    val subtasks = remember { mutableStateListOf<SubTask>().apply { addAll(existing?.subtasks ?: emptyList()) } }
    val attachments = remember { mutableStateListOf<Attachment>().apply { addAll(existing?.attachments ?: emptyList()) } }
    var newSub by remember { mutableStateOf("") }

    var dateMenu by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }
    var timeMenu by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val att = readAttachment(context, uri)
            if (attachments.none { it.uri == att.uri }) attachments.add(att)
        }
    }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spoken != null) {
                notes = if (notes.isBlank()) spoken else notes.trimEnd() + " " + spoken
            }
        }
    }

    fun addPendingSubtask() {
        val s = newSub.trim()
        if (s.isNotEmpty()) {
            subtasks.add(SubTask(vm.newId(), s))
            newSub = ""
        }
    }

    fun save() {
        val t = title.trim()
        if (t.isEmpty()) return
        addPendingSubtask()
        var day = dueDay
        if (repeatMode != Repeat.NONE && day == null) day = today
        val start = startMin
        val base = existing ?: Task(id = vm.newId(), title = t)
        vm.upsertTask(
            base.copy(
                title = t,
                notes = notes.trim(),
                dueDay = day,
                dueMinute = if (day == null) null else start,
                endMinute = if (day == null || start == null) null else endMin,
                repeat = repeatMode,
                category = category,
                priority = priority,
                subtasks = subtasks.toList(),
                attachments = attachments.toList()
            )
        )
        onFinish()
    }

    val dueLabel = dueDay.let { if (it == null) "No date" else shortDayLabel(it, today) }

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
            RoundIconButton(Ic.CLOSE, onClick = onFinish)
            Spacer(Modifier.weight(1f))
            GradientButton(
                text = "Save",
                onClick = { save() },
                enabled = title.isNotBlank(),
                height = 44.dp,
                textSize = 16.sp
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (existing == null && onSwitchToHabit != null) {
                SegmentedControl(listOf("Habit", "Task"), 1, { if (it == 0) onSwitchToHabit() })
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Txt(
                if (existing == null) "Create a New Task" else "Edit Task",
                size = 30.sp,
                weight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Txt(
                if (existing == null) "Turn your plans into progress." else "Make it exactly right.",
                size = 15.sp,
                color = p.muted
            )
            Spacer(Modifier.height(22.dp))

            FormCard {
                AppIcon(Ic.EDIT, p.muted, size = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Txt("Task title", size = 13.sp, color = p.muted)
                    Spacer(Modifier.height(4.dp))
                    PlainField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "E.g. Complete project proposal",
                        size = 16.sp,
                        weight = FontWeight.Medium,
                        autoFocus = existing == null
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FormCard {
                AppIcon(Ic.NOTES, p.muted, size = 22.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Txt("Notes (optional)", size = 13.sp, color = p.muted)
                    Spacer(Modifier.height(4.dp))
                    PlainField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = "Add details, links, or ideas...",
                        size = 15.sp
                    )
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note")
                            }
                            try {
                                voice.launch(intent)
                            } catch (e: Exception) {
                                Toast
                                    .makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(Ic.MIC, p.accentText, size = 22.dp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionTile(Ic.CALENDAR, "Date", Modifier.weight(1f)) {
                    Box {
                        SelectPill(dueLabel, onClick = { dateMenu = true })
                        DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                            DropdownMenuItem(text = { Text("Today") }, onClick = {
                                dueDay = today
                                dateMenu = false
                            })
                            DropdownMenuItem(text = { Text("Tomorrow") }, onClick = {
                                dueDay = today + 1
                                dateMenu = false
                            })
                            DropdownMenuItem(text = { Text("Next week") }, onClick = {
                                dueDay = today + 7
                                dateMenu = false
                            })
                            DropdownMenuItem(text = { Text("Pick a date…") }, onClick = {
                                dateMenu = false
                                showDate = true
                            })
                            DropdownMenuItem(text = { Text("No date") }, onClick = {
                                dueDay = null
                                startMin = null
                                endMin = null
                                dateMenu = false
                            })
                        }
                    }
                }
                OptionTile(Ic.CLOCK, "Time", Modifier.weight(1f)) {
                    val sm = startMin
                    Box {
                        SelectPill(if (sm != null) formatMinute(sm) else "Any time", onClick = { timeMenu = true })
                        DropdownMenu(expanded = timeMenu, onDismissRequest = { timeMenu = false }) {
                            DropdownMenuItem(text = { Text("Pick a time…") }, onClick = {
                                timeMenu = false
                                if (dueDay == null) dueDay = today
                                showStart = true
                            })
                            DropdownMenuItem(text = { Text("Any time") }, onClick = {
                                startMin = null
                                endMin = null
                                timeMenu = false
                            })
                        }
                    }
                }
                OptionTile(Ic.REPEAT, "Repeat", Modifier.weight(1f)) {
                    Box {
                        SelectPill(repeatMode.label, onClick = { repeatMenu = true })
                        DropdownMenu(expanded = repeatMenu, onDismissRequest = { repeatMenu = false }) {
                            Repeat.values().forEach { r ->
                                DropdownMenuItem(text = { Text(r.label) }, onClick = {
                                    repeatMode = r
                                    if (r != Repeat.NONE && dueDay == null) dueDay = today
                                    repeatMenu = false
                                })
                            }
                        }
                    }
                }
            }

            val sm = startMin
            if (sm != null) {
                Spacer(Modifier.height(12.dp))
                val em = endMin
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Txt("End time", size = 14.sp, color = p.muted, modifier = Modifier.weight(1f))
                    if (em != null) {
                        Txt(formatMinute(em), size = 14.sp, weight = FontWeight.SemiBold)
                        Spacer(Modifier.width(14.dp))
                        Txt(
                            "Remove",
                            size = 14.sp,
                            color = p.accentText,
                            modifier = Modifier.clickable { endMin = null }
                        )
                    } else {
                        Txt(
                            "+ Add end time",
                            size = 14.sp,
                            weight = FontWeight.Medium,
                            color = p.accentText,
                            modifier = Modifier.clickable { showEnd = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Txt("Category", size = 17.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Category.values().forEach { c ->
                    val on = c == category
                    val shape = RoundedCornerShape(18.dp)
                    Column(
                        Modifier
                            .width(72.dp)
                            .clip(shape)
                            .background(if (on) p.inverse else p.field)
                            .border(1.dp, if (on) p.inverse else p.cardBorder, shape)
                            .clickable { category = c }
                            .padding(vertical = 13.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppIcon(c.icon, if (on) p.onInverse else p.muted, size = 24.dp)
                        Spacer(Modifier.height(7.dp))
                        Txt(c.label, size = 12.sp, weight = FontWeight.Medium, color = if (on) p.onInverse else p.text, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Txt("Priority", size = 17.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PriorityOption("High", 3, priority, StatusRed, Modifier.weight(1f)) { priority = 3 }
                PriorityOption("Medium", 2, priority, Amber, Modifier.weight(1f)) { priority = 2 }
                PriorityOption("Low", 1, priority, p.muted, Modifier.weight(1f)) { priority = 1 }
            }

            Spacer(Modifier.height(26.dp))
            Txt("Sub-tasks", size = 17.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            subtasks.forEach { s ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(p.accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Txt(s.title, size = 15.sp, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { subtasks.remove(s) },
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(Ic.CLOSE, p.muted, size = 18.dp)
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
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
                        onDone = { addPendingSubtask() }
                    )
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(p.accent.copy(alpha = 0.14f))
                        .clickable { addPendingSubtask() },
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(Ic.PLUS, p.accentText, size = 20.dp, stroke = 2.2f)
                }
            }

            Spacer(Modifier.height(26.dp))
            Txt("Attachments", size = 17.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            attachments.forEach { a ->
                AttachmentRow(
                    att = a,
                    onOpen = { openAttachment(context, a) },
                    onRemove = { attachments.remove(a) }
                )
                Spacer(Modifier.height(8.dp))
            }
            val pillShape = RoundedCornerShape(50)
            Row(
                Modifier
                    .clip(pillShape)
                    .border(1.dp, p.muted.copy(alpha = 0.45f), pillShape)
                    .clickable { filePicker.launch(arrayOf("*/*")) }
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(Ic.CLIP, p.text, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Txt("Add attachment", size = 14.sp, weight = FontWeight.Medium)
            }
            Spacer(Modifier.height(40.dp))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            GradientButton(
                text = if (existing == null) "Add Task" else "Save Changes",
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
                icon = Ic.ARROW_RIGHT,
                enabled = title.isNotBlank()
            )
        }
    }

    if (showDate) {
        DatePickDialog(
            initialDay = dueDay,
            onPick = {
                dueDay = it
                showDate = false
            },
            onDismiss = { showDate = false }
        )
    }
    if (showStart) {
        TimePickDialog(
            initialMinute = startMin,
            onPick = {
                startMin = it
                val e = endMin
                if (e != null && e <= it) endMin = null
                showStart = false
            },
            onDismiss = { showStart = false }
        )
    }
    if (showEnd) {
        TimePickDialog(
            initialMinute = endMin ?: ((startMin ?: 540) + 60).coerceAtMost(23 * 60 + 59),
            onPick = {
                endMin = it
                showEnd = false
            },
            onDismiss = { showEnd = false }
        )
    }
}

@Composable
private fun FormCard(content: @Composable RowScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(p.field)
            .border(1.dp, p.cardBorder, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun OptionTile(ic: Ic, label: String, modifier: Modifier, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(p.field)
            .border(1.dp, p.cardBorder, shape)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(ic, p.muted, size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Txt(label, size = 13.sp, color = p.muted)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun PriorityOption(
    label: String,
    value: Int,
    selected: Int,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    val on = value == selected
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .clip(shape)
            .background(if (on) color.copy(alpha = 0.16f) else p.field)
            .border(1.5.dp, if (on) color else p.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(Ic.FLAG, color, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Txt(label, size = 14.sp, weight = FontWeight.Medium)
    }
}

@Composable
fun AttachmentRow(att: Attachment, onOpen: () -> Unit, onRemove: (() -> Unit)?) {
    val p = LocalPalette.current
    val isPdf = fileKind(att) == "PDF"
    val tileColor = if (isPdf) StatusRed else p.accent
    val sizeText = formatSize(att.size)
    Row(
        Modifier
            .fillMaxWidth()
            .cardStyle(p, 16.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tileColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(Ic.DOC, tileColor, size = 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(att.name, size = 14.sp, weight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Txt(
                if (sizeText.isEmpty()) fileKind(att) else sizeText + "  •  " + fileKind(att),
                size = 12.sp,
                color = p.muted
            )
        }
        if (onRemove != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(Ic.CLOSE, p.muted, size = 18.dp)
            }
        }
    }
}
