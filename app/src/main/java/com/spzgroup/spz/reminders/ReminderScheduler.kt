package com.spzgroup.spz.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.spzgroup.spz.data.Reminder
import java.util.Calendar

object ReminderScheduler {

    const val EXTRA_REMINDER_ID = "reminder_id"


    private const val WINDOW_MS = 10 * 60 * 1000L

    fun schedule(context: Context, reminder: Reminder) {
        cancel(context, reminder.id)
        if (!reminder.enabled || reminder.days == 0) return

        val triggerAt = nextTriggerTime(reminder, System.currentTimeMillis()) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            WINDOW_MS,
            pendingIntent(context, reminder.id)
        )
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, reminderId))
    }

    fun nextTriggerTime(reminder: Reminder, now: Long): Long? {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0..7) {
            if (i > 0) calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayIndex = toDayIndex(calendar.get(Calendar.DAY_OF_WEEK))
            if (calendar.timeInMillis > now && Reminder.isDaySelected(reminder.days, dayIndex)) {
                return calendar.timeInMillis
            }
        }
        return null
    }

    private fun toDayIndex(calendarDay: Int): Int = (calendarDay + 5) % 7

    private fun pendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}