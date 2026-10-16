# forge

A habits-first Android app built with Kotlin and Jetpack Compose, in a calm Sage theme with a
matching dark mode. It works fully offline and everything is stored on the phone.

**Install:** https://github.com/Phaneendraaa/taskmanager/releases/download/latest/TaskManager.apk

## Features
- **Onboarding**: welcome page, a few lifestyle questions (name, wake-up time, goals, best energy
  window, how many habits) and a starter habit plan you can edit before you begin
- **Home**: daily ring (done / due habits), day streak, weekly percentage, total habits, and a
  Habits / Tasks switch with a Today list
- **Habits**: filter by category, grouped Morning / Afternoon / Evening, tick them off, undo delete
- **Habit details**: Overview (week, completion, total days, longest streak, heatmap), History
  (tap any day to fix it) and Edit (category, frequency, time, icon, reminder)
- **Add Habit / Task**: one screen with a switch, plus Quick Add suggestions and voice input
- **Tasks**: dates, times, repeat, priority, sub-tasks, attachments, calendar view, Mark as Complete
- **Focus timer**: Focus, Short Break and Long Break with a ring, current task, saved sessions and
  adjustable lengths; it keeps running while you look at other screens
- **Stats**: Overview, Habits, Tasks and Focus tabs for This Week / Month / Year, with activity
  heatmap, habit categories donut and focus timeline
- **Settings**: Light / Dark / System, four accent themes (Sage, Sky, Peach, Lavender), daily goal,
  focus lengths, habit reminders (notifications), backup and restore to a file

## Build
Every push to `main` is built by GitHub Actions (`.github/workflows/build-apk.yml`) and the APK is
published to the `latest` release. `PROGRESS.md` shows how the redesign was split into parts.
