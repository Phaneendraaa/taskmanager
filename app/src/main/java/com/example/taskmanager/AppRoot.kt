package com.example.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AppRoot(vm: AppViewModel) {
    val p = LocalPalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Keep "today" and overdue states fresh while the app stays open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.refreshNow()
        }
    }

    Scaffold(
        containerColor = p.bg,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = { FloatingNav(tab, { tab = it }, onAdd = {}) }
    ) { padding ->
        when (tab) {
            0 -> HomeScreen(
                vm = vm,
                padding = padding,
                onOpenTask = {},
                onOpenProfile = { tab = 3 },
                onOpenHabits = {}
            )
            else -> ComingSoon(listOf("Home", "Calendar", "Stats", "Profile")[tab], padding)
        }
    }
}

@Composable
private fun ComingSoon(title: String, padding: androidx.compose.foundation.layout.PaddingValues) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())
            .background(p.bg)
    ) {
        HeroArt(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(340.dp))
            Txt(title, size = 28.sp, weight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Txt("This screen is being built.", size = 15.sp, color = p.muted)
        }
    }
}
