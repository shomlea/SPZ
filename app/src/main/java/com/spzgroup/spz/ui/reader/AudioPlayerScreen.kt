package com.spzgroup.spz.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spzgroup.spz.data.Book
import com.spzgroup.spz.ui.AppViewModelFactory
import com.spzgroup.spz.ui.formatTime

@Composable
fun AudioPlayerScreen(book: Book, factory: AppViewModelFactory, onClose: () -> Unit) {
    val viewModel: AudioPlayerViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    // Position of the slider while the user drags it, null otherwise
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(book.id) { viewModel.open(book) }

    // Leaving the screen pauses the audio and saves the position
    DisposableEffect(book.id) {
        onDispose { viewModel.close() }
    }

    // The slider needs a range that is never empty
    val maxPosition = maxOf(state.durationMs.toFloat(), 1f)
    val shownPosition = (dragPosition ?: state.positionMs.toFloat()).coerceIn(0f, maxPosition)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onClose) { Text("Back") }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = book.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        if (book.author.isNotEmpty()) {
            Text(book.author, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (state.error) {
            Text("Could not play this file", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Slider(
            value = shownPosition,
            onValueChange = { dragPosition = it },
            onValueChangeFinished = {
                dragPosition?.let { viewModel.seekTo(it.toLong()) }
                dragPosition = null
            },
            valueRange = 0f..maxPosition,
            enabled = state.durationMs > 0
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(formatTime(shownPosition.toLong()))
            Text(formatTime(state.durationMs))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { viewModel.skip(-15_000) }) { Text("-15 s") }
            Button(onClick = { viewModel.togglePlay() }) {
                Text(if (state.isPlaying) "Pause" else "Play")
            }
            Button(onClick = { viewModel.skip(15_000) }) { Text("+15 s") }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}