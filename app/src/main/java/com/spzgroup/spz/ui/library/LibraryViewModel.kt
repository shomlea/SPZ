package com.spzgroup.spz.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.data.Book
import com.spzgroup.spz.data.ImportResult
import com.spzgroup.spz.data.Reflection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: AppRepository) : ViewModel() {

    val books: StateFlow<List<Book>> =
        repository.books.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The book whose dialog is open, or null when no dialog is shown
    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    // Every time the selected book changes, we switch to that book's reflections
    @OptIn(ExperimentalCoroutinesApi::class)
    val reflections: StateFlow<List<Reflection>> =
        _selectedBook.flatMapLatest { book ->
            if (book == null) flowOf(emptyList()) else repository.reflectionsFor(book.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Short text shown as a Toast, then cleared
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectBook(book: Book?) {
        _selectedBook.value = book
    }

    fun saveReflection(text: String, rating: Int) {
        val book = _selectedBook.value ?: return
        viewModelScope.launch { repository.addReflection(book.id, text.trim(), rating) }
    }

    fun deleteSelectedBook() {
        val book = _selectedBook.value ?: return
        _selectedBook.value = null
        viewModelScope.launch {
            repository.deleteBook(book)
            _message.value = "\"${book.title}\" was deleted"
        }
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            _message.value = when (repository.importBook(uri)) {
                ImportResult.SUCCESS -> "Book imported"
                ImportResult.DUPLICATE -> "This book is already in your library"
                ImportResult.FAILED -> "Could not import this file"
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}