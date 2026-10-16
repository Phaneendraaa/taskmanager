# Redesign progress

Goal: rebuild the app to match the reference design (light + dark, purple gradient, floating
bottom bar) with every feature shown in it. Work is split into small parts. Each part is
pushed on its own and built by GitHub Actions, so a broken part is easy to find.

Install the latest build:
https://github.com/Phaneendraaa/taskmanager/releases/download/latest/TaskManager.apk

Legend: DONE = pushed and building, WIP = being worked on, TODO = not started

| # | Part | Status |
|---|------|--------|
| 1 | Design system, data layer, app shell (palette, icons, hero art, components, model, floating nav) | WIP |
| 2 | Home screen (greeting hero, date strip, category chips, task cards, search) | TODO |
| 3 | Create / Edit task (date, time, repeat, category, priority, sub-tasks, attachments, voice notes) | TODO |
| 4 | Task detail (chips, info, sub-tasks, attachments, Mark as Complete, undo delete) | TODO |
| 5 | Calendar (month grid with task dots, day list) | TODO |
| 6 | Progress (Week/Month/Year, ring, stats, daily goal, tasks by category) | TODO |
| 7 | Profile and Habits (theme, daily goal, name, daily habits with streaks) | TODO |
| 8 | Final polish, README, release check | TODO |

## Part details

### 1. Design system, data layer, app shell
- `Theme.kt` light and dark palettes, `Icons.kt` icons drawn in code, `Hero.kt` sunrise illustration
- `Components.kt` shared pieces (cards, check circle, gradient button, chips, fields, heatmap)
- `Models.kt` / `AppViewModel.kt`: categories, repeat, sub-tasks, attachments, daily goal, theme, storage
  (old saved tasks still load)
- `Navigation.kt` floating bottom bar with raised + button

### Notes
- Everything is stored on the device only.
- Repeating tasks create their next occurrence when completed.
