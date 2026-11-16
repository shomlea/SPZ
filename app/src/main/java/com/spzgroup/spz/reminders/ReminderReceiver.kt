package com.spzgroup.spz.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spzgroup.spz.SpzApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val appContext = context.applicationContext
        val repository = (appContext as SpzApplication).repository

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = repository.getReminder(reminderId)
                if (reminder != null && reminder.enabled) {
                    ReminderNotifications.show(appContext, reminder)
                    ReminderScheduler.schedule(appContext, reminder)
                }
            } finally {
                pending.finish()
            }
        }
    }
}