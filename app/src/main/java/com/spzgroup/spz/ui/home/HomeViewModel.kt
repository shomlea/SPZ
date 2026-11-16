package com.spzgroup.spz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.data.Milestone
import com.spzgroup.spz.timer.FocusMode
import com.spzgroup.spz.timer.FocusTimer
import com.spzgroup.spz.timer.FocusTimerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bookCount: Int = 0,
    val readingMinutes: Int = 0,
    val listeningMinutes: Int = 0,
    val readingGoal: Int = 0, // 0 = no goal set
    val listeningGoal: Int = 0
)

class HomeViewModel(
    private val repository: AppRepository,
    private val focusTimer: FocusTimer
) : ViewModel() {

    // Merges the book count, today's progress and the daily goals into one state
    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.bookCount,
            repository.todayProgress(),
            repository.milestones
        ) { count, progress, milestones ->
            HomeUiState(
                bookCount = count,
                readingMinutes = progress?.readingMinutes ?: 0,
                listeningMinutes = progress?.listeningMinutes ?: 0,
                readingGoal = milestones.find { it.type == Milestone.READING }?.dailyGoalMinutes ?: 0,
                listeningGoal = milestones.find { it.type == Milestone.LISTENING }?.dailyGoalMinutes ?: 0
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val timerState: StateFlow<FocusTimerState> = focusTimer.state

    fun addReadingMinutes() {
        viewModelScope.launch { repository.addReadingMinutes(5) }
    }

    fun setGoals(readingMinutes: Int, listeningMinutes: Int) {
        viewModelScope.launch {
            repository.setDailyGoal(Milestone.READING, readingMinutes)
            repository.setDailyGoal(Milestone.LISTENING, listeningMinutes)
        }
    }

    fun startTimer(mode: FocusMode, minutes: Int) = focusTimer.start(mode, minutes)

    fun stopTimer() = focusTimer.stop()

    fun dismissTimerPopup() = focusTimer.dismissFinished()
}