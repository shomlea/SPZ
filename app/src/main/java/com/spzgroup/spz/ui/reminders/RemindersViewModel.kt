package com.spzgroup.spz.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.data.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(private val repository: AppRepository) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> =
        repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Works for new reminders (id = 0) and for edited ones
    fun save(reminder: Reminder) {
        viewModelScope.launch { repository.saveReminder(reminder) }
    }

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        save(reminder.copy(enabled = enabled))
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch { repository.deleteReminder(reminder) }
    }
}