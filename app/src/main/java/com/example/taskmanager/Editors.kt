@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.taskmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TaskEditor(
    existing: Task?,
    today: Long,
    onSave: (String, String, Long?, Int?, Int) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var dueDay by remember { mutableStateOf<Long?>(existing?.dueDay) }
    var dueMinute by remember { mutableStateOf<Long?>(existing?.dueMinute?.toLong()) }
    var priority by remember { mutableIntStateOf(existing?.priority ?: 0) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        EditorTopBar(
            label = if (existing == null) "New task" else "Edit task",
            canSave = title.isNotBlank(),
            onCancel = onClose,
            onSave = {
                val day = dueDay
                val minute = dueMinute
                onSave(
                    title.trim(),
                    notes.trim(),
                    day,
                    if (day != null && minute != null) minute.toInt() else null,
                    priority
                )
            }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            PlainField(
                value = title,
                onValueChange = { title = it },
                placeholder = "What needs doing?",
                textStyle = MaterialTheme.typography.headlineMedium,
                autoFocus = existing == null
            )
            Spacer(Modifier.height(12.dp))
            PlainField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Add notes",
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(32.dp))
            Hairline()
            Spacer(Modifier.height(24.dp))

            SmallCaps("Done by")
            Spacer(Modifier.height(12.dp))
            val d = dueDay
            val custom = d != null && d != today && d != today + 1 && d != today + 7
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("Today", d == today, { dueDay = today })
                Chip("Tomorrow", d == today + 1, { dueDay = today + 1 })
                Chip("In a week", d == today + 7, { dueDay = today + 7 })
                Chip(if (custom && d != null) formatDay(d, today) else "Pick date", custom, { showDate = true })
                Chip("No date", d == null, {
                    dueDay = null
                    dueMinute = null
                })
            }

            if (d != null) {
                Spacer(Modifier.height(28.dp))
                SmallCaps("Time")
                Spacer(Modifier.height(12.dp))
                val m = dueMinute
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip(if (m != null) formatMinute(m.toInt()) else "Add time", m != null, { showTime = true })
                    if (m != null) {
                        Chip("All day", false, { dueMinute = null })
                    }
                }
                Spacer(Modifier.height(14.dp))
                val summary = formatDay(d, today) + (if (m != null) ", " + formatMinute(m.toInt()) else "")
                Text(
                    "Due $summary",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted
                )
            }

            Spacer(Modifier.height(28.dp))
            SmallCaps("Priority")
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("None", "Low", "Medium", "High").forEachIndexed { i, label ->
                    Chip(label, priority == i, { priority = i })
                }
            }

            if (existing != null) {
                Spacer(Modifier.height(36.dp))
                Hairline()
                Spacer(Modifier.height(16.dp))
                Text("Created " + stampOf(existing.createdAt), style = MaterialTheme.typography.bodySmall, color = muted)
                val completed = existing.completedAt
                if (completed != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Completed " + stampOf(completed), style = MaterialTheme.typography.bodySmall, color = muted)
                }
            }

            if (onDelete != null) {
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDelete) {
                    Text("Delete task", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(64.dp))
        }
    }

    if (showDate) {
        val startMillis = dueDay?.let {
            LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startMillis)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = pickerState.selectedDateMillis
                    if (ms != null) {
                        dueDay = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTime) {
        val initial = (dueMinute ?: 540L).toInt()
        val timeState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    dueMinute = (timeState.hour * 60 + timeState.minute).toLong()
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
fun HabitEditor(
    existing: Habit?,
    today: Long,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        EditorTopBar(
            label = if (existing == null) "New habit" else "Edit habit",
            canSave = name.isNotBlank(),
            onCancel = onClose,
            onSave = { onSave(name.trim()) }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            PlainField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Name a daily habit",
                textStyle = MaterialTheme.typography.headlineMedium,
                autoFocus = existing == null,
                singleLine = true,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                onDone = { if (name.isNotBlank()) onSave(name.trim()) }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Shown every day. Tap the circle to check it off.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )

            if (existing != null) {
                Spacer(Modifier.height(32.dp))
                Hairline()
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth()) {
                    Stat(existing.currentStreak(today).toString(), "Current streak", Modifier.weight(1f))
                    Stat(existing.bestStreak().toString(), "Best streak", Modifier.weight(1f))
                }
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth()) {
                    Stat(existing.rate(today).toString() + "%", "Last 30 days", Modifier.weight(1f))
                    Stat(existing.doneDays.size.toString(), "Total days", Modifier.weight(1f))
                }
                Spacer(Modifier.height(32.dp))
                SmallCaps("Last 16 weeks")
                Spacer(Modifier.height(14.dp))
                Heatmap(existing.doneDays, today)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Started " + LocalDate.ofEpochDay(existing.createdDay).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }

            if (onDelete != null) {
                Spacer(Modifier.height(28.dp))
                TextButton(onClick = onDelete) {
                    Text("Delete habit", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
private fun EditorTopBar(label: String, canSave: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SmallCaps(label)
        TextButton(onClick = onSave, enabled = canSave) {
            Text("Save", fontWeight = FontWeight.Bold)
        }
    }
}
