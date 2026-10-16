package com.example.taskmanager

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Repeat(val label: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

enum class Category(val label: String, val color: Color, val icon: Ic) {
    WORK("Work", Color(0xFF5B7FC7), Ic.BRIEFCASE),
    PERSONAL("Personal", Color(0xFF5FA777), Ic.HOME),
    STUDY("Study", Color(0xFFD9A441), Ic.BOOK),
    HEALTH("Health", Color(0xFFD9695F), Ic.DUMBBELL),
    CAREER("Career", Color(0xFFD98B4A), Ic.DOC),
    OTHER("Other", Color(0xFF8E9A92), Ic.MORE_H)
}

data class SubTask(val id: String, val title: String, val done: Boolean = false)

data class Attachment(val uri: String, val name: String, val size: Long, val mime: String)

/**
 * dueDay      = LocalDate.toEpochDay(), null = no date
 * dueMinute   = start time in minutes after midnight, null = any time
 * endMinute   = optional end time
 * priority    = 0 none, 1 low, 2 medium, 3 high
 */
data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val dueDay: Long? = null,
    val dueMinute: Int? = null,
    val endMinute: Int? = null,
    val priority: Int = 0,
    val category: Category = Category.OTHER,
    val repeat: Repeat = Repeat.NONE,
    val subtasks: List<SubTask> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class HabitCategory(val label: String, val color: Color, val icon: Ic) {
    HEALTH("Health", Color(0xFFD9695F), Ic.DUMBBELL),
    LEARNING("Learning", Color(0xFFE3B65B), Ic.BOOK),
    PERSONAL("Personal", Color(0xFF5FA777), Ic.PERSON),
    PRODUCTIVITY("Productivity", Color(0xFF5B7FC7), Ic.TARGET),
    OTHERS("Others", Color(0xFF8E9A92), Ic.MORE_H)
}

/**
 * A habit repeats on the days in [daysMask] (bit 0 = Monday ... bit 6 = Sunday, 127 = every day).
 * doneDays holds the epoch days it was completed. timeMinute is the optional time of day.
 */
data class Habit(
    val id: String,
    val name: String,
    val createdDay: Long,
    val doneDays: Set<Long> = emptySet(),
    val category: HabitCategory = HabitCategory.PERSONAL,
    val timeMinute: Int? = null,
    val daysMask: Int = 127,
    val icon: String = "",
    val reminder: Boolean = false
)

data class FocusSession(val endedAt: Long, val minutes: Int, val label: String)

fun Task.isOverdue(now: LocalDateTime): Boolean {
    val day = dueDay ?: return false
    if (done) return false
    val today = now.toLocalDate().toEpochDay()
    if (day < today) return true
    val minute = dueMinute
    return day == today && minute != null && minute < now.hour * 60 + now.minute
}

/** Picks a fitting icon from the title, falling back to the category icon. */
fun Task.iconKind(): Ic {
    val t = title.lowercase()
    fun has(vararg words: String): Boolean = words.any { t.contains(it) }
    return when {
        has("trip", "travel", "flight", "vacation", "holiday") -> Ic.PLANE
        has("read", "book", "study", "learn", "exam", "homework") -> Ic.BOOK
        has("workout", "gym", "exercise", "yoga", "run ", "walk") -> Ic.DUMBBELL
        has("resume", "cv ", "document", "report", "invoice") -> Ic.DOC
        has("meeting", "project", "proposal", "client", "deck") -> Ic.BRIEFCASE
        else -> category.icon
    }
}

// ---------- Habits ----------

enum class Period(val label: String) { MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening"), ANYTIME("Anytime") }

fun Habit.isDue(day: Long): Boolean {
    val dow = LocalDate.ofEpochDay(day).dayOfWeek.value - 1
    return (daysMask shr dow) and 1 == 1
}

fun Habit.period(): Period {
    val m = timeMinute ?: return Period.ANYTIME
    return when {
        m < 12 * 60 -> Period.MORNING
        m < 17 * 60 -> Period.AFTERNOON
        else -> Period.EVENING
    }
}

fun Habit.iconKind(): Ic {
    val named = Ic.values().firstOrNull { it.name == icon }
    return named ?: category.icon
}

fun Habit.frequencyText(): String = when {
    daysMask == 127 -> "Every day"
    daysMask == 31 -> "Weekdays"
    else -> {
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        names.filterIndexed { i, _ -> (daysMask shr i) and 1 == 1 }.joinToString(", ")
    }
}

/** Last day (up to today) that the habit was due, searching back at most a year. */
private fun Habit.lastDueOnOrBefore(day: Long): Long {
    var d = day
    var n = 0
    while (!isDue(d) && n < 370) { d--; n++ }
    return d
}

fun Habit.currentStreak(today: Long): Int {
    var d = today
    // Today does not break the streak until the day is over.
    if (isDue(d) && d !in doneDays) d--
    var n = 0
    var guard = 0
    while (guard < 4000) {
        guard++
        if (d < createdDay - 1 && d !in doneDays) break
        if (!isDue(d)) { if (d in doneDays) n++; d--; continue }
        if (d in doneDays) { n++; d-- } else break
    }
    return n
}

fun Habit.bestStreak(): Int {
    if (doneDays.isEmpty()) return 0
    val sorted = doneDays.sorted()
    var best = 0
    var run = 0
    var prev: Long? = null
    for (d in sorted) {
        val p0 = prev
        run = if (p0 == null) 1 else {
            var gapBroken = false
            var x = p0 + 1
            while (x < d) { if (isDue(x)) { gapBroken = true; break }; x++ }
            if (gapBroken) 1 else run + 1
        }
        if (run > best) best = run
        prev = d
    }
    return best
}

/** Percent of due days completed in the last [windowDays] days (since the habit was created). */
fun Habit.rate(today: Long, windowDays: Int = 30): Int {
    val start = maxOf(createdDay, today - (windowDays - 1))
    var due = 0
    var done = 0
    var d = start
    while (d <= today) {
        if (isDue(d)) { due++; if (d in doneDays) done++ }
        d++
    }
    return if (due == 0) 0 else done * 100 / due
}

/** Percent of due days completed since the habit was created. */
fun Habit.overallRate(today: Long): Int = rate(today, (today - createdDay + 1).toInt().coerceAtLeast(1))

fun Habit.dueCount(day: Long): Int = if (isDue(day)) 1 else 0

/** Days in a row (ending today or yesterday) with at least one completed task. */
fun completionStreak(tasks: List<Task>, today: Long): Int {
    val days = HashSet<Long>()
    tasks.forEach { t ->
        val c = t.completedAt
        if (c != null) days.add(epochDayOf(c))
    }
    var d = if (today in days) today else today - 1
    var n = 0
    while (d in days) {
        n++
        d--
    }
    return n
}

// ---------- Formatting ----------

fun epochDayOf(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

fun formatMinute(minute: Int): String =
    LocalTime.of(minute / 60, minute % 60)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

fun timeRange(start: Int?, end: Int?): String {
    if (start == null) return "Any time"
    return if (end != null) formatMinute(start) + " – " + formatMinute(end) else formatMinute(start)
}

private val DOW = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MON = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun shortDow(day: Long): String = DOW[LocalDate.ofEpochDay(day).dayOfWeek.value - 1]
fun shortMonth(day: Long): String = MON[LocalDate.ofEpochDay(day).monthValue - 1]
fun dayOfMonth(day: Long): Int = LocalDate.ofEpochDay(day).dayOfMonth
fun dayInitial(day: Long): String = DOW[LocalDate.ofEpochDay(day).dayOfWeek.value - 1].substring(0, 1)

fun formatDay(day: Long, today: Long): String {
    return when (day - today) {
        0L -> "Today"
        1L -> "Tomorrow"
        -1L -> "Yesterday"
        else -> {
            val d = LocalDate.ofEpochDay(day)
            val pattern = if (d.year == LocalDate.now().year) "EEE, d MMM" else "d MMM yyyy"
            d.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        }
    }
}

/** Short label for tight spaces: Today / Tomorrow / 20 Oct. */
fun shortDayLabel(day: Long, today: Long): String {
    return when (day - today) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> dayOfMonth(day).toString() + " " + shortMonth(day)
    }
}

fun fullDate(day: Long): String =
    LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))

fun repeatText(r: Repeat): String = when (r) {
    Repeat.NONE -> "Does not repeat"
    Repeat.DAILY -> "Repeats daily"
    Repeat.WEEKLY -> "Repeats weekly"
    Repeat.MONTHLY -> "Repeats monthly"
}

fun greeting(hour: Int): String = when {
    hour < 12 -> "Good Morning"
    hour < 17 -> "Good Afternoon"
    else -> "Good Evening"
}

private val QUOTES = listOf(
    "Small steps every day lead to big results.",
    "Done is better than perfect.",
    "Focus on progress, not perfection.",
    "Start where you are. Use what you have.",
    "Discipline is choosing what you want most.",
    "One task at a time. One day at a time.",
    "Your future is built by what you do today."
)

fun quoteOfTheDay(day: Long): String = QUOTES[(day % QUOTES.size).toInt().let { if (it < 0) it + QUOTES.size else it }]

fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}

fun fileKind(att: Attachment): String {
    val ext = att.name.substringAfterLast('.', "").uppercase()
    return when {
        ext.isNotEmpty() && ext.length <= 4 -> ext
        att.mime.startsWith("image/") -> "Image"
        else -> "File"
    }
}

fun stampOf(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault()))
