package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

@Composable
fun TaskCard(
    task: Task,
    today: Long,
    now: LocalDateTime,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val overdue = task.isOverdue(now)
    val minute = task.dueMinute
    val day = task.dueDay
    val whenText = when {
        overdue -> "Overdue"
        minute != null -> formatMinute(minute)
        day != null -> formatDay(day, today)
        else -> "Anytime"
    }
    val subDone = task.subtasks.count { it.done }
    val subText = if (task.subtasks.isNotEmpty()) "  •  $subDone/${task.subtasks.size}" else ""

    Row(
        modifier
            .fillMaxWidth()
            .cardStyle(p, 16.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(task.done, onToggle)
        AppIcon(
            task.iconKind(),
            p.muted,
            size = 21.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(
                task.title,
                size = 16.sp,
                weight = FontWeight.SemiBold,
                color = if (task.done) p.muted else p.text,
                strike = task.done,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(p.muted)
                )
                Spacer(Modifier.width(6.dp))
                Txt(
                    task.category.label + "  •  " + whenText + subText,
                    size = 12.sp,
                    color = if (overdue) StatusRed else p.muted,
                    maxLines = 1
                )
            }
        }
        if (task.priority == 3) {
            AppIcon(Ic.FLAG, StatusRed, size = 20.dp)
            Spacer(Modifier.width(8.dp))
        }
    }
}
