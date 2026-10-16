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

private class Extras(val cue: String, val minimum: String, val identity: String)

/** Starter habits come with an anchor, a tiny version and an identity so they are easier to keep. */
private fun extras(name: String): Extras = when (name) {
    "Wake up on time" -> Extras("", "Get out of bed when the alarm rings", "someone who starts the day on purpose")
    "Workout" -> Extras("I get home", "Five minutes of movement", "someone who trains consistently")
    "Drink water" -> Extras("I sit down at my desk", "One glass", "someone who looks after their body")
    "Healthy breakfast" -> Extras("I wake up", "Something with protein", "someone who fuels well")
    "Walk 20 minutes" -> Extras("I finish lunch", "Walk around the block", "someone who moves every day")
    "Read 10 pages" -> Extras("I get into bed", "Read one page", "a reader")
    "Learn something new" -> Extras("I open my laptop to study", "Ten minutes", "a lifelong learner")
    "Deep work session" -> Extras("I sit down at my desk", "One 10 minute sprint", "someone who does deep work")
    "Plan your day" -> Extras("I make my first coffee", "Write one priority", "someone who plans the day")
    "Meditate" -> Extras("I wake up", "Three slow breaths", "someone who stays calm")
    "Gratitude note" -> Extras("I brush my teeth", "One line", "someone who notices the good")
    "Screens off" -> Extras("I set my alarm for tomorrow", "Screens off ten minutes earlier", "someone who protects their sleep")
    "Lights out" -> Extras("I finish my wind-down", "In bed on time tonight", "someone who protects their sleep")
    else -> Extras("", "", "")
}

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

private class IdOption(val key: String, val phrase: String, val title: String, val ic: Ic)

private val IDENTITIES = listOf(
    IdOption("student", "a focused student", "A focused student", Ic.BOOK),
    IdOption("strong", "a healthy, strong person", "A healthy, strong person", Ic.DUMBBELL),
    IdOption("learner", "a reader and lifelong learner", "A reader and learner", Ic.LAPTOP),
    IdOption("calm", "someone calm and steady", "A calm, steady mind", Ic.LEAF),
    IdOption("builder", "a builder who finishes things", "A builder who ships", Ic.TARGET),
    IdOption("rested", "someone who protects their sleep", "A well-rested person", Ic.MOON)
)

private class DropOption(val key: String, val title: String, val sub: String)

private val DROPS = listOf(
    DropOption("early", "After the first few days", "We will keep the start tiny and steady"),
    DropOption("busy", "When life gets busy", "Every habit gets a minimum version for bad days"),
    DropOption("boring", "When it stops feeling new", "We will show real progress so it stays meaningful"),
    DropOption("never", "I rarely get started", "We will make the first step almost effortless")
)

private class TourCard(val ic: Ic, val title: String, val what: String, val why: String, val sample: List<String>)

private val TOUR = listOf(
    TourCard(
        Ic.HOME, "Today: plan, do, review",
        "Each morning you choose up to three priorities. Your routine sits below, grouped by time of day. In the evening you rate the day in twenty seconds.",
        "Deciding in advance beats relying on willpower in the moment. The evening check-in turns every day into a small lesson.",
        listOf("Finish database assignment", "Email project proposal", "Prepare for Friday's exam")
    ),
    TourCard(
        Ic.REPEAT, "Habits that attach to your day",
        "Every habit can have an anchor (\"After I pour my coffee\"), a minimum version for bad days that still counts, and an identity line about who you are becoming.",
        "Linking a habit to something you already do makes follow-through far more likely than good intentions alone.",
        listOf("After I get into bed, I will: Read 10 pages", "Short on time? Read one page")
    ),
    TourCard(
        Ic.CHECK, "Consistency, not perfection",
        "You will see a 30 day consistency score and a run that forgives one missed day. Miss one and the app simply says: do not miss twice.",
        "Research on habit formation found a single missed day makes no real difference. Two in a row is where habits quietly die.",
        listOf("86% over 30 days", "Don't miss twice")
    ),
    TourCard(
        Ic.TIMER, "Focus with a target",
        "A deep-work timer with a daily target. It keeps running if you leave the app and alerts you when it ends, even with the screen off.",
        "Protected blocks of focused time are how hard things actually get done. A target makes progress visible.",
        listOf("25:00  Deep work", "75 of 120 min today")
    ),
    TourCard(
        Ic.CHART, "Insights in plain words",
        "A weekly review that tells you what is working, your strongest day and time, and one small thing to change next.",
        "What gets measured honestly gets improved. No streak pressure, just a clear picture.",
        listOf("83% of habits done, last 7 days", "Best on Wednesdays")
    )
)

