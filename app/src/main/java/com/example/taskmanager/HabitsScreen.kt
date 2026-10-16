package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HabitsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onAdd: () -> Unit
) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    // -1 = all, otherwise the HabitCategory ordinal.
    var filter by rememberSaveable { mutableIntStateOf(-1) }
    val shown = vm.habits
        .filter { filter < 0 || it.category.ordinal == filter }
        .sortedWith(compareBy({ it.timeMinute ?: Int.MAX_VALUE }, { it.name }))

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.glow, p.bg, p.bg)))
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Ic.BACK, onClick = onBack)
            Spacer(Modifier.size(14.dp))
            Txt("Habits", size = 30.sp, weight = FontWeight.Bold, serif = true, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(p.accent)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) { AppIcon(Ic.PLUS, Color.White, size = 24.dp, stroke = 2.4f) }
        }

        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("All ${vm.habits.size}", filter < 0) { filter = -1 }
            HabitCategory.values().forEach { c ->
                val n = vm.habits.count { it.category == c }
                if (n > 0) Chip("${c.label} $n", filter == c.ordinal) { filter = c.ordinal }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            if (shown.isEmpty()) {
                Spacer(Modifier.height(60.dp))
                Txt(
                    "No habits here yet.\nTap + to add one.",
                    Modifier.fillMaxWidth(),
                    size = 15.sp,
                    color = p.muted,
                    align = TextAlign.Center
                )
            }
            Period.values().forEach { period ->
                val group = shown.filter { it.period() == period }
                if (group.isNotEmpty()) {
                    val due = group.filter { it.isDue(today) }
                    val done = due.count { today in it.doneDays }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Txt(
                            period.label,
                            size = 18.sp,
                            weight = FontWeight.Bold,
                            serif = true,
                            modifier = Modifier.weight(1f)
                        )
                        Txt("$done/${due.size}", size = 13.sp, color = p.muted)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        group.forEach { h ->
                            val isDue = h.isDue(today)
                            HabitRow(
                                h, today,
                                onToggle = { if (isDue) vm.toggleHabitDay(h.id, today) },
                                onClick = { onOpenHabit(h.id) },
                                modifier = Modifier.alpha(if (isDue) 1f else 0.55f),
                                bar = true,
                                checkAtEnd = true
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
