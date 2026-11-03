package com.spzgroup.spz.ui.reader

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.data.Book
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AudioUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0, // 0 until the length is known
    val error: Boolean = false
)

// Owns the ExoPlayer, so the audio does not depend on the screen being drawn.
class AudioPlayerViewModel(
    private val repository: AppRepository,
    appContext: Context
) : ViewModel() {

    private val player = ExoPlayer.Builder(appContext).build()
    private var currentBookId: Long? = null
    private var tickerJob: Job? = null

    // Time spent actually playing that is not yet added to daily progress
    private var listenedMs = 0L

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publish()
                if (!isPlaying) saveProgress()
            }

            override fun onPlayerError(error: PlaybackException) {
                currentBookId = null // so opening the book again tries from scratch
                _uiState.update { it.copy(isPlaying = false, error = true) }
            }
        })
    }

    // Prepares the book. Opening the same book again keeps the current playback.
    fun open(book: Book) {
        if (currentBookId != book.id) {
            currentBookId = book.id
            // A finished book starts again from the beginning
            val finished = book.totalUnits > 0 && book.position >= book.totalUnits - 1000
            val start = if (finished) 0L else book.position
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(book.filePath))), start)
            player.prepare()
            _uiState.value = AudioUiState(
                title = book.title,
                positionMs = start,
                durationMs = book.totalUnits
            )
        }
        startTicker()
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun skip(deltaMs: Long) {
        val duration = _uiState.value.durationMs
        val max = if (duration > 0) duration else Long.MAX_VALUE
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, max))
        publish()
    }

    // Called when the player screen is left: pause, save the position, count listening time
    fun close() {
        player.pause()
        tickerJob?.cancel()
        saveProgress()

        val minutes = (listenedMs / 60_000).toInt()
        if (minutes > 0) {
            listenedMs -= minutes * 60_000L // keep the leftover seconds for next time
            viewModelScope.launch { repository.addListeningMinutes(minutes) }
        }
    }

    // Updates the screen twice a second and counts how long the audio really played
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (true) {
                delay(500)
                val now = SystemClock.elapsedRealtime()
                if (player.isPlaying) listenedMs += now - last
                last = now
                publish()
            }
        }
    }

    private fun publish() {
        // The player reports a negative length until it knows it
        val duration = if (player.duration > 0) player.duration else _uiState.value.durationMs
        _uiState.update {
            it.copy(
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition,
                durationMs = duration
            )
        }
    }

    private fun saveProgress() {
        val bookId = currentBookId ?: return
        val position = player.currentPosition
        val duration = _uiState.value.durationMs
        viewModelScope.launch { repository.saveProgress(bookId, position, duration) }
    }

    override fun onCleared() {
        player.release()
    }
}