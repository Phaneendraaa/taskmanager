package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HabitsScreen(vm: AppViewModel, padding: PaddingValues, onOpen: (String) -> Unit) {
    val now = vm.now
    val today = now.toLocalDate().toEpochDay()
    val habits = vm.habits.toList()
    val doneToday = habits.count { today in it.doneDays }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
    ) {
        item { ScreenHeader("Habits", now) }

        if (habits.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No habits yet.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to add something you want to do every day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Column(Modifier.padding(bottom = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (doneToday == habits.size) "All done for today." else "$doneToday of ${habits.size} done today",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ThinProgress(doneToday.toFloat() / habits.size)
                }
                Hairline()
            }
            items(habits, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    today = today,
                    onToggle = { day -> vm.toggleHabitDay(habit.id, day) },
                    onOpen = { onOpen(habit.id) }
                )
            }
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, today: Long, onToggle: (Long) -> Unit, onOpen: () -> Unit) {
    val doneToday = today in habit.doneDays
    val streak = habit.currentStreak(today)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleCheck(checked = doneToday, onClick = { onToggle(today) })
            Spacer(Modifier.width(6.dp))
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .padding(vertical = 4.dp)
            ) {
                Text(habit.name, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                val streakText = if (streak == 0) "No streak yet" else if (streak == 1) "1 day streak" else "$streak day streak"
                Text(
                    "$streakText · ${habit.rate(today)}% this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (offset in 6 downTo 0) {
                val day = today - offset
                DayDot(
                    day = day,
                    done = day in habit.doneDays,
                    isToday = offset == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onToggle(day) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Hairline()
    }
}

@Composable
private fun DayDot(day: Long, done: Boolean, isToday: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayInitial(day),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) ink else muted
        )
        Spacer(Modifier.height(5.dp))
        Spacer(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (done) ink else Color.Transparent)
                .border(1.dp, if (done) ink else muted.copy(alpha = 0.5f), CircleShape)
        )
    }
}
