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
fun BottomTabs(tab: Int, onTab: (Int) -> Unit) {
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
