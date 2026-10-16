package com.example.taskmanager

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val MODE_NAMES = listOf("Focus", "Short Break", "Long Break")

private fun clock(ms: Long): String {
    val total = ((ms + 999) / 1000).toInt()
    val m = total / 60
    val s = total % 60
    return (if (m < 10) "0$m" else "$m") + ":" + (if (s < 10) "0$s" else "$s")
}

private fun beep() {
    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 600)
    } catch (e: Exception) {
        // no sound available; ignore
    }
}

@Composable
fun FocusScreen(vm: AppViewModel, onClose: (() -> Unit)?) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pickTask by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }

    // Keep the screen awake while the timer page is open.
    val view = LocalView.current
    DisposableEffect(vm.timerRunning) {
        view.keepScreenOn = vm.timerRunning
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(vm.timerRunning) {
        while (vm.timerRunning) {
            nowMs = System.currentTimeMillis()
            if (vm.completeIfDone()) beep()
            delay(200)
        }
        nowMs = System.currentTimeMillis()
    }

    val remaining = vm.timerRemainingMs(nowMs)
    val total = (vm.lenOf(vm.focusMode) * 60_000L).coerceAtLeast(1L)
    val elapsed = 1f - remaining.toFloat() / total.toFloat()
    val isFocus = vm.focusMode == 0

    val todaySessions = vm.focusSessions.filter { epochDayOf(it.endedAt) == today }
    val todayMinutes = todaySessions.sumOf { it.minutes }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClose != null) {
                RoundIconButton(Ic.CLOSE, onClick = onClose, bordered = false, bg = Color.Transparent)
            }
            Txt("Focus", size = 28.sp, weight = FontWeight.SemiBold, serif = true, modifier = Modifier.weight(1f))
            RoundIconButton(Ic.SLIDERS, onClick = { settings = true }, bordered = false, bg = Color.Transparent)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SegmentedControl(MODE_NAMES, vm.focusMode, { vm.selectMode(it) })

            Spacer(Modifier.height(28.dp))
            Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
                ProgressRing(elapsed, Modifier.fillMaxSize(), thickness = 8.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt(clock(remaining), size = 54.sp, weight = FontWeight.Medium, serif = true)
                    Txt(
                        if (isFocus) (vm.focusLabel.ifBlank { "Deep work" }) else "Rest",
                        size = 15.sp,
                        color = p.muted,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 36.dp),
                        align = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(p.field).clickable { vm.resetTimer() },
                    contentAlignment = Alignment.Center
                ) { AppIcon(Ic.STOP, p.text, size = 20.dp, filled = true) }
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(p.inverse)
                        .clickable { if (vm.timerRunning) vm.pauseTimer() else vm.startTimer() },
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        if (vm.timerRunning) Ic.PAUSE else Ic.PLAY,
                        p.onInverse,
                        size = 32.dp,
                        filled = true
                    )
                }
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(p.field)
                        .clickable { vm.selectMode(if (vm.focusMode == 0) 1 else 0) },
                    contentAlignment = Alignment.Center
                ) { AppIcon(Ic.ARROW_RIGHT, p.text, size = 22.dp) }
            }

            Spacer(Modifier.height(28.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .cardStyle(p)
                    .clickable { pickTask = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(p.field),
                    contentAlignment = Alignment.Center
                ) { AppIcon(Ic.TARGET, p.muted, size = 20.dp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Txt("Working on", size = 12.sp, color = p.muted)
                    Txt(
                        vm.focusLabel.ifBlank { "Choose from today's priorities" },
                        size = 15.sp,
                        weight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 20.dp)
            }

            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Txt("Deep work today", size = 12.sp, color = p.muted)
                        Txt(
                            "$todayMinutes of ${vm.focusTarget} min",
                            size = 16.sp,
                            weight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Txt(
                        if (todaySessions.size == 1) "1 session" else "${todaySessions.size} sessions",
                        size = 13.sp,
                        color = p.muted,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(12.dp))
                GradientProgress(todayMinutes.toFloat() / vm.focusTarget.coerceAtLeast(1), Modifier.fillMaxWidth(), 6.dp)
                if (todayMinutes >= vm.focusTarget) {
                    Spacer(Modifier.height(10.dp))
                    Txt("Target reached. Anything more is a bonus.", size = 12.sp, color = p.accentText)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (pickTask) {
        val options = ArrayList<String>()
        vm.prioritiesFor(today).forEach { id -> vm.taskById(id)?.let { if (!it.done) options.add(it.title) } }
        vm.habits.filter { it.isDue(today) && today !in it.doneDays }.forEach { options.add(it.name) }
        vm.tasks.filter { !it.done }.forEach { options.add(it.title) }
        AlertDialog(
            onDismissRequest = { pickTask = false },
            title = { Text("Working on") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DialogRow("No task", vm.focusLabel.isBlank()) {
                        vm.focusLabel = ""
                        pickTask = false
                    }
                    options.distinct().forEach { name ->
                        DialogRow(name, vm.focusLabel == name) {
                            vm.focusLabel = name
                            pickTask = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickTask = false }) { Text("Close") } }
        )
    }

    if (settings) FocusLengthsDialog(vm) { settings = false }
}

@Composable
private fun DialogRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(
            text,
            size = 15.sp,
            weight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) p.accentText else p.text,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (selected) AppIcon(Ic.CHECK, p.accent, size = 18.dp, stroke = 2.4f)
    }
}

@Composable
fun LenRow(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, unit: String = "min") {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Txt(label, size = 15.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(p.field).clickable(onClick = onMinus),
            contentAlignment = Alignment.Center
        ) { Txt("-", size = 20.sp, weight = FontWeight.Bold, color = p.accent) }
        Txt("$value $unit", Modifier.width(70.dp), size = 15.sp, weight = FontWeight.SemiBold, align = TextAlign.Center)
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(p.field).clickable(onClick = onPlus),
            contentAlignment = Alignment.Center
        ) { Txt("+", size = 20.sp, weight = FontWeight.Bold, color = p.accent) }
    }
}

@Composable
fun FocusLengthsDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var f by remember { mutableLongStateOf(vm.focusLen.toLong()) }
    var s by remember { mutableLongStateOf(vm.shortLen.toLong()) }
    var l by remember { mutableLongStateOf(vm.longLen.toLong()) }
    var t by remember { mutableLongStateOf(vm.focusTarget.toLong()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer lengths") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LenRow("Focus", f.toInt(), { f = (f - 5).coerceAtLeast(5) }, { f = (f + 5).coerceAtMost(90) })
                LenRow("Short break", s.toInt(), { s = (s - 1).coerceAtLeast(1) }, { s = (s + 1).coerceAtMost(30) })
                LenRow("Long break", l.toInt(), { l = (l - 5).coerceAtLeast(5) }, { l = (l + 5).coerceAtMost(60) })
                LenRow("Daily target", t.toInt(), { t = (t - 15).coerceAtLeast(15) }, { t = (t + 15).coerceAtMost(480) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.updateFocusLengths(f.toInt(), s.toInt(), l.toInt())
                vm.updateFocusTarget(t.toInt())
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
