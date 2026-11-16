package com.spzgroup.spz.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spzgroup.spz.SpzApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val repository = (context.applicationContext as SpzApplication).repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.rescheduleReminders()
            } finally {
                pending.finish()
            }
        }
    }
}