package com.spzgroup.spz.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.data.Book
import com.spzgroup.spz.timer.FocusTimer
import com.spzgroup.spz.timer.FocusTimerState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

// Shared by the PDF and EPUB readers (the audio player has its own ViewModel).
class ReaderViewModel(
    private val repository: AppRepository,
    private val focusTimer: FocusTimer
) : ViewModel() {

    // Reading time that is not yet added to daily progress (less than a minute)
    private var pendingMs = 0L

    // The reading-mode timer, shown in a banner above the reader
    val timerState: StateFlow<FocusTimerState> = focusTimer.state

    // Reading mode counts down only while a reader is open
    fun setReaderOpen(open: Boolean) = focusTimer.setReaderOpen(open)

    // Loads the book and remembers that it was just opened
    suspend fun openBook(bookId: Long): Book? {
        val book = repository.getBook(bookId) ?: return null
        repository.markOpened(bookId)
        return book
    }

    suspend fun loadEpubChapters(book: Book): List<File> = repository.loadEpubChapters(book)

    fun saveProgress(bookId: Long, position: Long, totalUnits: Long) {
        viewModelScope.launch { repository.saveProgress(bookId, position, totalUnits) }
    }

    // Adds whole minutes to today's progress and keeps the leftover seconds for next time
    fun addReadingTime(ms: Long) {
        pendingMs += ms
        val minutes = (pendingMs / 60_000).toInt()
        if (minutes > 0) {
            pendingMs -= minutes * 60_000L
            viewModelScope.launch { repository.addReadingMinutes(minutes) }
        }
    }
}