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
    WORK("Work", Color(0xFF6C63FF), Ic.BRIEFCASE),
    PERSONAL("Personal", Color(0xFF2ECC8F), Ic.HOME),
    STUDY("Study", Color(0xFFFF5C9E), Ic.BOOK),
    HEALTH("Health", Color(0xFFF5B73B), Ic.DUMBBELL),
    CAREER("Career", Color(0xFFFF8A3D), Ic.DOC),
    OTHER("Other", Color(0xFF8E94A8), Ic.MORE_H)
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

/** A habit repeats every day. doneDays holds the epoch days it was completed. */
data class Habit(
    val id: String,
    val name: String,
    val createdDay: Long,
    val doneDays: Set<Long> = emptySet()
)

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

fun Habit.currentStreak(today: Long): Int {
    var d = if (today in doneDays) today else today - 1
    var n = 0
    while (d in doneDays) {
        n++
        d--
    }
    return n
}

fun Habit.bestStreak(): Int {
    if (doneDays.isEmpty()) return 0
    val sorted = doneDays.sorted()
    var best = 1
    var run = 1
    for (i in 1 until sorted.size) {
        run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
        if (run > best) best = run
    }
    return best
}

/** Percent of days completed in the last [windowDays] days (since the habit was created). */
fun Habit.rate(today: Long, windowDays: Int = 30): Int {
    val start = maxOf(createdDay, today - (windowDays - 1))
    val span = (today - start + 1).toInt()
    if (span <= 0) return 0
    var done = 0
    var d = start
    while (d <= today) {
        if (d in doneDays) done++
        d++
    }
    return done * 100 / span
}

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