private val PAGES = listOf("welcome", "name", "why", "identity", "dropoff", "wake", "energy", "tour", "starter")

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    var page by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(vm.userName) }
    var wake by remember { mutableIntStateOf(1) }
    val goals = remember { mutableStateListOf<String>() }
    var energy by remember { mutableIntStateOf(0) }
    var identity by remember { mutableStateOf<String?>(null) }
    var dropoff by remember { mutableStateOf<String?>(null) }
    val picked = remember { mutableStateListOf<Int>() }
    var tourStep by remember { mutableIntStateOf(0) }
    val last = PAGES.size - 1

    val plan = buildPlan(WAKE_MINUTES[wake], goals.toList(), ENERGY_MINUTES[energy], 6)

    fun back() {
        if (PAGES[page] == "tour" && tourStep > 0) tourStep -= 1 else page -= 1
    }
    BackHandler(enabled = page > 0) { back() }

    fun finish() {
        val today = LocalDate.now().toEpochDay()
        if (name.isNotBlank()) vm.updateUserName(name.trim())
        val idPhrase = IDENTITIES.firstOrNull { it.key == identity }?.phrase ?: ""
        vm.updateIdentity(idPhrase)
        vm.updateDropoff(dropoff ?: "")
        picked.sorted().forEach { i ->
            val s = plan.getOrNull(i) ?: return@forEach
            val x = extras(s.name)
            vm.upsertHabit(
                Habit(
                    id = vm.newId(),
                    name = s.name,
                    createdDay = today,
                    category = s.cat,
                    timeMinute = s.minute,
                    icon = s.icon.name,
                    cue = x.cue,
                    minimum = x.minimum,
                    identity = x.identity
                )
            )
        }
        vm.finishOnboarding()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
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
                        RoundIconButton(Ic.BACK, onClick = { back() }, size = 40.dp)
                        Spacer(Modifier.size(14.dp))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 1..last) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(if (i <= current) p.text else p.faint)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    when (PAGES[current]) {
                        "name" -> Step("What should we call you?", "Just a first name is fine.") {
                            Column(Modifier.fillMaxWidth().cardStyle(p).padding(horizontal = 18.dp, vertical = 14.dp)) {
                                PlainField(
                                    value = name,
                                    onValueChange = { name = it.take(24) },
                                    placeholder = "Your name",
                                    size = 20.sp,
                                    weight = FontWeight.SemiBold,
                                    singleLine = true,
                                    autoFocus = true,
                                    imeAction = ImeAction.Done,
                                    onDone = { page = current + 1 }
                                )
                            }
                        }
                        "why" -> Step(
                            if (name.isBlank()) "What do you want more of?" else name.trim() + ", what do you want more of?",
                            "Pick as many as you like. This shapes your suggestions."
                        ) {
                            GOALS.forEach { g ->
                                OptionCard(g.title, g.sub, g.ic, g.key in goals) {
                                    if (g.key in goals) goals.remove(g.key) else goals.add(g.key)
                                }
                            }
                        }
                        "identity" -> Step(
                            "A year from now, who are you?",
                            "Habits last when they are about who you are, not only what you do."
                        ) {
                            IDENTITIES.forEach { o ->
                                OptionCard(o.title, null, o.ic, identity == o.key) { identity = o.key }
                            }
                        }
                        "dropoff" -> Step(
                            "Be honest: when do you usually quit?",
                            "Nearly everyone does. Knowing when lets us plan for it."
                        ) {
                            DROPS.forEach { o ->
                                OptionCard(o.title, o.sub, Ic.TARGET, dropoff == o.key) { dropoff = o.key }
                            }
                        }
                        "wake" -> Step("When do you usually wake up?", "We will plan your morning around it.") {
                            WAKE_LABELS.forEachIndexed { i, label ->
                                OptionCard(label, null, if (i < 2) Ic.SUN else Ic.CLOUD, wake == i) { wake = i }
                            }
                        }
                        "energy" -> Step("When is your energy highest?", "Focus habits go in your best window.") {
                            ENERGY_LABELS.forEachIndexed { i, label ->
                                OptionCard(label, ENERGY_SUB[i], ENERGY_ICONS[i], energy == i) { energy = i }
                            }
                        }
                        "tour" -> {
                            val c = TOUR[tourStep]
                            Step(c.title, c.what) { TourBody(c, tourStep, TOUR.size) }
                        }
                        else -> Step(
                            "Start small, or start clean",
                            "One to three habits is plenty. Nothing is added unless you tick it."
                        ) {
                            plan.forEachIndexed { i, s ->
                                PlanRow(s, i in picked) {
                                    if (i in picked) picked.remove(i) else picked.add(i)
                                }
                            }
                            if (picked.size > 3) {
                                Txt(
                                    "That is a lot to begin with. People who start with two or three tend to keep going.",
                                    size = 13.sp, color = p.muted, lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = when {
                            PAGES[current] == "tour" -> if (tourStep < TOUR.size - 1) "Next" else "Got it"
                            current != last -> "Continue"
                            picked.isEmpty() -> "Start with a clean slate"
                            picked.size == 1 -> "Begin with 1 habit"
                            else -> "Begin with ${picked.size} habits"
                        },
                        onClick = {
                            if (current == last) finish()
                            else if (PAGES[current] == "tour" && tourStep < TOUR.size - 1) tourStep += 1
                            else page = current + 1
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Ic.ARROW_RIGHT,
                        enabled = when (PAGES[current]) {
                            "why" -> goals.isNotEmpty()
                            "identity" -> identity != null
                            "dropoff" -> dropoff != null
                            else -> true
                        }
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
    Spacer(Modifier.height(56.dp))
    Txt("forge", Modifier.fillMaxWidth(), size = 52.sp, weight = FontWeight.SemiBold, serif = true, align = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    Txt(
        "Most people do not fail because they lack motivation. They fail because they have no system.",
        Modifier.fillMaxWidth(),
        size = 17.sp,
        color = p.text,
        align = TextAlign.Center,
        lineHeight = 25.sp
    )
    Spacer(Modifier.height(10.dp))
    Txt(
        "A few honest questions, then a clean slate. Nothing is added unless you choose it.",
        Modifier.fillMaxWidth(),
        size = 14.sp,
        color = p.muted,
        align = TextAlign.Center,
        lineHeight = 20.sp
    )
    Spacer(Modifier.height(32.dp))
    WelcomeCard(Ic.TARGET, "Plan", "Three priorities each morning.")
    Spacer(Modifier.height(12.dp))
    WelcomeCard(Ic.REPEAT, "Practise", "Habits tied to your routine.")
    Spacer(Modifier.height(12.dp))
    WelcomeCard(Ic.CHART, "Review", "Twenty seconds each evening.")
    Spacer(Modifier.weight(1f))
    GradientButton("Begin", onStart, Modifier.fillMaxWidth(), icon = Ic.ARROW_RIGHT)
    Spacer(Modifier.height(8.dp))
    Txt(
        "Skip, I will set up myself",
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onSkip).padding(12.dp),
        size = 14.sp,
        color = p.muted,
        align = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun WelcomeCard(ic: Ic, title: String, sub: String) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(p.field),
            contentAlignment = Alignment.Center
        ) { AppIcon(ic, p.muted, size = 22.dp) }
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
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(p.field),
            contentAlignment = Alignment.Center
        ) { AppIcon(s.icon, p.muted, size = 22.dp) }
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

@Composable
private fun TourBody(c: TourCard, step: Int, total: Int) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().cardStyle(p).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt("PREVIEW", size = 11.sp, weight = FontWeight.SemiBold, color = p.muted)
        c.sample.forEachIndexed { i, line ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.field).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .background(if (i == 0) p.accent else Color.Transparent)
                        .border(1.5.dp, if (i == 0) p.accent else p.muted.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { if (i == 0) AppIcon(Ic.CHECK, Color.White, size = 12.dp, stroke = 2.6f) }
                Spacer(Modifier.size(12.dp))
                Txt(line, size = 14.sp, weight = FontWeight.Medium)
            }
        }
    }
    Column(Modifier.fillMaxWidth().cardStyle(p).padding(16.dp)) {
        Txt("WHY IT WORKS", size = 11.sp, weight = FontWeight.SemiBold, color = p.accentText)
        Spacer(Modifier.height(6.dp))
        Txt(c.why, size = 14.sp, lineHeight = 20.sp)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        for (i in 0 until total) {
            Box(
                Modifier.padding(horizontal = 3.dp).size(if (i == step) 8.dp else 6.dp).clip(CircleShape)
                    .background(if (i == step) p.text else p.faint)
            )
        }
    }
}
