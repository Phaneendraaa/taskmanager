package com.example.taskmanager

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.time.LocalDate

/** Editable copy of a habit, shared by the Add screen and the Edit tab of Habit Details. */
class HabitDraft(h: Habit?) {
    var name by mutableStateOf(h?.name ?: "")
    var category by mutableStateOf(h?.category ?: HabitCategory.HEALTH)
    var mask by mutableIntStateOf(h?.daysMask ?: 127)
    var minute by mutableStateOf<Int?>(h?.timeMinute)
    var icon by mutableStateOf(h?.icon ?: "")

    fun applyTo(base: Habit): Habit = base.copy(
        name = name.trim(),
        category = category,
        daysMask = mask,
        timeMinute = minute,
        icon = icon
    )
}

private val ICON_CHOICES = listOf(
    Ic.SUN, Ic.DROP, Ic.DUMBBELL, Ic.BOOK, Ic.LEAF, Ic.MOON, Ic.HEART, Ic.FOOD,
    Ic.LAPTOP, Ic.TIMER, Ic.FLAME, Ic.STAR, Ic.TARGET, Ic.MUSIC, Ic.PLANE
)

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun SmallLabel(text: String) {
    Txt(text, size = 13.sp, weight = FontWeight.SemiBold, color = LocalPalette.current.muted)
}

@Composable
fun HabitFields(d: HabitDraft, onMic: (() -> Unit)? = null) {
    val p = LocalPalette.current
    var showTime by remember { mutableStateOf(false) }
    val frequency = when (d.mask) {
        127 -> 0
        31 -> 1
        else -> 2
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.field)
            .border(1.dp, p.cardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(Ic.LEAF, p.accent, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            PlainField(
                value = d.name,
                onValueChange = { d.name = it.take(40) },
                placeholder = "What's the habit?",
                size = 16.sp,
                singleLine = true
            )
        }
        if (onMic != null) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onMic),
                contentAlignment = Alignment.Center
            ) { AppIcon(Ic.MIC, p.muted, size = 20.dp) }
        }
    }

    Spacer(Modifier.height(20.dp))
    SmallLabel("Category")
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HabitCategory.values().forEach { c ->
            CategoryChip(c.label, c.icon, c.color, d.category == c) { d.category = c }
        }
    }

    Spacer(Modifier.height(20.dp))
    SmallLabel("Frequency")
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Daily", frequency == 0) { d.mask = 127 }
        Chip("Weekdays", frequency == 1) { d.mask = 31 }
        Chip("Custom", frequency == 2) { if (frequency != 2) d.mask = 21 }
    }
    if (frequency == 2) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..6) {
                val on = (d.mask shr i) and 1 == 1
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (on) p.accent else p.field)
                        .clickable {
                            val next = d.mask xor (1 shl i)
                            if (next != 0) d.mask = next
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Txt(
                        DAY_LETTERS[i],
                        size = 14.sp,
                        weight = FontWeight.SemiBold,
                        color = if (on) Color.White else p.text
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    SmallLabel("Time (optional)")
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.field)
            .border(1.dp, p.cardBorder, RoundedCornerShape(18.dp))
            .clickable { showTime = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(Ic.CLOCK, p.accent, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        val m = d.minute
        Txt(
            if (m == null) "Any time of day" else formatMinute(m),
            size = 16.sp,
            weight = FontWeight.Medium,
            color = if (m == null) p.muted else p.text,
            modifier = Modifier.weight(1f)
        )
        if (m != null) {
            Txt(
                "Clear",
                Modifier.clip(RoundedCornerShape(10.dp)).clickable { d.minute = null }.padding(6.dp),
                size = 13.sp,
                weight = FontWeight.SemiBold,
                color = p.accentText
            )
        } else {
            AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 18.dp)
        }
    }

    Spacer(Modifier.height(20.dp))
    SmallLabel("Icon")
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ICON_CHOICES.forEach { ic ->
            val on = d.icon == ic.name
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) d.category.color.copy(alpha = 0.2f) else p.field)
                    .border(
                        if (on) 1.5.dp else 1.dp,
                        if (on) d.category.color else p.cardBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { d.icon = if (on) "" else ic.name },
                contentAlignment = Alignment.Center
            ) { AppIcon(ic, if (on) d.category.color else p.muted, size = 22.dp) }
        }
    }

    if (showTime) {
        TimePickDialog(
            initialMinute = d.minute,
            onPick = {
                d.minute = it
                showTime = false
            },
            onDismiss = { showTime = false }
        )
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) p.accent.copy(alpha = 0.18f) else p.field)
            .border(1.dp, if (selected) p.accent else p.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Txt(
            label,
            size = 14.sp,
            weight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) p.accentText else p.text
        )
    }
}

