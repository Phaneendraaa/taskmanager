# forge: research and product spec

forge is for focused adults and students. The goal is an app people keep using because it helps
them, not because it nags them. This file records what the research says and what we build from it.

## What the evidence says

| Finding | Source | What it means for the app |
|---|---|---|
| A habit takes about 66 days on average to feel automatic, with wide variation. | [UCL, Lally et al.](https://www.ucl.ac.uk/news/2009/aug/how-long-does-it-take-form-habit) | Show progress on a 66 day arc, not a 21 day myth. Mark a habit "Automatic" once it is solid. |
| Missing one day did not significantly affect habit formation. Very inconsistent behaviour did. | same | Never reset to zero after one miss. Measure consistency, not perfection. |
| If-then plans ("After X, I will Y") help goal attainment: mean d = 0.65 in the 2006 meta-analysis (94 studies); the 2025 update (642 tests) finds a smaller but real effect, stronger for true if-then wording and when the plan is rehearsed. | [Gollwitzer and Sheeran, summarised here](https://yukaichou.com/gamification-analysis/implementation-intentions-if-then-planning-gollwitzer/) | Every habit has a cue: "After I ___, I will ___". Vague cues are flagged. Plans are visible on the main screen. |
| Streaks lift engagement, but when a streak breaks motivation can collapse and users may quit. Freezes, grace and milestone resets reduce this. | [Mind the Product](https://www.mindtheproduct.com/designing-streaks-for-long-term-user-growth/) | Streaks are secondary. A "never miss twice" rule gives one grace day. Missed days are logged quietly, with an easy recovery. |
| In a study of the Lift habit app, reminders and streaks worked but created dependency: when people stopped using the app, they stopped the behaviour. | [Bristol/Northumbria, "Don't kick the habit"](https://research-information.bris.ac.uk/en/publications/dont-kick-the-habit-the-role-of-dependency-in-habit-formation-app/) | Design for graduation. When a habit is established the app backs off: fewer prompts, quieter row. |
| Tiny versions of a behaviour, anchored to an existing routine, are easier to start (Fogg, Tiny Habits). | [Tiny Habits summary](https://themeaningmovement.com/tiny-habits) | Each habit has an optional minimum version. Doing the minimum counts as showing up. |

## Product decisions

1. **Calm, adult design.** Ink on paper in light mode, graphite in dark mode, one restrained green
   signal for completion. No bright pastels, no cartoon icons. Hairline borders, generous spacing,
   an editorial serif only for large numbers and titles.
2. **Four tabs, nothing else:** Today, Habits, Focus, Insights. Settings from the top corner.
   The calendar is removed. Upcoming deadlines appear on Today instead.
3. **Plan, do, review** is the daily loop:
   - Plan: pick up to three priorities for today.
   - Do: tick habits by their cue, run focus sessions on a priority.
   - Review: a 20 second evening check-in (rate the day, one line).
4. **Habits have a cue, a minimum version and an identity line.** "After I pour my morning coffee,
   I will read 10 pages. Minimum: 1 page. I am a reader."
5. **Consistency over streaks.** The headline number is the 30 day consistency score. The streak
   uses the never-miss-twice rule.
6. **Recovery, not guilt.** After a miss the app shows "Don't miss twice", not a broken streak.
7. **Graduation.** A habit done on 85 percent of due days over 66 days becomes Automatic.
8. **Insights that say something.** Best time of day, best weekday, trend against the last 30 days,
   and a weekly review written from the user's own data.
9. **Notifications that do work.** Done and Snooze buttons on the notification, an evening
   "at risk" nudge, a morning plan nudge, and a timer-finished alert that works with the screen off.
10. **Everything stays on the phone.** Backup and restore to a file.

## Delivery plan (each part is pushed on its own)

R0 research, R1 screenshot harness, R2 design system and navigation, R3 data model,
R4 Today, R5 Habits, R6 Focus, R7 Insights, R8 notifications, R9 onboarding, settings and polish.
