package com.spzgroup.spz.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.data.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: AppRepository) : ViewModel() {

    val books: StateFlow<List<Book>> =
        repository.books.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // We keep only the id, so the screen always shows the latest position from the database
    private val _selectedBookId = MutableStateFlow<Long?>(null)
    val selectedBookId: StateFlow<Long?> = _selectedBookId.asStateFlow()

    fun selectBook(bookId: Long) {
        _selectedBookId.value = bookId
    }

    fun nextPage() {
        val bookId = _selectedBookId.value ?: return
        viewModelScope.launch { repository.increasePosition(bookId) }
    }
}