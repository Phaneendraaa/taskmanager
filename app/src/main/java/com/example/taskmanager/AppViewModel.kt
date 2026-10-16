package com.example.taskmanager

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Holds tasks, habits and settings and saves them on this device (SharedPreferences). No backend. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tasks", Context.MODE_PRIVATE)

    val tasks = mutableStateListOf<Task>()
    val habits = mutableStateListOf<Habit>()
    val focusSessions = mutableStateListOf<FocusSession>()

    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
        private set

    var focusLen by mutableIntStateOf(prefs.getInt("focusLen", 25))
        private set
    var shortLen by mutableIntStateOf(prefs.getInt("shortLen", 5))
        private set
    var longLen by mutableIntStateOf(prefs.getInt("longLen", 15))
        private set

    // Focus timer. It lives here so it keeps running while other screens are open.
    /** 0 = focus, 1 = short break, 2 = long break. */
    var focusMode by mutableIntStateOf(0)
        private set
    var timerRunning by mutableStateOf(false)
        private set
    var timerEndAt by mutableLongStateOf(0L)
        private set
    var timerLeftMs by mutableLongStateOf(prefs.getInt("focusLen", 25) * 60_000L)
        private set
    var focusLabel by mutableStateOf("")

    var themeMode by mutableStateOf(prefs.getString("theme", "light") ?: "light")
        private set

    var dailyGoal by mutableIntStateOf(prefs.getInt("goal", 5))
        private set

    var userName by mutableStateOf(prefs.getString("name", "") ?: "")
        private set

    var now by mutableStateOf(LocalDateTime.now())
        private set

    /** The day shown on Home and Calendar (epoch day). */
    var selectedDay by mutableStateOf(LocalDate.now().toEpochDay())

    init {
        tasks.addAll(loadTasks())
        habits.addAll(loadHabits())
        focusSessions.addAll(loadFocus())
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun refreshNow() {
        val before = now.toLocalDate().toEpochDay()
        now = LocalDateTime.now()
        val after = now.toLocalDate().toEpochDay()
        if (after != before && selectedDay == before) selectedDay = after
    }

    // ---------- Settings ----------

    fun setTheme(mode: String) {
        themeMode = mode
        prefs.edit().putString("theme", mode).apply()
    }

    fun updateDailyGoal(goal: Int) {
        dailyGoal = goal.coerceIn(1, 30)
        prefs.edit().putInt("goal", dailyGoal).apply()
    }

    fun updateUserName(name: String) {
        userName = name.take(24)
        prefs.edit().putString("name", userName).apply()
    }

    fun finishOnboarding() {
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    fun updateFocusLengths(focus: Int, short: Int, long: Int) {
        focusLen = focus.coerceIn(5, 90)
        shortLen = short.coerceIn(1, 30)
        longLen = long.coerceIn(5, 60)
        prefs.edit().putInt("focusLen", focusLen).putInt("shortLen", shortLen).putInt("longLen", longLen).apply()
        if (!timerRunning) timerLeftMs = lenOf(focusMode) * 60_000L
    }

    fun lenOf(mode: Int): Int = when (mode) {
        0 -> focusLen
        1 -> shortLen
        else -> longLen
    }

    fun timerRemainingMs(nowMs: Long): Long =
        if (timerRunning) (timerEndAt - nowMs).coerceAtLeast(0L) else timerLeftMs

    fun startTimer() {
        if (timerRunning) return
        timerEndAt = System.currentTimeMillis() + timerLeftMs
        timerRunning = true
    }

    fun pauseTimer() {
        if (!timerRunning) return
        timerLeftMs = timerRemainingMs(System.currentTimeMillis())
        timerRunning = false
    }

    fun selectMode(mode: Int) {
        timerRunning = false
        focusMode = mode
        timerLeftMs = lenOf(mode) * 60_000L
    }

    fun resetTimer() = selectMode(focusMode)

    /** Call on every tick. Returns true when a session just ended. */
    fun completeIfDone(): Boolean {
        if (!timerRunning || timerEndAt > System.currentTimeMillis()) return false
        timerRunning = false
        if (focusMode == 0) {
            logFocus(focusLen, focusLabel.ifBlank { "Focus" })
            val doneToday = focusSessions.count { epochDayOf(it.endedAt) == LocalDate.now().toEpochDay() }
            selectMode(if (doneToday % 4 == 0) 2 else 1)
        } else {
            selectMode(0)
        }
        return true
    }

    fun logFocus(minutes: Int, label: String) {
        if (minutes <= 0) return
        focusSessions.add(FocusSession(System.currentTimeMillis(), minutes, label))
        saveFocus()
    }

    // ---------- Tasks ----------

    fun taskById(id: String): Task? = tasks.firstOrNull { it.id == id }

    fun upsertTask(task: Task) {
        val i = tasks.indexOfFirst { it.id == task.id }
        if (i >= 0) tasks[i] = task else tasks.add(task)
        saveTasks()
    }

    fun toggleTask(id: String) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        val t = tasks[i]
        if (t.done) {
            tasks[i] = t.copy(done = false, completedAt = null)
        } else {
            tasks[i] = t.copy(done = true, completedAt = System.currentTimeMillis())
            spawnNextOccurrence(t)
        }
        saveTasks()
    }

    /** A repeating task creates its next occurrence when it is completed. */
    private fun spawnNextOccurrence(t: Task) {
        val day = t.dueDay ?: return
        if (t.repeat == Repeat.NONE) return
        val today = LocalDate.now().toEpochDay()
        var next = nextDay(day, t.repeat)
        var guard = 0
        while (next < today && guard < 800) {
            next = nextDay(next, t.repeat)
            guard++
        }
        tasks.add(
            t.copy(
                id = newId(),
                dueDay = next,
                done = false,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                subtasks = t.subtasks.map { it.copy(done = false) }
            )
        )
    }

    private fun nextDay(day: Long, r: Repeat): Long = when (r) {
        Repeat.DAILY -> day + 1
        Repeat.WEEKLY -> day + 7
        Repeat.MONTHLY -> LocalDate.ofEpochDay(day).plusMonths(1).toEpochDay()
        Repeat.NONE -> day
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

    fun duplicateTask(id: String): String? {
        val t = taskById(id) ?: return null
        val copy = t.copy(
            id = newId(),
            title = t.title + " (copy)",
            done = false,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            subtasks = t.subtasks.map { it.copy(id = newId(), done = false) }
        )
        tasks.add(copy)
        saveTasks()
        return copy.id
    }

    fun clearCompleted() {
        tasks.removeAll { it.done }
        saveTasks()
    }

    fun toggleSubTask(taskId: String, subId: String) {
        val t = taskById(taskId) ?: return
        upsertTask(t.copy(subtasks = t.subtasks.map { if (it.id == subId) it.copy(done = !it.done) else it }))
    }

    fun addSubTask(taskId: String, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val t = taskById(taskId) ?: return
        upsertTask(t.copy(subtasks = t.subtasks + SubTask(newId(), clean)))
    }

    fun removeSubTask(taskId: String, subId: String) {
        val t = taskById(taskId) ?: return
        upsertTask(t.copy(subtasks = t.subtasks.filter { it.id != subId }))
    }

    fun addAttachment(taskId: String, att: Attachment) {
        val t = taskById(taskId) ?: return
        if (t.attachments.any { it.uri == att.uri }) return
        upsertTask(t.copy(attachments = t.attachments + att))
    }

    fun removeAttachment(taskId: String, uri: String) {
        val t = taskById(taskId) ?: return
        upsertTask(t.copy(attachments = t.attachments.filter { it.uri != uri }))
    }

    // ---------- Habits ----------

    fun addHabit(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        habits.add(Habit(id = newId(), name = clean, createdDay = LocalDate.now().toEpochDay()))
        saveHabits()
    }

    fun upsertHabit(habit: Habit) {
        val i = habits.indexOfFirst { it.id == habit.id }
        if (i >= 0) habits[i] = habit else habits.add(habit)
        saveHabits()
    }

    fun habitById(id: String): Habit? = habits.firstOrNull { it.id == id }

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
            o.put("category", t.category.name)
            o.put("repeat", t.repeat.name)
            o.put("done", t.done)
            o.put("createdAt", t.createdAt)
            val due = t.dueDay
            if (due != null) o.put("dueDay", due)
            val minute = t.dueMinute
            if (minute != null) o.put("dueMinute", minute)
            val end = t.endMinute
            if (end != null) o.put("endMinute", end)
            val completed = t.completedAt
            if (completed != null) o.put("completedAt", completed)

            val subs = JSONArray()
            t.subtasks.forEach { s ->
                val so = JSONObject()
                so.put("id", s.id)
                so.put("title", s.title)
                so.put("done", s.done)
                subs.put(so)
            }
            o.put("subtasks", subs)

            val atts = JSONArray()
            t.attachments.forEach { a ->
                val ao = JSONObject()
                ao.put("uri", a.uri)
                ao.put("name", a.name)
                ao.put("size", a.size)
                ao.put("mime", a.mime)
                atts.put(ao)
            }
            o.put("attachments", atts)
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

                val subs = ArrayList<SubTask>()
                val subArr = o.optJSONArray("subtasks")
                if (subArr != null) {
                    for (k in 0 until subArr.length()) {
                        val so = subArr.getJSONObject(k)
                        subs.add(SubTask(so.getString("id"), so.getString("title"), so.optBoolean("done", false)))
                    }
                }
                val atts = ArrayList<Attachment>()
                val attArr = o.optJSONArray("attachments")
                if (attArr != null) {
                    for (k in 0 until attArr.length()) {
                        val ao = attArr.getJSONObject(k)
                        atts.add(
                            Attachment(
                                ao.getString("uri"),
                                ao.optString("name", "File"),
                                ao.optLong("size", 0L),
                                ao.optString("mime", "")
                            )
                        )
                    }
                }

                Task(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    notes = o.optString("notes", ""),
                    dueDay = if (o.has("dueDay")) o.getLong("dueDay") else null,
                    dueMinute = if (o.has("dueMinute")) o.getInt("dueMinute") else null,
                    endMinute = if (o.has("endMinute")) o.getInt("endMinute") else null,
                    priority = o.optInt("priority", 0),
                    category = enumOrDefault(o.optString("category", ""), Category.OTHER),
                    repeat = enumOrDefault(o.optString("repeat", ""), Repeat.NONE),
                    subtasks = subs,
                    attachments = atts,
                    done = o.optBoolean("done", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    completedAt = if (o.has("completedAt")) o.getLong("completedAt") else null
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E {
        return try {
            enumValueOf<E>(name)
        } catch (e: Exception) {
            default
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
            o.put("category", h.category.name)
            val tm = h.timeMinute
            if (tm != null) o.put("timeMinute", tm)
            o.put("daysMask", h.daysMask)
            o.put("icon", h.icon)
            o.put("reminder", h.reminder)
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
                    doneDays = set,
                    category = enumOrDefault(o.optString("category", ""), HabitCategory.PERSONAL),
                    timeMinute = if (o.has("timeMinute")) o.getInt("timeMinute") else null,
                    daysMask = o.optInt("daysMask", 127),
                    icon = o.optString("icon", ""),
                    reminder = o.optBoolean("reminder", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveFocus() {
        val arr = JSONArray()
        focusSessions.forEach { f ->
            val o = JSONObject()
            o.put("at", f.endedAt)
            o.put("min", f.minutes)
            o.put("label", f.label)
            arr.put(o)
        }
        prefs.edit().putString("focus", arr.toString()).apply()
    }

    private fun loadFocus(): List<FocusSession> {
        val raw = prefs.getString("focus", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FocusSession(o.getLong("at"), o.getInt("min"), o.optString("label", ""))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
