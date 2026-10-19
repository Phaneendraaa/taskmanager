package com.example.taskmanager

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Daily habit reminders. Each habit with a time and the reminder switch gets one alarm at a time. */
object Reminders {
    private const val CHANNEL = "habits"
    private const val ACTION = "com.example.taskmanager.REMIND"

    private fun pending(context: Context, id: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION).putExtra("id", id)
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTrigger(h: Habit, from: LocalDateTime): Long? {
        val m = h.timeMinute ?: return null
        for (k in 0..7) {
            val date = from.toLocalDate().plusDays(k.toLong())
            val dow = date.dayOfWeek.value - 1
            if ((h.daysMask shr dow) and 1 == 0) continue
            val t = date.atTime(m / 60, m % 60)
            if (t.isAfter(from)) return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return null
    }

    fun schedule(context: Context, h: Habit) {
        val at = nextTrigger(h, LocalDateTime.now()) ?: return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, h.id))
        } catch (e: Exception) {
            // alarms not allowed right now; the next app start will try again
        }
    }

    private fun cancel(context: Context, id: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pending(context, id))
    }

    /** Makes the alarms match the habits: schedules the wanted ones and cancels the rest. */
    fun sync(context: Context, habits: List<Habit>, enabled: Boolean) {
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val old = prefs.getStringSet("remIds", emptySet()) ?: emptySet()
        val keep = HashSet<String>()
        for (h in habits) {
            if (enabled && h.reminder && h.timeMinute != null) {
                schedule(context, h)
                keep.add(h.id)
            }
        }
        for (id in old) {
            if (id !in keep) cancel(context, id)
        }
        prefs.edit().putStringSet("remIds", keep).apply()
    }

    private const val FOCUS_ACTION = "com.example.taskmanager.FOCUS_DONE"
    private const val FOCUS_CHANNEL = "focus"

    private fun focusPending(context: Context, label: String): PendingIntent {
        val intent = Intent(context, FocusReceiver::class.java).setAction(FOCUS_ACTION).putExtra("label", label)
        return PendingIntent.getBroadcast(
            context, 7001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Rings when a focus or break timer ends, even with the screen off or the app in the background. */
    fun scheduleFocusEnd(context: Context, atMs: Long, label: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, focusPending(context, label))
        } catch (e: Exception) {
        }
    }

    fun cancelFocusEnd(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(focusPending(context, ""))
    }

    fun notifyFocusDone(context: Context, label: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(FOCUS_CHANNEL, "Focus timer", NotificationManager.IMPORTANCE_HIGH)
        )
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(context, FOCUS_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_habit)
            .setContentTitle("Session complete")
            .setContentText(if (label.isBlank()) "Take a short break." else label + ". Take a short break.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(7001, n)
    }

    fun notifyHabit(context: Context, h: Habit) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = when {
            h.cue.isNotBlank() -> h.cueSentence() ?: "Time for your habit."
            h.minimum.isNotBlank() -> "Short on time? " + h.minimum.trim().trimEnd('.') + " still counts."
            else -> "Time for your habit. Small steps count."
        }
        fun action(kind: String, label: String, code: Int): Notification.Action {
            val pi = PendingIntent.getBroadcast(
                context, h.id.hashCode() + code,
                Intent(context, HabitActionReceiver::class.java).setAction(kind).putExtra("id", h.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return Notification.Action.Builder(null as android.graphics.drawable.Icon?, label, pi).build()
        }
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_habit)
            .setContentTitle(h.name)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(action(HabitActionReceiver.DONE, "Done", 11))
            .addAction(action(HabitActionReceiver.SNOOZE, "Snooze 30 min", 12))
            .build()
        nm.notify(h.id.hashCode(), n)
    }

    /** Marks a habit done for [day] straight in the saved data, so it works while the app is closed. */
    fun markDone(context: Context, id: String, day: Long) {
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val raw = prefs.getString("habits", null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getString("id") != id) continue
                val days = o.optJSONArray("days") ?: JSONArray().also { o.put("days", it) }
                var has = false
                for (k in 0 until days.length()) if (days.getLong(k) == day) has = true
                if (!has) days.put(day)
            }
            prefs.edit().putString("habits", arr.toString()).apply()
        } catch (e: Exception) {
        }
    }

    fun snooze(context: Context, id: String, minutes: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION).putExtra("id", id).putExtra("snooze", true)
        val pi = PendingIntent.getBroadcast(
            context, id.hashCode() + 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pi)
        } catch (e: Exception) {
        }
    }

    // ---- Daily nudges: a morning planning prompt and an evening note about what is still open ----

    private const val NUDGE_ACTION = "com.example.taskmanager.NUDGE"
    private const val NUDGE_CHANNEL = "nudges"
    const val MORNING_MIN = 8 * 60
    const val EVENING_MIN = 20 * 60 + 30

    private fun nudgePending(context: Context, kind: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, 7100 + kind,
            Intent(context, NudgeReceiver::class.java).setAction(NUDGE_ACTION).putExtra("kind", kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun scheduleNudge(context: Context, kind: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val m = if (kind == 0) MORNING_MIN else EVENING_MIN
        val now = LocalDateTime.now()
        var t = now.toLocalDate().atTime(m / 60, m % 60)
        if (!t.isAfter(now)) t = t.plusDays(1)
        try {
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), nudgePending(context, kind)
            )
        } catch (e: Exception) {
        }
    }

    fun syncNudges(context: Context, enabled: Boolean) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        for (kind in 0..1) {
            if (enabled) scheduleNudge(context, kind) else alarm.cancel(nudgePending(context, kind))
        }
    }

    fun notifyNudge(context: Context, id: Int, title: String, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(NUDGE_CHANNEL, "Daily nudges", NotificationManager.IMPORTANCE_DEFAULT))
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(context, NUDGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_habit)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }

    /** Reads the saved habits directly, so alarms work even when the app is not running. */
    fun loadHabits(prefs: SharedPreferences): List<Habit> {
        Rest.load(prefs)
        val raw = prefs.getString("habits", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val days = o.optJSONArray("days")
                val set = HashSet<Long>()
                if (days != null) {
                    for (k in 0 until days.length()) set.add(days.getLong(k))
                }
                Habit(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    createdDay = o.optLong("createdDay", 0L),
                    doneDays = set,
                    timeMinute = if (o.has("timeMinute")) o.getInt("timeMinute") else null,
                    daysMask = o.optInt("daysMask", 127),
                    reminder = o.optBoolean("reminder", false),
                    cue = o.optString("cue", ""),
                    minimum = o.optString("minimum", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("remindersOn", true)) return
        val h = Reminders.loadHabits(prefs).firstOrNull { it.id == id } ?: return
        if (!h.reminder) return
        val today = LocalDate.now().toEpochDay()
        if (h.isDue(today) && today !in h.doneDays) Reminders.notifyHabit(context, h)
        if (!intent.getBooleanExtra("snooze", false)) Reminders.schedule(context, h)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val on = prefs.getBoolean("remindersOn", true)
        Reminders.sync(context, Reminders.loadHabits(prefs), on)
        Reminders.syncNudges(context, on)
    }
}

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        if (prefs.getInt("timerMode", 0) != 0) return
        Reminders.notifyFocusDone(context, intent.getStringExtra("label") ?: "")
    }
}

