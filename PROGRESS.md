# Redesign progress

Goal: rebuild the app to match the reference design (light + dark, purple gradient, floating
bottom bar) with every feature shown in it. Work is split into small parts. Each part is
pushed on its own and built by GitHub Actions, so a broken part is easy to find.

Install the latest build:
https://github.com/Phaneendraaa/taskmanager/releases/download/latest/TaskManager.apk

Legend: DONE = pushed and the build passed, PUSHED = pushed, build being checked,
TODO = not started

| # | Part | Status |
|---|------|--------|
| 1 | Design system, data layer, app shell (palette, icons, hero art, components, model, floating nav) | DONE |
| 2 | Home screen (greeting hero, date strip, category chips, task cards, search) | DONE |
| 3 | Create / Edit task (date, time, repeat, category, priority, sub-tasks, attachments, voice notes) | DONE |
| 4 | Task detail (chips, info, sub-tasks, attachments, Mark as Complete, undo delete) | DONE |
| 5 | Calendar (month grid with task dots, day list) | DONE |
| 6 | Progress (Week/Month/Year, ring, stats, daily goal, tasks by category) | DONE |
| 7 | Profile and Habits (theme, daily goal, name, daily habits with streaks) | DONE |
| 8 | Final polish, README, release check | DONE |

## Part details

### 1. Design system, data layer, app shell (DONE)
- `Theme.kt` light and dark palettes, `Icons.kt` icons drawn in code, `Hero.kt` sunrise illustration
- `Components.kt` shared pieces (cards, check circle, gradient button, chips, fields, heatmap)
- `Models.kt` / `AppViewModel.kt`: categories, repeat, sub-tasks, attachments, daily goal, theme, storage
  (old saved tasks still load)
- `Navigation.kt` floating bottom bar with raised + button
- Build fix: renamed two functions whose names clashed with property setters

### 2. Home screen (DONE)
- `HomeScreen.kt`: time-based greeting, "Let's make it happen." hero with quote of the day,
  scrollable date strip, category filter chips, Today list, overdue and anytime sections,
  search, menu (habits, dark mode, clear completed)
- Task cards: check circle, icon, category dot, time, high-priority flag, sub-task count
- The + button and opening a task now lead to the task form (part 3)

### 3. Create / Edit task (DONE)
- `TaskForm.kt`: title, notes with voice input, date, start and end time, repeat, category,
  priority, sub-tasks, attachments, gradient Add Task / Save button
- `AppRoot.kt`: pages open on top of the tabs, system back closes them; + opens a new task
  for the selected day, tapping a task opens it for editing (the detail page comes in part 4)

### 4. Task detail (DONE)
- `TaskDetail.kt`: back / edit / more, big checkbox with title and flag, category and priority
  chips, date, time range, repeat, description, sub-tasks with progress, attachments, Mark as Complete
- Tapping a task opens the detail; Edit opens the form; Delete shows an Undo bar

### 5. Calendar (DONE)
- `CalendarScreen.kt`: month grid (Monday first) with category dots, previous / next month,
  today highlight, selected day list using the same task cards; the selected day is shared with Home

### 6. Progress (DONE)
- `StatsScreen.kt`: Week / Month / Year switch, week strip with status dots (tap a day to open it
  on Home), completion ring with percent, Completed / Pending / Overdue counts, Daily Goal bar,
  Tasks by Category donut with legend

### 7. Profile and Habits (DONE)
- `ProfileScreen.kt`: name, completed / streak / habit counts, Light / Dark / System, daily goal
  stepper, link to habits, clear completed
- `HabitsScreen.kt`: add habits, tick today, streak, best streak, 30 day rate, heatmap, undo delete
- Habits open from the Home menu and from Profile

### 8. Final polish (DONE)
- Version 3.0, README rewritten for the new app

### Notes
- Everything is stored on the device only.
- Repeating tasks create their next occurrence when completed.
