# Task Manager

Minimal black-and-white task and habit tracker for Android (Kotlin + Jetpack Compose).
Fully offline: everything is stored on the device.

## Features

**Tasks**
- Title, notes, "done by" date and optional time, priority (none / low / medium / high)
- Grouped by Overdue, Today, Tomorrow, Upcoming, Anytime
- Filters: All, Today, Overdue, Done
- Overdue detection (including a missed time today), today's progress bar
- Edit, delete with undo, clear completed, created / completed timestamps

**Habits**
- Daily habits shown every day with a one-tap check
- Current streak, 30-day rate, last-7-days strip (tap a past day to backfill)
- Detail view with best streak, total days and a 16-week heatmap

**Insights**
- Completed this week / all time, on-time rate, overdue count
- 7-day completion chart, weekly habit consistency
- Light / Dark / System appearance

## Install (no Android Studio)

Every push to `main` builds the APK on GitHub Actions and publishes it to the
`latest` release:

https://github.com/Phaneendraaa/taskmanager/releases/download/latest/TaskManager.apk

Open it on your phone and tap Install. New builds install over the old one and
keep your data (the debug signing key is fixed in `app/debug.keystore`).

## Build locally

Needs JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).

```
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
