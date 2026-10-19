package com.example.taskmanager

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/** Renders screens with sample data to PNG files in build/previews. Used by CI, not part of the app. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class PreviewShots {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun seeded(): AppViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = AppViewModel(app)
        val today = LocalDate.now().toEpochDay()
        vm.updateUserName("Phani")
        vm.finishOnboarding()
        fun habit(name: String, cat: HabitCategory, minute: Int?, done: Boolean, days: Int, icon: Ic) {
            val set = HashSet<Long>()
            for (i in 1..days) set.add(today - i)
            if (done) set.add(today)
            vm.upsertHabit(
                Habit(
                    id = vm.newId(), name = name, createdDay = today - 40, doneDays = set,
                    category = cat, timeMinute = minute, icon = icon.name
                )
            )
        }
        habit("Wake up early", HabitCategory.PERSONAL, 6 * 60 + 30, true, 12, Ic.SUN)
        habit("Workout", HabitCategory.HEALTH, 8 * 60, true, 9, Ic.DUMBBELL)
        habit("Read 10 pages", HabitCategory.LEARNING, 13 * 60, false, 20, Ic.BOOK)
        habit("Deep work session", HabitCategory.PRODUCTIVITY, 14 * 60, false, 5, Ic.TIMER)
        habit("Drink water", HabitCategory.HEALTH, null, true, 30, Ic.DROP)
        habit("Meditate", HabitCategory.PERSONAL, 22 * 60, false, 3, Ic.LEAF)
        vm.upsertTask(Task(id = vm.newId(), title = "Finish database assignment", dueDay = today, dueMinute = 17 * 60, priority = 3, category = Category.STUDY))
        vm.upsertTask(Task(id = vm.newId(), title = "Email the project proposal", dueDay = today, priority = 2, category = Category.WORK))
        vm.upsertTask(Task(id = vm.newId(), title = "Prepare for Friday's exam", dueDay = today + 2, priority = 3, category = Category.STUDY))
        vm.logFocus(25, "Database assignment")
        vm.logFocus(50, "Deep work")
        return vm
    }

    private fun shot(name: String, dark: Boolean, waitMs: Long = 1200, fresh: Boolean = false, content: @Composable (AppViewModel) -> Unit) {
        val vm = if (fresh) AppViewModel(ApplicationProvider.getApplicationContext<Application>()) else seeded()
        // Screens contain endless timers; step the clock by hand instead of waiting for idle.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(dark) {
                Box(Modifier.fillMaxSize()) { content(vm) }
            }
        }
        var t = 0L
        while (t < waitMs) {
            rule.mainClock.advanceTimeBy(300)
            shadowOf(Looper.getMainLooper()).idle()
            t += 300
        }
        // Draw the window directly; waiting for "idle" never finishes while timers are running.
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/previews")
        dir.mkdirs()
        FileOutputStream(File(dir, name + (if (dark) "-dark" else "") + ".png")).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun app_light() = shot("app", false) { AppRoot(it) }
    @Test fun app_dark() = shot("app", true) { AppRoot(it) }
    @Test fun habits_light() = shot("habits", false) { HabitsScreen(it, null, {}, {}) }
    @Test fun stats_light() = shot("stats", false) { InsightsScreen(it, PaddingValues(0.dp)) }
    @Test fun settings_light() = shot("settings", false) { ProfileScreen(it, PaddingValues(0.dp), {}, {}) }
    @Test fun onboarding_light() = shot("onboarding", false, waitMs = 4500, fresh = true) { OnboardingScreen(it) }
    @Test fun add_sheet_light() = shot("addsheet", false) { AddSheet({}, {}, {}) }
    @Test fun taskform_light() = shot("taskform", false) { TaskForm(it, null, LocalDate.now().toEpochDay(), LocalDate.now().toEpochDay(), {}, {}) }
    @Test fun habitform_light() = shot("habitform", false) { HabitForm(it, LocalDate.now().toEpochDay(), {}, {}) }
    @Test fun focus_dark() = shot("focus", true) { FocusScreen(it, null) }
    @Test fun rest_sheet_light() = shot("restsheet", false) { RestSheet(null, { _, _ -> }, {}, {}) }
    @Test fun today_rest_dark() = shot("today-rest", true) {
        androidx.compose.runtime.remember { it.addRest(LocalDate.now().toEpochDay(), 3) }
        TodayScreen(it, PaddingValues(0.dp), {}, {}, {}, {})
    }
}
