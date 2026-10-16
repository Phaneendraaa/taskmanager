package com.example.taskmanager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private data class Suggest(val name: String, val cat: HabitCategory, val icon: Ic, val minute: Int?)

private class GoalOption(val key: String, val title: String, val sub: String, val ic: Ic)

private val GOALS = listOf(
    GoalOption("health", "Health and fitness", "Move more, feel stronger", Ic.DUMBBELL),
    GoalOption("food", "Eat and walk better", "Simple food and daily steps", Ic.FOOD),
    GoalOption("learn", "Learning", "Read and pick up new skills", Ic.BOOK),
    GoalOption("focus", "Focus and work", "Deep work, fewer distractions", Ic.TIMER),
    GoalOption("mind", "Calm and mindfulness", "Slow down and reflect", Ic.LEAF),
    GoalOption("sleep", "Better sleep", "A steady wind-down routine", Ic.MOON)
)

private val WAKE_LABELS = listOf("Before 6 AM", "6 to 7 AM", "7 to 8 AM", "After 8 AM")
private val WAKE_MINUTES = listOf(5 * 60 + 30, 6 * 60 + 30, 7 * 60 + 30, 8 * 60 + 30)
private val ENERGY_LABELS = listOf("Morning", "Afternoon", "Evening")
private val ENERGY_SUB = listOf("I do my best work early", "I peak after lunch", "I come alive at night")
private val ENERGY_MINUTES = listOf(9 * 60, 14 * 60, 19 * 60)
private val ENERGY_ICONS = listOf(Ic.SUN, Ic.CLOUD, Ic.MOON)

