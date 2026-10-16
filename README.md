# Task Manager

A task manager for Android built with Kotlin and Jetpack Compose. Works fully offline;
everything is stored on the phone.

**Install:** https://github.com/Phaneendraaa/taskmanager/releases/download/latest/TaskManager.apk

## Features
- **Home**: greeting, date strip, category filters, today's tasks, overdue and anytime sections, search
- **Create / edit task**: title, notes (with voice input), date, start and end time, repeat,
  category, priority, sub-tasks, file attachments
- **Task detail**: sub-task progress, attachments, Mark as Complete, duplicate, delete with Undo
- **Calendar**: month view with category dots and a list for the selected day
- **Progress**: Week / Month / Year, completion ring, pending and overdue counts, daily goal,
  tasks by category
- **Profile**: name, light / dark / system theme, daily goal, clear completed
- **Daily habits**: tick each day, streaks, 30 day rate, heatmap
- Repeating tasks create their next occurrence when completed

## Build
Every push to `main` is built by GitHub Actions (`.github/workflows/build-apk.yml`) and the
APK is published to the `latest` release. See `PROGRESS.md` for how the redesign was split up.