class HabitActionReceiver : BroadcastReceiver() {
    companion object {
        const val DONE = "com.example.taskmanager.HABIT_DONE"
        const val SNOOZE = "com.example.taskmanager.HABIT_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.cancel(id.hashCode())
        when (intent.action) {
            DONE -> Reminders.markDone(context, id, LocalDate.now().toEpochDay())
            SNOOZE -> Reminders.snooze(context, id, 30)
        }
    }
}

class NudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getIntExtra("kind", 0)
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("remindersOn", true)) return
        val today = LocalDate.now().toEpochDay()
        if (kind == 0) {
            val picked = try {
                org.json.JSONObject(prefs.getString("priorities", "{}") ?: "{}").has(today.toString())
            } catch (e: Exception) {
                false
            }
            if (!picked) {
                Reminders.notifyNudge(context, 7100, "Plan your day", "Choose up to three priorities. It takes a minute and sets the tone.")
            }
        } else {
            val open = Reminders.loadHabits(prefs).filter { it.isDue(today) && today !in it.doneDays && it.createdDay <= today }
            if (open.isNotEmpty()) {
                val names = open.take(2).joinToString(" and ") { it.name }
                val more = if (open.size > 2) " and ${open.size - 2} more" else ""
                Reminders.notifyNudge(
                    context, 7101, "Still open today",
                    "$names$more. A tiny version still counts. Don't miss twice."
                )
            }
        }
        Reminders.scheduleNudge(context, kind)
    }
}
