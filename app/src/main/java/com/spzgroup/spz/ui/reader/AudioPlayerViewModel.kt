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
import com.spzgroup.spz.timer.FocusTimer
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
    val error: Boolean = false,
    val sleepMinutes: Int = 0, // 0 = sleep timer off
    val sleepRemainingMs: Long = 0, // time left without activity before the audio pauses
    val pausedBySleepTimer: Boolean = false
)

// Owns the ExoPlayer, so the audio does not depend on the screen being drawn.
class AudioPlayerViewModel(
    private val repository: AppRepository,
    appContext: Context,
    private val focusTimer: FocusTimer
) : ViewModel() {

    private val player = ExoPlayer.Builder(appContext).build()
    private var currentBookId: Long? = null
    private var screenOpen = false
    private var tickerJob: Job? = null

    // Time spent actually playing that is not yet added to daily progress
    private var listenedMs = 0L

    // Sleep timer: how long the audio has been playing without the user touching the screen
    private var idleMs = 0L

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publish()
                // The reading-mode timer needs to know when the audio stops
                focusTimer.setAudioState(screenOpen, isPlaying)
                if (isPlaying) {
                    _uiState.update { it.copy(pausedBySleepTimer = false) }
                } else {
                    saveProgress()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                currentBookId = null // so opening the book again tries from scratch
                _uiState.update { it.copy(isPlaying = false, error = true) }
            }
        })

        // When the reading-mode timer ends, the audiobook pauses too
        viewModelScope.launch {
            focusTimer.state.collect { timer ->
                if (timer.finished) player.pause()
            }
        }
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
            _uiState.update {
                it.copy(
                    title = book.title,
                    isPlaying = false,
                    positionMs = start,
                    durationMs = book.totalUnits,
                    error = false,
                    pausedBySleepTimer = false
                )
            }
        }
        screenOpen = true
        idleMs = 0
        focusTimer.setAudioState(screenOpen = true, playing = player.isPlaying)
        startTicker()
    }

    fun togglePlay() {
        idleMs = 0
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        idleMs = 0
        player.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun skip(deltaMs: Long) {
        idleMs = 0
        val duration = _uiState.value.durationMs
        val max = if (duration > 0) duration else Long.MAX_VALUE
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, max))
        publish()
    }

    // 0 turns the sleep timer off
    fun setSleepTimer(minutes: Int) {
        idleMs = 0
        _uiState.update {
            it.copy(sleepMinutes = minutes, sleepRemainingMs = minutes * 60_000L)
        }
    }

    // Any touch on the player screen counts as activity and restarts the sleep countdown
    fun onUserActivity() {
        idleMs = 0
    }

    // Called when the player screen is left: pause, save the position, count listening time
    fun close() {
        player.pause()
        tickerJob?.cancel()
        screenOpen = false
        idleMs = 0
        focusTimer.setAudioState(screenOpen = false, playing = false)
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
                if (player.isPlaying) {
                    listenedMs += now - last
                    checkSleepTimer(now - last)
                }
                last = now
                publish()
            }
        }
    }

    private fun checkSleepTimer(elapsedMs: Long) {
        val limit = _uiState.value.sleepMinutes * 60_000L
        if (limit == 0L) return

        idleMs += elapsedMs
        if (idleMs >= limit) {
            idleMs = 0
            player.pause()
            _uiState.update { it.copy(pausedBySleepTimer = true) }
        }
    }

    private fun publish() {
        // The player reports a negative length until it knows it
        val duration = if (player.duration > 0) player.duration else _uiState.value.durationMs
        val sleepLimit = _uiState.value.sleepMinutes * 60_000L
        _uiState.update {
            it.copy(
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition,
                durationMs = duration,
                sleepRemainingMs = (sleepLimit - idleMs).coerceAtLeast(0L)
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