private fun buildPlan(wake: Int, goals: List<String>, focus: Int, count: Int): List<Suggest> {
    fun at(m: Int): Int = m % 1440
    val chosen = if (goals.isEmpty()) listOf("health", "focus") else goals
    val pairs = chosen.map { g ->
        when (g) {
            "health" -> listOf(
                Suggest("Workout", HabitCategory.HEALTH, Ic.DUMBBELL, at(wake + 60)),
                Suggest("Drink water", HabitCategory.HEALTH, Ic.DROP, null)
            )
            "food" -> listOf(
                Suggest("Healthy breakfast", HabitCategory.HEALTH, Ic.FOOD, at(wake + 90)),
                Suggest("Walk 20 minutes", HabitCategory.HEALTH, Ic.HEART, 18 * 60)
            )
            "learn" -> listOf(
                Suggest("Read 10 pages", HabitCategory.LEARNING, Ic.BOOK, 21 * 60),
                Suggest("Learn something new", HabitCategory.LEARNING, Ic.LAPTOP, at(focus + 60))
            )
            "focus" -> listOf(
                Suggest("Deep work session", HabitCategory.PRODUCTIVITY, Ic.TIMER, focus),
                Suggest("Plan your day", HabitCategory.PRODUCTIVITY, Ic.NOTES, at(wake + 30))
            )
            "mind" -> listOf(
                Suggest("Meditate", HabitCategory.PERSONAL, Ic.LEAF, at(wake + 15)),
                Suggest("Gratitude note", HabitCategory.PERSONAL, Ic.HEART, 21 * 60 + 30)
            )
            else -> listOf(
                Suggest("Screens off", HabitCategory.PERSONAL, Ic.MOON, 22 * 60),
                Suggest("Lights out", HabitCategory.PERSONAL, Ic.MOON, 22 * 60 + 45)
            )
        }
    }
    val out = ArrayList<Suggest>()
    out.add(Suggest("Wake up on time", HabitCategory.PERSONAL, Ic.SUN, wake))
    for (round in 0..1) {
        for (pair in pairs) {
            if (out.size < count) out.add(pair[round])
        }
    }
    return out
}

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    var page by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(vm.userName) }
    var wake by remember { mutableIntStateOf(1) }
    val goals = remember { mutableStateListOf<String>() }
    var energy by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(5) }
    val picked = remember { mutableStateListOf<Int>() }
    val last = 6

    val plan = buildPlan(WAKE_MINUTES[wake], goals.toList(), ENERGY_MINUTES[energy], count)

    BackHandler(enabled = page > 0) { page -= 1 }

    fun finish() {
        val today = LocalDate.now().toEpochDay()
        if (name.isNotBlank()) vm.updateUserName(name.trim())
        vm.updateDailyGoal(count)
        picked.sorted().forEach { i ->
            val s = plan.getOrNull(i) ?: return@forEach
            vm.upsertHabit(
                Habit(
                    id = vm.newId(),
                    name = s.name,
                    createdDay = today,
                    category = s.cat,
                    timeMinute = s.minute,
                    icon = s.icon.name
                )
            )
        }
        vm.finishOnboarding()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
    ) {
        Crossfade(targetState = page, label = "onboarding") { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
            ) {
                if (current == 0) {
                    Welcome(
                        onStart = { page = 1 },
                        onSkip = { vm.finishOnboarding() }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                        RoundIconButton(Ic.BACK, onClick = { page -= 1 }, size = 40.dp)
                        Spacer(Modifier.size(14.dp))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            for (i in 1..last) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(if (i <= current) p.accent else p.faint)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    when (current) {
                        1 -> Step("What should we call you?", "Just a first name is fine.") {
                            Column(Modifier.fillMaxWidth().cardStyle(p, 20.dp).padding(horizontal = 18.dp, vertical = 14.dp)) {
                                PlainField(
                                    value = name,
                                    onValueChange = { name = it.take(24) },
                                    placeholder = "Your name",
                                    size = 20.sp,
                                    weight = FontWeight.SemiBold,
                                    singleLine = true,
                                    autoFocus = true,
                                    imeAction = ImeAction.Done,
                                    onDone = { page = 2 }
                                )
                            }
                        }
                        2 -> Step("When do you usually wake up?", "We will plan your morning around it.") {
                            WAKE_LABELS.forEachIndexed { i, label ->
                                OptionCard(label, null, if (i < 2) Ic.SUN else Ic.CLOUD, wake == i) { wake = i }
                            }
                        }
                        3 -> Step("What do you want to build?", "Pick as many as you like.") {
                            GOALS.forEach { g ->
                                OptionCard(g.title, g.sub, g.ic, g.key in goals) {
                                    if (g.key in goals) goals.remove(g.key) else goals.add(g.key)
                                }
                            }
                        }
                        4 -> Step("When is your energy highest?", "Focus habits go in your best window.") {
                            ENERGY_LABELS.forEachIndexed { i, label ->
                                OptionCard(label, ENERGY_SUB[i], ENERGY_ICONS[i], energy == i) { energy = i }
                            }
                        }
                        5 -> Step("How many habits to start with?", "Start small. You can add more any time.") {
                            listOf(3, 5, 7).forEach { n ->
                                val sub = when (n) {
                                    3 -> "Gentle and easy to keep"
                                    5 -> "A balanced daily routine"
                                    else -> "For a full, structured day"
                                }
                                OptionCard("$n habits a day", sub, Ic.TARGET, count == n) { count = n }
                            }
                        }
                        else -> Step("Your starter plan", "Untick anything you do not want.") {
                            plan.forEachIndexed { i, s ->
                                PlanRow(s, i in picked) {
                                    if (i in picked) picked.remove(i) else picked.add(i)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = if (current == last) "Start forging" else "Continue",
                        onClick = {
                            if (current == last) {
                                finish()
                            } else {
                                if (current == 5) {
                                    picked.clear()
                                    plan.indices.forEach { picked.add(it) }
                                }
                                page = current + 1
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Ic.ARROW_RIGHT,
                        enabled = current != 3 || goals.isNotEmpty()
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Welcome(onStart: () -> Unit, onSkip: () -> Unit) {
    val p = LocalPalette.current
    Spacer(Modifier.height(64.dp))
    Txt("forge", Modifier.fillMaxWidth(), size = 58.sp, weight = FontWeight.Bold, serif = true, align = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Txt(
        "Build a better you\nwith small consistent\nactions.",
        Modifier.fillMaxWidth(),
        size = 17.sp,
        color = p.muted,
        align = TextAlign.Center,
        lineHeight = 24.sp
    )
    Spacer(Modifier.height(40.dp))
    WelcomeCard(Ic.TARGET, "Habits", "Your daily system.", p.accent)
    Spacer(Modifier.height(12.dp))
    WelcomeCard(Ic.NOTES, "Tasks", "Stay organized.", Color(0xFFD98B4A))
    Spacer(Modifier.height(12.dp))
    WelcomeCard(Ic.CHART, "Insights", "See real progress.", Color(0xFF7C6BC4))
    Spacer(Modifier.weight(1f))
    GradientButton("Get started", onStart, Modifier.fillMaxWidth(), icon = Ic.ARROW_RIGHT)
    Spacer(Modifier.height(8.dp))
    Txt(
        "Skip for now",
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onSkip).padding(12.dp),
        size = 14.sp,
        color = p.muted,
        align = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun WelcomeCard(ic: Ic, title: String, sub: String, tint: Color) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { AppIcon(ic, tint, size = 24.dp) }
        Spacer(Modifier.size(14.dp))
        Column {
            Txt(title, size = 16.sp, weight = FontWeight.SemiBold)
            Txt(sub, size = 13.sp, color = p.muted)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Step(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    Txt(title, size = 30.sp, weight = FontWeight.Bold, serif = true, lineHeight = 36.sp)
    Spacer(Modifier.height(6.dp))
    Txt(subtitle, size = 15.sp, color = p.muted)
    Spacer(Modifier.height(22.dp))
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun OptionCard(title: String, sub: String?, ic: Ic, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) p.accent.copy(alpha = 0.12f) else p.card)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) p.accent else p.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(p.field),
            contentAlignment = Alignment.Center
        ) { AppIcon(ic, p.accent, size = 22.dp) }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, size = 16.sp, weight = FontWeight.SemiBold)
            if (sub != null) Txt(sub, size = 13.sp, color = p.muted)
        }
        Tick(selected)
    }
}

@Composable
private fun Tick(on: Boolean) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (on) p.accent else Color.Transparent)
            .border(1.5.dp, if (on) p.accent else p.faint, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (on) AppIcon(Ic.CHECK, Color.White, size = 14.dp, stroke = 2.6f)
    }
}

@Composable
private fun PlanRow(s: Suggest, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .cardStyle(p, 20.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(s.cat.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { AppIcon(s.icon, s.cat.color, size = 22.dp) }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(s.name, size = 16.sp, weight = FontWeight.SemiBold)
            Txt(
                (if (s.minute != null) formatMinute(s.minute) + "  •  " else "Anytime  •  ") + s.cat.label,
                size = 12.sp,
                color = p.muted
            )
        }
        Tick(on)
    }
}
