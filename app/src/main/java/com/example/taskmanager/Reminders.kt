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
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_habit)
            .setContentTitle(h.name)
            .setContentText("Time for your habit. Small steps count.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(h.id.hashCode(), n)
    }

    /** Reads the saved habits directly, so alarms work even when the app is not running. */
    fun loadHabits(prefs: SharedPreferences): List<Habit> {
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
                    reminder = o.optBoolean("reminder", false)
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
        Reminders.schedule(context, h)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        Reminders.sync(context, Reminders.loadHabits(prefs), prefs.getBoolean("remindersOn", true))
    }
}

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        if (prefs.getInt("timerMode", 0) != 0) return
        Reminders.notifyFocusDone(context, intent.getStringExtra("label") ?: "")
    }
}
