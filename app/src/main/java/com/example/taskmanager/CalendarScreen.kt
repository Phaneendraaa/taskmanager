package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@Composable
fun CalendarScreen(
    vm: AppViewModel,
    padding: PaddingValues,
    onOpenTask: (String) -> Unit
) {
    val p = LocalPalette.current
    val today = vm.now.toLocalDate().toEpochDay()
    val sel = LocalDate.ofEpochDay(vm.selectedDay)
    // Month shown, as year * 12 + (month - 1). Starts on the selected day's month.
    var monthIndex by rememberSaveable { mutableIntStateOf(sel.year * 12 + sel.monthValue - 1) }
    val ym = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val first = ym.atDay(1)
    val lead = first.dayOfWeek.value - 1 // Monday first
    val total = ((lead + ym.lengthOfMonth() + 6) / 7) * 7

    val dayTasks = vm.tasks
        .filter { it.dueDay == vm.selectedDay }
        .sortedWith(compareBy({ it.done }, { it.dueMinute ?: Int.MAX_VALUE }))

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = padding.calculateBottomPadding() + 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt("Calendar", size = 28.sp, weight = FontWeight.ExtraBold)
                Txt("${MONTH_NAMES[ym.monthValue - 1]} ${ym.year}", size = 14.sp, color = p.muted)
            }
            RoundIconButton(Ic.CHEVRON_LEFT, onClick = { monthIndex -= 1 })
            Spacer(Modifier.size(8.dp))
            RoundIconButton(Ic.CHEVRON_RIGHT, onClick = { monthIndex += 1 })
        }
        Spacer(Modifier.height(18.dp))

        Column(Modifier.fillMaxWidth().cardStyle(p, 24.dp).padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Txt(
                        d,
                        Modifier.weight(1f),
                        size = 12.sp,
                        weight = FontWeight.SemiBold,
                        color = p.muted,
                        align = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            for (row in 0 until total / 7) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val n = row * 7 + col - lead + 1
                        if (n < 1 || n > ym.lengthOfMonth()) {
                            Box(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val day = ym.atDay(n).toEpochDay()
                            val items = vm.tasks.filter { it.dueDay == day }
                            DayCell(
                                n = n,
                                selected = day == vm.selectedDay,
                                isToday = day == today,
                                cats = items.map { it.category }.distinct().take(3),
                                modifier = Modifier.weight(1f)
                            ) { vm.selectedDay = day }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Txt(
            formatDay(vm.selectedDay, today),
            size = 18.sp,
            weight = FontWeight.Bold
        )
        Txt(
            if (dayTasks.isEmpty()) "Nothing planned" else "${dayTasks.size} task" + if (dayTasks.size == 1) "" else "s",
            size = 13.sp,
            color = p.muted
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            dayTasks.forEach { t ->
                TaskCard(
                    task = t,
                    today = today,
                    now = vm.now,
                    onToggle = { vm.toggleTask(t.id) },
                    onClick = { onOpenTask(t.id) }
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    n: Int,
    selected: Boolean,
    isToday: Boolean,
    cats: List<Category>,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    Box(modifier.aspectRatio(1f).padding(2.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        selected -> p.accent
                        isToday -> p.accent.copy(alpha = 0.14f)
                        else -> p.card
                    }
                )
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Txt(
                n.toString(),
                size = 14.sp,
                weight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else p.text
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(5.dp)) {
                cats.forEach { c ->
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else c.color)
                    )
                }
            }
        }
    }
}
