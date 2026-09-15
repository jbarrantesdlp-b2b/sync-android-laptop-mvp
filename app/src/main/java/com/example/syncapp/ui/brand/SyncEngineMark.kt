package com.example.syncapp.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.syncapp.R

/** Reduced mark — SO 3D without wordmark. Launcher, widgets, chrome. */
@Composable
fun SyncEngineMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    glow: Boolean = true
) {
    Image(
        painter = painterResource(id = R.drawable.sync_engine_mark),
        contentDescription = "Sync Engine",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Full lockup — mark + SYNC ENGINE + BY BARRANTES CO. Heroes and about. */
@Composable
fun SyncEngineLockup(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    Image(
        painter = painterResource(id = R.drawable.sync_engine_lockup),
        contentDescription = "SYNC ENGINE by Barrantes Co.",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
