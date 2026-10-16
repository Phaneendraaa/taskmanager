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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppRoot(vm: AppViewModel) {
    val p = LocalPalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = p.bg,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = { FloatingNav(tab, { tab = it }, onAdd = {}) }
    ) { padding ->
        // Screens arrive in the next parts; this shows the new look while they are built.
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
                Txt(
                    listOf("Home", "Calendar", "Stats", "Profile")[tab],
                    size = 28.sp,
                    weight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(8.dp))
                Txt("This screen is being built.", size = 15.sp, color = p.muted)
            }
        }
    }
}
