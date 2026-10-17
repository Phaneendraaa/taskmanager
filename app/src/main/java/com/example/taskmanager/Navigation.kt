package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class TabSpec(val ic: Ic, val label: String)

private val TABS = listOf(
    TabSpec(Ic.HOME, "Today"),
    TabSpec(Ic.REPEAT, "Habits"),
    TabSpec(Ic.TIMER, "Focus"),
    TabSpec(Ic.CHART, "Insights")
)

/** Flat bottom bar with four tabs. Nothing floats, nothing is raised. */
@Composable
fun BottomTabs(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(p.bg)
    ) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp)
        ) {
            TABS.forEachIndexed { i, spec ->
                if (i == 2) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onAdd),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(p.inverse),
                            contentAlignment = Alignment.Center
                        ) { AppIcon(Ic.PLUS, p.onInverse, size = 24.dp, stroke = 2.4f) }
                    }
                }
                val on = i == tab
                val tint = if (on) p.text else p.muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (on) p.text else p.bg)
                    )
                    Spacer(Modifier.height(9.dp))
                    AppIcon(spec.ic, tint, size = 23.dp, stroke = if (on) 2.1f else 1.7f)
                    Spacer(Modifier.height(3.dp))
                    Txt(
                        spec.label,
                        size = 11.sp,
                        weight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Small sheet over the app that asks what to create. */
@Composable
fun AddSheet(onTask: () -> Unit, onHabit: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(p.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Txt("Create", size = 22.sp, weight = FontWeight.SemiBold, serif = true)
            Spacer(Modifier.height(4.dp))
            Txt("What would you like to add?", size = 14.sp, color = p.muted)
            Spacer(Modifier.height(16.dp))
            AddOption(Ic.NOTES, "New task", "Something to get done, with a date if you like", onTask)
            Spacer(Modifier.height(10.dp))
            AddOption(Ic.REPEAT, "New habit", "Something to repeat, tied to your day", onHabit)
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AddOption(ic: Ic, title: String, sub: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().cardStyle(p).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(p.field),
            contentAlignment = Alignment.Center
        ) { AppIcon(ic, p.text, size = 22.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, size = 16.sp, weight = FontWeight.SemiBold)
            Txt(sub, size = 12.sp, color = p.muted, maxLines = 2)
        }
        AppIcon(Ic.CHEVRON_RIGHT, p.muted, size = 18.dp)
    }
}
