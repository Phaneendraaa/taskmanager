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
    val reviews = mutableStateListOf<DayReview>()
    /** Task ids picked as "today's three", per epoch day. */
    val priorities = androidx.compose.runtime.mutableStateMapOf<Long, List<String>>()

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

    /** "compact", "standard" or "large". */
    var displaySize by mutableStateOf(prefs.getString("displaySize", "compact") ?: "compact")
        private set

    val uiScale: Float get() = when (displaySize) {
        "standard" -> 0.94f
        "large" -> 1.0f
        else -> 0.88f
    }

    fun updateDisplaySize(key: String) {
        displaySize = key
        prefs.edit().putString("displaySize", key).apply()
    }

    /** Who the person wants to become, from onboarding. Shown on Today as a quiet reminder. */
    var identity by mutableStateOf(prefs.getString("identity", "") ?: "")
        private set

    /** When the person says they usually quit: "early", "busy", "boring" or "never". */
    val dropoff: String get() = prefs.getString("dropoff", "") ?: ""

    /** Daily deep-work goal in minutes. */
    var focusTarget by mutableIntStateOf(prefs.getInt("focusTarget", 120))
        private set

    var themeMode by mutableStateOf(prefs.getString("theme", "light") ?: "light")
        private set

    var dailyGoal by mutableIntStateOf(prefs.getInt("goal", 5))
        private set

    var userName by mutableStateOf(prefs.getString("name", "") ?: "")
        private set

    var accentKey by mutableStateOf(prefs.getString("accent", "sage") ?: "sage")
        private set

    var remindersOn by mutableStateOf(prefs.getBoolean("remindersOn", true))
        private set

    var now by mutableStateOf(LocalDateTime.now())
        private set

    /** The day shown on Home and Calendar (epoch day). */
    var selectedDay by mutableStateOf(LocalDate.now().toEpochDay())

    init {
        tasks.addAll(loadTasks())
        habits.addAll(loadHabits())
        loadPlanning()
        focusSessions.addAll(loadFocus())
        Reminders.sync(getApplication<Application>(), habits, remindersOn)
        Reminders.syncNudges(getApplication<Application>(), remindersOn)
        restoreTimer()
    }

    /** Picks up changes made outside the app, such as tapping Done on a notification. */
    fun reloadFromDisk() {
        val fresh = loadHabits()
        if (fresh.map { it.id to it.doneDays } != habits.map { it.id to it.doneDays }) {
            habits.clear()
            habits.addAll(fresh)
        }
    }

    fun updateIdentity(text: String) {
        identity = text.take(60)
        prefs.edit().putString("identity", identity).apply()
    }

    fun updateDropoff(key: String) {
        prefs.edit().putString("dropoff", key).apply()
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

    fun updateReminders(on: Boolean) {
        remindersOn = on
        prefs.edit().putBoolean("remindersOn", on).apply()
        Reminders.sync(getApplication<Application>(), habits, on)
        Reminders.syncNudges(getApplication<Application>(), on)
    }

    fun updateAccent(key: String) {
        accentKey = key
        prefs.edit().putString("accent", key).apply()
    }

    /** Everything stored on this phone as one JSON text, for backup. */
    fun exportBackup(): String {
        val o = JSONObject()
        for ((k, v) in prefs.all) {
            if (v is String || v is Int || v is Long || v is Boolean) o.put(k, v)
        }
        return o.toString(2)
    }

    /** Replaces everything with a backup made by [exportBackup]. Returns false if the text is not a backup. */
    fun importBackup(text: String): Boolean {
        try {
            val o = JSONObject(text)
            if (!(o.has("list") || o.has("habits") || o.has("focus") || o.has("name") || o.has("goal"))) return false
            val e = prefs.edit()
            e.clear()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = o.get(k)
                if (v is String) e.putString(k, v)
                else if (v is Int) e.putInt(k, v)
                else if (v is Long) e.putLong(k, v)
                else if (v is Boolean) e.putBoolean(k, v)
            }
            e.putBoolean("onboarded", true)
            e.apply()
            reloadAll()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun reloadAll() {
        tasks.clear()
        tasks.addAll(loadTasks())
        habits.clear()
        habits.addAll(loadHabits())
        loadPlanning()
        focusSessions.clear()
        focusSessions.addAll(loadFocus())
        themeMode = prefs.getString("theme", "light") ?: "light"
        accentKey = prefs.getString("accent", "sage") ?: "sage"
        dailyGoal = prefs.getInt("goal", 5).coerceIn(1, 30)
        userName = prefs.getString("name", "") ?: ""
        identity = prefs.getString("identity", "") ?: ""
        onboarded = prefs.getBoolean("onboarded", true)
        focusLen = prefs.getInt("focusLen", 25)
        shortLen = prefs.getInt("shortLen", 5)
        longLen = prefs.getInt("longLen", 15)
        focusTarget = prefs.getInt("focusTarget", 120)
        timerRunning = false
        focusMode = 0
        timerLeftMs = focusLen * 60_000L
        remindersOn = prefs.getBoolean("remindersOn", true)
        Reminders.sync(getApplication<Application>(), habits, remindersOn)
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
        prefs.edit().putLong("timerEnd", timerEndAt).putInt("timerMode", focusMode)
            .putString("timerLabel", focusLabel).apply()
        Reminders.scheduleFocusEnd(getApplication<Application>(), timerEndAt, focusLabel)
    }

    fun pauseTimer() {
        if (!timerRunning) return
        timerLeftMs = timerRemainingMs(System.currentTimeMillis())
        timerRunning = false
        clearTimerState()
    }

    fun selectMode(mode: Int) {
        timerRunning = false
        focusMode = mode
        timerLeftMs = lenOf(mode) * 60_000L
        clearTimerState()
    }

    fun resetTimer() = selectMode(focusMode)

    private fun clearTimerState() {
        prefs.edit().remove("timerEnd").remove("timerMode").remove("timerLabel").apply()
        Reminders.cancelFocusEnd(getApplication<Application>())
    }

    /** Call on every tick. Returns true when a session just ended. */
    fun completeIfDone(): Boolean {
        if (!timerRunning || timerEndAt > System.currentTimeMillis()) return false
        finishSession(timerEndAt)
        return true
    }

    private fun finishSession(endedAt: Long) {
        timerRunning = false
        clearTimerState()
        if (focusMode == 0) {
            focusSessions.add(FocusSession(endedAt, focusLen, focusLabel.ifBlank { "Focus" }))
            saveFocus()
            val doneToday = focusSessions.count { epochDayOf(it.endedAt) == epochDayOf(endedAt) }
            selectMode(if (doneToday % 4 == 0) 2 else 1)
        } else {
            selectMode(0)
        }
    }

    /** After the app was closed: resume a running timer, or log a session that ended meanwhile. */
    private fun restoreTimer() {
        val end = prefs.getLong("timerEnd", 0L)
        if (end <= 0L) return
        focusMode = prefs.getInt("timerMode", 0).coerceIn(0, 2)
        focusLabel = prefs.getString("timerLabel", "") ?: ""
        if (end <= System.currentTimeMillis()) {
            finishSession(end)
        } else {
            timerEndAt = end
            timerLeftMs = end - System.currentTimeMillis()
            timerRunning = true
        }
    }

    fun updateFocusTarget(minutes: Int) {
        focusTarget = minutes.coerceIn(15, 480)
        prefs.edit().putInt("focusTarget", focusTarget).apply()
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

    /** Wipes habits, tasks, focus history and check-ins, then shows the welcome flow again. Settings stay. */
    fun startFresh() {
        habits.clear()
        tasks.clear()
        focusSessions.clear()
        reviews.clear()
        priorities.clear()
        saveHabits()
        saveTasks()
        saveFocus()
        savePlanning()
        selectMode(0)
        updateIdentity("")
        onboarded = false
        prefs.edit().putBoolean("onboarded", false).apply()
    }

    /** Shows the welcome flow again without touching any data. */
    fun replayIntro() {
        onboarded = false
        prefs.edit().putBoolean("onboarded", false).apply()
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

    /** Marks the tiny version done: it counts as showing up, and is remembered as minimum only. */
    fun doMinimum(id: String, day: Long) {
        val i = habits.indexOfFirst { it.id == id }
        if (i < 0) return
        val h = habits[i]
        habits[i] = h.copy(doneDays = h.doneDays + day, miniDays = h.miniDays + day)
        saveHabits()
    }

    fun toggleHabitDay(id: String, day: Long) {
        val i = habits.indexOfFirst { it.id == id }
        if (i < 0) return
        val h = habits[i]
        habits[i] = if (day in h.doneDays) h.copy(doneDays = h.doneDays - day, miniDays = h.miniDays - day)
        else h.copy(doneDays = h.doneDays + day)
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
            o.put("cue", h.cue)
            o.put("minimum", h.minimum)
            o.put("identity", h.identity)
            val mini = JSONArray()
            h.miniDays.forEach { mini.put(it) }
            o.put("mini", mini)
            arr.put(o)
        }
        prefs.edit().putString("habits", arr.toString()).apply()
        Reminders.sync(getApplication<Application>(), habits, remindersOn)
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
                    reminder = o.optBoolean("reminder", false),
                    cue = o.optString("cue", ""),
                    minimum = o.optString("minimum", ""),
                    identity = o.optString("identity", ""),
                    miniDays = HashSet<Long>().also { s ->
                        val m = o.optJSONArray("mini")
                        if (m != null) for (k in 0 until m.length()) s.add(m.getLong(k))
                    }
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

    // ---- Today's three and evening reviews ----

    /** Up to three task ids chosen for [day], dropping tasks that no longer exist. */
    fun prioritiesFor(day: Long): List<String> =
        (priorities[day] ?: emptyList()).filter { id -> tasks.any { it.id == id } }

    /** Adds or removes a task from the day's three. Returns false when the day already has three. */
    fun togglePriority(day: Long, taskId: String): Boolean {
        val cur = prioritiesFor(day)
        val next = if (taskId in cur) cur - taskId else {
            if (cur.size >= 3) return false
            cur + taskId
        }
        priorities[day] = next
        savePlanning()
        return true
    }

    fun reviewFor(day: Long): DayReview? = reviews.firstOrNull { it.day == day }

    fun saveReview(day: Long, rating: Int, note: String) {
        reviews.removeAll { it.day == day }
        reviews.add(DayReview(day, rating.coerceIn(1, 5), note.trim()))
        savePlanning()
    }

    private fun savePlanning() {
        val po = JSONObject()
        val keep = LocalDate.now().toEpochDay() - 120
        priorities.forEach { (d, ids) -> if (d >= keep && ids.isNotEmpty()) po.put(d.toString(), JSONArray(ids)) }
        val ra = JSONArray()
        reviews.sortedBy { it.day }.takeLast(400).forEach { r ->
            ra.put(JSONObject().put("day", r.day).put("rating", r.rating).put("note", r.note))
        }
        prefs.edit().putString("priorities", po.toString()).putString("reviews", ra.toString()).apply()
    }

    private fun loadPlanning() {
        priorities.clear()
        reviews.clear()
        try {
            val po = JSONObject(prefs.getString("priorities", "{}") ?: "{}")
            val keys = po.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = po.getJSONArray(k)
                priorities[k.toLong()] = (0 until arr.length()).map { arr.getString(it) }
            }
            val ra = JSONArray(prefs.getString("reviews", "[]") ?: "[]")
            for (i in 0 until ra.length()) {
                val o = ra.getJSONObject(i)
                reviews.add(DayReview(o.getLong("day"), o.getInt("rating"), o.optString("note", "")))
            }
        } catch (e: Exception) {
        }
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
