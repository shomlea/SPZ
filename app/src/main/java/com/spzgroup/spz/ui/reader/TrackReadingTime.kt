package com.spzgroup.spz.ui.reader

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

// Put this inside a reader screen. While the screen is open, reading mode counts down.
// When the screen closes, the time it was open is added to today's reading minutes.
@Composable
fun TrackReadingTime(viewModel: ReaderViewModel) {
    DisposableEffect(Unit) {
        val start = SystemClock.elapsedRealtime()
        viewModel.setReaderOpen(true)

        onDispose {
            viewModel.setReaderOpen(false)
            viewModel.addReadingTime(SystemClock.elapsedRealtime() - start)
        }
    }
}