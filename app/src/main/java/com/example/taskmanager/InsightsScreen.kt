package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(vm: AppViewModel, padding: PaddingValues) {
    val now = vm.now
    val today = now.toLocalDate().toEpochDay()
    val tasks = vm.tasks.toList()
    val habits = vm.habits.toList()

    val done = tasks.filter { it.done }
    val perDay = (6 downTo 0).map { off ->
        val d = today - off
        d to done.count { t ->
            val c = t.completedAt
            c != null && epochDayOf(c) == d
        }
    }
    val doneThisWeek = perDay.sumOf { it.second }

    val onTimeFlags = done.mapNotNull { t ->
        val d = t.dueDay
        val c = t.completedAt
        if (d != null && c != null) epochDayOf(c) <= d else null
    }
    val onTimeText = if (onTimeFlags.isEmpty()) "—" else "${onTimeFlags.count { it } * 100 / onTimeFlags.size}%"
    val overdueNow = tasks.count { it.isOverdue(now) }

    val habitChecks = habits.sumOf { h -> (0..6).count { off -> (today - off) in h.doneDays } }
    val consistencyText = if (habits.isEmpty()) "—" else "${habitChecks * 100 / (habits.size * 7)}%"
    val bestStreak = habits.maxOfOrNull { it.currentStreak(today) } ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        ScreenHeader("Insights", now)

        Row(Modifier.fillMaxWidth()) {
            Stat(doneThisWeek.toString(), "Done this week", Modifier.weight(1f))
            Stat(done.size.toString(), "Done all time", Modifier.weight(1f))
        }
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat(onTimeText, "Finished on time", Modifier.weight(1f))
            Stat(overdueNow.toString(), "Overdue now", Modifier.weight(1f))
        }
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat(consistencyText, "Habit consistency", Modifier.weight(1f))
            Stat(bestStreak.toString(), "Best live streak", Modifier.weight(1f))
        }

        Spacer(Modifier.height(36.dp))
        Hairline()
        Spacer(Modifier.height(24.dp))
        SmallCaps("Tasks completed · last 7 days")
        Spacer(Modifier.height(20.dp))
        WeekBars(perDay, today)

        Spacer(Modifier.height(36.dp))
        Hairline()
        Spacer(Modifier.height(24.dp))
        SmallCaps("Habits · this week")
        Spacer(Modifier.height(16.dp))
        if (habits.isEmpty()) {
            Text(
                "Add a habit to see how consistent you are.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            habits.forEach { h ->
                val n = (0..6).count { off -> (today - off) in h.doneDays }
                Row(Modifier.fillMaxWidth()) {
                    Text(h.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "$n / 7",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                ThinProgress(n / 7f)
                Spacer(Modifier.height(18.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Hairline()
        Spacer(Modifier.height(24.dp))
        SmallCaps("Appearance")
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Light", vm.themeMode == "light", { vm.setTheme("light") })
            Chip("Dark", vm.themeMode == "dark", { vm.setTheme("dark") })
            Chip("System", vm.themeMode == "system", { vm.setTheme("system") })
        }

        Spacer(Modifier.height(36.dp))
        Text(
            "Everything is stored on this device only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun WeekBars(values: List<Pair<Long, Int>>, today: Long) {
    val ink = MaterialTheme.colorScheme.onSurface
    val soft = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = (values.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    Row(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { (day, n) ->
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (n > 0) n.toString() else " ",
                    style = MaterialTheme.typography.labelMedium,
                    color = muted
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(10.dp)
                        .height((84.dp * (n.toFloat() / maxValue)).coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (n > 0) ink else soft)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = dayInitial(day),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                    color = if (day == today) ink else muted
                )
            }
        }
    }
}
