package com.example.taskmanager

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Holds tasks and habits and saves them on this device (SharedPreferences). No backend. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tasks", Context.MODE_PRIVATE)

    val tasks = mutableStateListOf<Task>()
    val habits = mutableStateListOf<Habit>()

    var themeMode by mutableStateOf(prefs.getString("theme", "light") ?: "light")
        private set

    var now by mutableStateOf(LocalDateTime.now())
        private set

    init {
        tasks.addAll(loadTasks())
        habits.addAll(loadHabits())
    }

    fun refreshNow() {
        now = LocalDateTime.now()
    }

    fun setTheme(mode: String) {
        themeMode = mode
        prefs.edit().putString("theme", mode).apply()
    }

    // ---------- Tasks ----------

    fun addTask(title: String, notes: String, dueDay: Long?, dueMinute: Int?, priority: Int) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        tasks.add(
            Task(
                id = UUID.randomUUID().toString(),
                title = clean,
                notes = notes.trim(),
                dueDay = dueDay,
                dueMinute = dueMinute,
                priority = priority
            )
        )
        saveTasks()
    }

    fun updateTask(id: String, title: String, notes: String, dueDay: Long?, dueMinute: Int?, priority: Int) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        val clean = title.trim()
        if (clean.isEmpty()) return
        tasks[i] = tasks[i].copy(
            title = clean,
            notes = notes.trim(),
            dueDay = dueDay,
            dueMinute = dueMinute,
            priority = priority
        )
        saveTasks()
    }

    fun toggleTask(id: String) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        val t = tasks[i]
        tasks[i] = if (t.done) {
            t.copy(done = false, completedAt = null)
        } else {
            t.copy(done = true, completedAt = System.currentTimeMillis())
        }
        saveTasks()
    }

    fun deleteTask(id: String): Task? {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return null
        val removed = tasks.removeAt(i)
        saveTasks()
        return removed
    }

    fun restoreTask(task: Task) {
        tasks.add(task)
        saveTasks()
    }

    fun clearCompleted() {
        tasks.removeAll { it.done }
        saveTasks()
    }

    // ---------- Habits ----------

    fun addHabit(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        habits.add(
            Habit(
                id = UUID.randomUUID().toString(),
                name = clean,
                createdDay = LocalDate.now().toEpochDay()
            )
        )
        saveHabits()
    }

    fun renameHabit(id: String, name: String) {
        val i = habits.indexOfFirst { it.id == id }
        val clean = name.trim()
        if (i < 0 || clean.isEmpty()) return
        habits[i] = habits[i].copy(name = clean)
        saveHabits()
    }

    fun toggleHabitDay(id: String, day: Long) {
        val i = habits.indexOfFirst { it.id == id }
        if (i < 0) return
        val h = habits[i]
        habits[i] = h.copy(doneDays = if (day in h.doneDays) h.doneDays - day else h.doneDays + day)
        saveHabits()
    }

    fun deleteHabit(id: String): Habit? {
        val i = habits.indexOfFirst { it.id == id }
        if (i < 0) return null
        val removed = habits.removeAt(i)
        saveHabits()
        return removed
    }

    fun restoreHabit(habit: Habit) {
        habits.add(habit)
        saveHabits()
    }

    // ---------- Storage ----------

    private fun saveTasks() {
        val arr = JSONArray()
        tasks.forEach { t ->
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("notes", t.notes)
            o.put("priority", t.priority)
            o.put("done", t.done)
            o.put("createdAt", t.createdAt)
            val due = t.dueDay
            if (due != null) o.put("dueDay", due)
            val minute = t.dueMinute
            if (minute != null) o.put("dueMinute", minute)
            val completed = t.completedAt
            if (completed != null) o.put("completedAt", completed)
            arr.put(o)
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    private fun loadTasks(): List<Task> {
        val raw = prefs.getString("list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Task(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    notes = o.optString("notes", ""),
                    dueDay = if (o.has("dueDay")) o.getLong("dueDay") else null,
                    dueMinute = if (o.has("dueMinute")) o.getInt("dueMinute") else null,
                    priority = o.optInt("priority", 0),
                    done = o.optBoolean("done", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    completedAt = if (o.has("completedAt")) o.getLong("completedAt") else null
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHabits() {
        val arr = JSONArray()
        habits.forEach { h ->
            val days = JSONArray()
            h.doneDays.forEach { days.put(it) }
            val o = JSONObject()
            o.put("id", h.id)
            o.put("name", h.name)
            o.put("createdDay", h.createdDay)
            o.put("days", days)
            arr.put(o)
        }
        prefs.edit().putString("habits", arr.toString()).apply()
    }

    private fun loadHabits(): List<Habit> {
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
                    createdDay = o.optLong("createdDay", LocalDate.now().toEpochDay()),
                    doneDays = set
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
