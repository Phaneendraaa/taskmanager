package com.example.taskmanager

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Home-screen widget: today's priorities and habits, each tappable to tick it off.
 * It reads the saved data directly, so it works while the app is closed.
 */
class TodayWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) manager.updateAppWidget(id, build(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> refresh(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.taskmanager.WIDGET_TOGGLE"
        private const val MAX_ROWS = 5

        private val ROWS = intArrayOf(R.id.w_row1, R.id.w_row2, R.id.w_row3, R.id.w_row4, R.id.w_row5)
        private val ICONS = intArrayOf(R.id.w_icon1, R.id.w_icon2, R.id.w_icon3, R.id.w_icon4, R.id.w_icon5)
        private val TEXTS = intArrayOf(R.id.w_text1, R.id.w_text2, R.id.w_text3, R.id.w_text4, R.id.w_text5)

        /** Redraws every placed widget. Safe to call often. */
        fun refresh(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
                if (ids.isEmpty()) return
                val views = build(context)
                for (id in ids) manager.updateAppWidget(id, views)
            } catch (e: Exception) {
                // a widget that cannot draw must never break the app
            }
        }

        private class Row(val kind: String, val id: String, val title: String, val done: Boolean, val opensApp: Boolean)

        private fun loadRows(prefs: SharedPreferences, today: Long): Triple<List<Row>, Int, Int> {
            val habits = Reminders.loadHabits(prefs)
                .filter { it.isDue(today) && it.createdDay <= today }
                .sortedWith(compareBy({ today in it.doneDays }, { it.timeMinute ?: Int.MAX_VALUE }, { it.name }))

            // Today's priorities, in the order they were picked.
            val picked = ArrayList<String>()
            try {
                val arr = JSONObject(prefs.getString("priorities", "{}") ?: "{}").optJSONArray(today.toString())
                if (arr != null) for (i in 0 until arr.length()) picked.add(arr.getString(i))
            } catch (e: Exception) {
            }
            val tasks = HashMap<String, Row>()
            try {
                val arr = JSONArray(prefs.getString("list", "[]") ?: "[]")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.getString("id")
                    tasks[id] = Row("t", id, o.optString("title", ""), o.optBoolean("done", false), o.optString("repeat", "NONE") != "NONE")
                }
            } catch (e: Exception) {
            }
            val priorities = picked.mapNotNull { tasks[it] }

            val rows = ArrayList<Row>()
            rows.addAll(priorities)
            habits.forEach { rows.add(Row("h", it.id, it.name, today in it.doneDays, false)) }
            val done = rows.count { it.done }
            return Triple(rows, done, rows.size)
        }

        fun build(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
            val today = LocalDate.now().toEpochDay()
            val (rows, done, total) = loadRows(prefs, today)
            val views = RemoteViews(context.packageName, R.layout.widget_today)

            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.w_root, open)
            views.setTextViewText(R.id.w_date, shortDow(today).uppercase() + "  " + dayOfMonth(today) + " " + shortMonth(today).uppercase())

            val resting = today in Rest.days
            views.setTextViewText(
                R.id.w_count,
                when {
                    resting && total == 0 -> "Rest day"
                    total == 0 -> ""
                    else -> "$done of $total"
                }
            )

            val muted = context.getColor(R.color.widget_muted)
            val text = context.getColor(R.color.widget_text)
            val shown = rows.take(MAX_ROWS)
            for (i in 0 until MAX_ROWS) {
                if (i >= shown.size) {
                    views.setViewVisibility(ROWS[i], android.view.View.GONE)
                    continue
                }
                val r = shown[i]
                views.setViewVisibility(ROWS[i], android.view.View.VISIBLE)
                views.setImageViewResource(ICONS[i], if (r.done) R.drawable.widget_check_on else R.drawable.widget_check_off)
                val label = SpannableString(r.title)
                if (r.done) label.setSpan(StrikethroughSpan(), 0, label.length, 0)
                views.setTextViewText(TEXTS[i], label)
                views.setTextColor(TEXTS[i], if (r.done) muted else text)
                val pending = if (r.opensApp) open else {
                    PendingIntent.getBroadcast(
                        context, (r.kind + r.id).hashCode(),
                        Intent(context, WidgetActionReceiver::class.java)
                            .setAction(ACTION_TOGGLE).putExtra("kind", r.kind).putExtra("id", r.id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                views.setOnClickPendingIntent(ROWS[i], pending)
            }

            val extra = rows.size - shown.size
            if (extra > 0) {
                views.setTextViewText(R.id.w_more, "+$extra more in forge")
                views.setViewVisibility(R.id.w_more, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.w_more, android.view.View.GONE)
            }

            if (rows.isEmpty()) {
                views.setTextViewText(
                    R.id.w_empty,
                    if (resting) "Resting today. Your streaks are safe."
                    else "Nothing for today yet. Tap to plan your day."
                )
                views.setViewVisibility(R.id.w_empty, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.w_empty, android.view.View.GONE)
            }
            return views
        }

        /** Ticks or unticks one habit for [day] straight in the saved data. */
        fun toggleHabit(prefs: SharedPreferences, id: String, day: Long) {
            val raw = prefs.getString("habits", null) ?: return
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.getString("id") != id) continue
                    val days = o.optJSONArray("days") ?: JSONArray().also { o.put("days", it) }
                    var at = -1
                    for (k in 0 until days.length()) if (days.getLong(k) == day) at = k
                    if (at >= 0) {
                        days.remove(at)
                        val mini = o.optJSONArray("mini")
                        if (mini != null) {
                            for (k in mini.length() - 1 downTo 0) if (mini.getLong(k) == day) mini.remove(k)
                        }
                    } else {
                        days.put(day)
                    }
                }
                prefs.edit().putString("habits", arr.toString()).apply()
            } catch (e: Exception) {
            }
        }

        /** Ticks or unticks one task straight in the saved data. */
        fun toggleTask(prefs: SharedPreferences, id: String) {
            val raw = prefs.getString("list", null) ?: return
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.getString("id") != id) continue
                    if (o.optBoolean("done", false)) {
                        o.put("done", false)
                        o.remove("completedAt")
                    } else {
                        o.put("done", true)
                        o.put("completedAt", System.currentTimeMillis())
                    }
                }
                prefs.edit().putString("list", arr.toString()).apply()
            } catch (e: Exception) {
            }
        }
    }
}

/** Handles taps on widget rows. Not exported, so only the widget itself can reach it. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TodayWidget.ACTION_TOGGLE) return
        val id = intent.getStringExtra("id") ?: return
        val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        when (intent.getStringExtra("kind")) {
            "h" -> TodayWidget.toggleHabit(prefs, id, LocalDate.now().toEpochDay())
            "t" -> TodayWidget.toggleTask(prefs, id)
        }
        TodayWidget.refresh(context)
    }
}
