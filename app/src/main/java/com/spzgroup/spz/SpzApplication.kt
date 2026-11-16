package com.spzgroup.spz

import android.app.Application
import com.spzgroup.spz.data.AppDatabase
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.reminders.ReminderNotifications
import com.spzgroup.spz.timer.FocusTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class SpzApplication : Application() {

    val repository: AppRepository by lazy {
        AppRepository(AppDatabase.create(this), this)
    }

    val focusTimer = FocusTimer()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)

        appScope.launch { repository.rescheduleReminders() }
    }
}