private class Quick(val name: String, val cat: HabitCategory, val ic: Ic, val minute: Int?)

private val QUICK = listOf(
    Quick("Drink water", HabitCategory.HEALTH, Ic.DROP, null),
    Quick("Go to gym", HabitCategory.HEALTH, Ic.DUMBBELL, 18 * 60),
    Quick("Read book", HabitCategory.LEARNING, Ic.BOOK, 21 * 60),
    Quick("Complete project", HabitCategory.PRODUCTIVITY, Ic.TARGET, 10 * 60),
    Quick("Meditate", HabitCategory.PERSONAL, Ic.LEAF, 7 * 60),
    Quick("Stretch", HabitCategory.HEALTH, Ic.HEART, 8 * 60)
)

/** Add Habit screen with a Habit / Task switch and a Quick Add section. */
@Composable
fun HabitForm(
    vm: AppViewModel,
    today: Long,
    onFinish: () -> Unit,
    onSwitchToTask: () -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val draft = remember { HabitDraft(null) }
    var quickText by remember { mutableStateOf("") }
    var micTarget by remember { mutableIntStateOf(0) }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                if (micTarget == 0) draft.name = spoken.take(40) else quickText = spoken.take(40)
            }
        }
    }

    fun startVoice(target: Int) {
        micTarget = target
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your habit")
            }
            voice.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun create() {
        val n = draft.name.trim()
        if (n.isEmpty()) return
        vm.upsertHabit(draft.applyTo(Habit(id = vm.newId(), name = n, createdDay = today)))
        onFinish()
    }

    fun quickAdd(name: String, cat: HabitCategory, ic: Ic?, minute: Int?) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        vm.upsertHabit(
            Habit(
                id = vm.newId(),
                name = clean,
                createdDay = today,
                category = cat,
                timeMinute = minute,
                icon = ic?.name ?: ""
            )
        )
    }

    val suggestions = QUICK.filter { q -> vm.habits.none { it.name.equals(q.name, ignoreCase = true) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Ic.CLOSE, onClick = onFinish)
            Spacer(Modifier.weight(1f))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SegmentedControl(listOf("Habit", "Task"), 0, { if (it == 1) onSwitchToTask() })
            Spacer(Modifier.height(20.dp))
            Txt("Add habit", size = 30.sp, weight = FontWeight.Bold, serif = true)
            Spacer(Modifier.height(4.dp))
            Txt("Small actions, repeated every day.", size = 15.sp, color = p.muted)
            Spacer(Modifier.height(20.dp))

            HabitFields(draft, onMic = { startVoice(0) })

            Spacer(Modifier.height(28.dp))
            SmallLabel("Quick add")
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().cardStyle(p, 20.dp).padding(12.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.field)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        PlainField(
                            value = quickText,
                            onValueChange = { quickText = it.take(40) },
                            placeholder = "I want to...",
                            singleLine = true,
                            imeAction = ImeAction.Done,
                            onDone = {
                                quickAdd(quickText, HabitCategory.PERSONAL, null, null)
                                quickText = ""
                            }
                        )
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).clickable { startVoice(1) },
                        contentAlignment = Alignment.Center
                    ) { AppIcon(Ic.MIC, p.muted, size = 20.dp) }
                }
                suggestions.forEach { q ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { quickAdd(q.name, q.cat, q.ic, q.minute) }
                            .padding(horizontal = 6.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(q.ic, q.cat.color, size = 22.dp)
                        Spacer(Modifier.width(14.dp))
                        Txt(q.name, size = 15.sp, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        AppIcon(Ic.PLUS, p.muted, size = 20.dp)
                    }
                }
                if (suggestions.isEmpty()) {
                    Txt("All suggestions added.", Modifier.padding(8.dp), size = 13.sp, color = p.muted)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        GradientButton(
            text = "Create Habit",
            onClick = { create() },
            enabled = draft.name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
