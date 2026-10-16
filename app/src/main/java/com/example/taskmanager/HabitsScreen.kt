package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HabitsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onDeleted: (Habit) -> Unit
) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    var newName by remember { mutableStateOf("") }

    fun add() {
        vm.addHabit(newName)
        newName = ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Ic.BACK, onClick = onBack)
            Spacer(Modifier.size(14.dp))
            Column {
                Txt("Daily habits", size = 24.sp, weight = FontWeight.ExtraBold)
                val doneNow = vm.habits.count { today in it.doneDays }
                Txt("$doneNow of ${vm.habits.size} done today", size = 13.sp, color = p.muted)
            }
        }
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth().cardStyle(p, 20.dp).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlainField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = "New habit, e.g. Read 20 pages",
                modifier = Modifier.weight(1f),
                singleLine = true,
                imeAction = ImeAction.Done,
                onDone = { add() }
            )
            RoundIconButton(Ic.PLUS, onClick = { add() }, bg = p.field, tint = p.accent)
        }
        Spacer(Modifier.height(16.dp))

        if (vm.habits.isEmpty()) {
            Spacer(Modifier.height(30.dp))
            Txt(
                "Habits repeat every day. Add one above and tick it off each day to build a streak.",
                size = 14.sp,
                color = p.muted
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.habits.forEach { h ->
                Column(Modifier.fillMaxWidth().cardStyle(p, 22.dp).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CheckCircle(today in h.doneDays, onClick = { vm.toggleHabitDay(h.id, today) })
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Txt(h.name, size = 16.sp, weight = FontWeight.SemiBold)
                            Txt(
                                "${h.currentStreak(today)} day streak  •  best ${h.bestStreak()}  •  ${h.rate(today)}% this month",
                                size = 12.sp,
                                color = p.muted
                            )
                        }
                        RoundIconButton(
                            Ic.TRASH,
                            onClick = { vm.deleteHabit(h.id)?.let(onDeleted) },
                            size = 38.dp,
                            tint = p.muted,
                            bordered = false
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Heatmap(h.doneDays, today)
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}
