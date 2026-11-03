package com.spzgroup.spz.ui.reader

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

// Put this inside a reader screen. When the screen closes, the time it was
// open is added to today's reading minutes.
@Composable
fun TrackReadingTime(viewModel: ReaderViewModel) {
    DisposableEffect(Unit) {
        val start = SystemClock.elapsedRealtime()
        onDispose {
            viewModel.addReadingTime(SystemClock.elapsedRealtime() - start)
        }
    }
}