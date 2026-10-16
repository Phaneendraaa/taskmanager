package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Floating rounded bottom bar with a raised gradient + button in the middle. */
@Composable
fun FloatingNav(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(bottom = 8.dp)
            .height(88.dp)
    ) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .cardStyle(p, 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Ic.HOME, "Home", tab == 0, Modifier.weight(1f)) { onTab(0) }
            NavItem(Ic.CALENDAR, "Calendar", tab == 1, Modifier.weight(1f)) { onTab(1) }
            Spacer(Modifier.weight(0.8f))
            NavItem(Ic.CHART, "Stats", tab == 2, Modifier.weight(1f)) { onTab(2) }
            NavItem(Ic.PERSON, "Profile", tab == 3, Modifier.weight(1f)) { onTab(3) }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(64.dp)
                .shadow(14.dp, CircleShape, clip = false, ambientColor = p.accent, spotColor = p.accent)
                .clip(CircleShape)
                .background(p.gradient)
                .border(4.dp, p.bg, CircleShape)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(Ic.PLUS, Color.White, size = 28.dp, stroke = 2.4f)
        }
    }
}

@Composable
private fun NavItem(ic: Ic, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val tint = if (selected) p.accent else p.muted
    Box(
        modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) p.accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppIcon(ic, tint, size = 24.dp, filled = selected && ic == Ic.HOME)
            Spacer(Modifier.height(2.dp))
            Txt(label, size = 11.sp, weight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = tint)
        }
    }
}
