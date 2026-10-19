# HabitAnvil

A calm, professional habit and focus app for adults and students. Kotlin and Jetpack Compose, paper-and-ink
design with a graphite dark mode, one restrained accent. Fully offline: everything stays on the phone.

**Install:** https://github.com/Phaneendraaa/taskmanager/releases/download/latest/TaskManager.apk

## How it keeps you coming back (see `docs/RESEARCH.md` for the evidence)
- **Plan, do, review**: pick up to three priorities each morning, work through a routine grouped by
  time of day, close the day with a twenty second check-in.
- **Habits tied to cues**: every habit can have an anchor ("After I ..."), a minimum version for bad
  days that still counts, and an identity line.
- **Consistency over streaks**: a 30 day consistency score and a run that forgives one missed day.
  Miss one and the app says "Don't miss twice".
- **Graduation**: after about 66 days at high consistency a habit is marked Automatic.
- **Insights in plain words**: weekly review, best weekday and time of day, per-habit consistency,
  deep work minutes against your daily target.
- **Rest days**: pause habits when ill or away. Streaks, rates and reminders skip those days.
- **Weekly review**: every Sunday evening, what went well, what slipped, and one thing to protect or drop.
- **Home-screen widget**: today's priorities and habits, tap to tick them off without opening the app.
- **Notifications that help**: Done and Snooze buttons on reminders, a morning planning nudge,
  an evening note about what is still open, and a focus-finished alert with the screen off.

## Screens
Today, Habits, Focus, Insights (bottom tabs). Settings opens from the gear on Today. No calendar.

## Other
Tasks with dates, repeat, priority, sub-tasks and attachments; Light, Dark or System theme; three sober
accents (Verdigris, Slate, Brass); backup and restore to a file.

## Build
Every push to `main` is built by GitHub Actions (`.github/workflows/build-apk.yml`) and the APK is
published to the `latest` release. `PROGRESS.md` shows how the redesign was split into parts.
