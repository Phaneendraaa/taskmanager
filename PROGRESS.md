# Forge redesign progress

Goal: rebuild the app as "forge", a habits-first app in the Sage theme (with a matching dark
theme), following the second reference design. The earlier violet task-manager redesign is
finished and is kept as the base. Work is split into small parts, each pushed on its own and
built by GitHub Actions.

Install the latest build:
https://github.com/Phaneendraaa/taskmanager/releases/download/latest/TaskManager.apk

Legend: DONE = pushed and the build passed, PUSHED = pushed, build being checked, TODO = not started

| # | Part | Status |
|---|------|--------|
| F1 | Sage light and dark theme, habit model (category, time, days), focus and onboarding storage | DONE |
| F2 | Onboarding: welcome page and lifestyle questions that build a starter plan | DONE |
| F3 | Home (5/7 ring, stat chips, Habits / Tasks switch, Today list) and new bottom bar | DONE |
| F4 | Habits tab (categories, Morning / Afternoon / Evening), Add Habit / Task, Quick Add | PUSHED |
| F5 | Habit details (Overview, History, Edit) | TODO |
| F6 | Focus timer (Focus, Short Break, Long Break) | TODO |
| F7 | Stats (Overview, Habits, Tasks, Focus, heatmap, categories, focus timeline) | TODO |
| F8 | Settings, reminders, polish, README, release | TODO |

Earlier work (violet task manager, parts 1 to 8) is in git history before the "Forge" commits.
