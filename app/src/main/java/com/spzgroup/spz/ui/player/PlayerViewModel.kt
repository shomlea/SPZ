package com.spzgroup.spz.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.data.Book
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(repository: AppRepository) : ViewModel() {

    // The book opened most recently, or null if nothing was opened yet
    val lastBook: StateFlow<Book?> =
        repository.lastOpenedBook.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}