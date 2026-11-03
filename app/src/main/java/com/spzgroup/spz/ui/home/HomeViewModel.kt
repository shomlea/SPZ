package com.spzgroup.spz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spzgroup.spz.data.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bookCount: Int = 0,
    val readingMinutes: Int = 0,
    val listeningMinutes: Int = 0
)

class HomeViewModel(private val repository: AppRepository) : ViewModel() {

    // Merges the book count and today's progress into one state for the screen
    val uiState: StateFlow<HomeUiState> =
        combine(repository.bookCount, repository.todayProgress()) { count, progress ->
            HomeUiState(
                bookCount = count,
                readingMinutes = progress?.readingMinutes ?: 0,
                listeningMinutes = progress?.listeningMinutes ?: 0
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun addReadingMinutes() {
        viewModelScope.launch { repository.addReadingMinutes(5) }
    }
}