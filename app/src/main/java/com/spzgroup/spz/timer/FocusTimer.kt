package com.spzgroup.spz.timer

import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FocusMode { READING, LISTENING }

data class FocusTimerState(
    val mode: FocusMode = FocusMode.READING,
    val totalMs: Long = 0,
    val remainingMs: Long = 0,
    val isActive: Boolean = false, // started and not stopped or finished yet
    val isTicking: Boolean = false, // counting down right now
    val finished: Boolean = false // the "time's up" pop-up should be shown
)

// The reading-mode timer. It lives in SpzApplication, so every screen sees the same one.
class FocusTimer {

    private val scope = MainScope()
    private var loop: Job? = null

    private val _state = MutableStateFlow(FocusTimerState())
    val state: StateFlow<FocusTimerState> = _state.asStateFlow()

    // What decides if the timer may count down
    private var appVisible = false
    private var readerOpen = false
    private var audioPlaying = false

    fun start(mode: FocusMode, minutes: Int) {
        val total = minutes * 60_000L
        _state.value = FocusTimerState(mode = mode, totalMs = total, remainingMs = total, isActive = true)

        loop?.cancel()
        loop = scope.launch {
            var last = SystemClock.elapsedRealtime()
            while (_state.value.isActive) {
                delay(250)
                val now = SystemClock.elapsedRealtime()
                tick(now - last)
                last = now
            }
        }
    }

    fun stop() {
        loop?.cancel()
        _state.value = FocusTimerState()
    }

    fun dismissFinished() {
        _state.update { it.copy(finished = false) }
    }

    // Called by MainActivity when the app goes to the background and back
    fun setAppVisible(visible: Boolean) {
        appVisible = visible
    }

    // Called by the PDF and EPUB readers when they open and close
    fun setReaderOpen(open: Boolean) {
        readerOpen = open
    }

    // Called by the audio player. Only "playing" matters for the timer.
    fun setAudioState(screenOpen: Boolean, playing: Boolean) {
        audioPlaying = playing
    }

    private fun canCount(mode: FocusMode): Boolean {
        if (!appVisible) return false // leaving the app pauses the timer
        return when (mode) {
            FocusMode.READING -> readerOpen // only while a PDF or EPUB is on screen
            FocusMode.LISTENING -> audioPlaying // only while an audiobook is playing
        }
    }

    private fun tick(elapsedMs: Long) {
        val current = _state.value
        if (!current.isActive) return

        val ticking = canCount(current.mode)
        val remaining = if (ticking) current.remainingMs - elapsedMs else current.remainingMs

        _state.value = if (remaining <= 0) {
            current.copy(remainingMs = 0, isActive = false, isTicking = false, finished = true)
        } else {
            current.copy(remainingMs = remaining, isTicking = ticking)
        }
    }
}