package com.example.taskmanager

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * dueDay    = LocalDate.toEpochDay(), null = no deadline
 * dueMinute = minutes after midnight, null = all day
 * priority  = 0 none, 1 low, 2 medium, 3 high
 */
data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val dueDay: Long? = null,
    val dueMinute: Int? = null,
    val priority: Int = 0,
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

fun epochDayOf(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

fun formatMinute(minute: Int): String =
    LocalTime.of(minute / 60, minute % 60)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

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

fun dueText(task: Task, today: Long): String {
    val day = task.dueDay ?: return ""
    val minute = task.dueMinute
    return if (minute != null) formatDay(day, today) + ", " + formatMinute(minute) else formatDay(day, today)
}

fun lateText(task: Task, today: Long): String {
    val day = task.dueDay ?: return ""
    val late = today - day
    return when {
        late <= 0L -> "Overdue"
        late == 1L -> "1 day late"
        else -> "$late days late"
    }
}

fun stampOf(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault()))

private val DAY_INITIALS = arrayOf("M", "T", "W", "T", "F", "S", "S")

fun dayInitial(day: Long): String = DAY_INITIALS[LocalDate.ofEpochDay(day).dayOfWeek.value - 1]
