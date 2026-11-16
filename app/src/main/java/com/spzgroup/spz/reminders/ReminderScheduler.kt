package com.spzgroup.spz.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.spzgroup.spz.data.Reminder
import java.util.Calendar

// Sets and cancels the alarms that wake up ReminderReceiver.
object ReminderScheduler {

    const val EXTRA_REMINDER_ID = "reminder_id"

    // Used only if Android does not let us set exact alarms
    private const val WINDOW_MS = 10 * 60 * 1000L

    fun schedule(context: Context, reminder: Reminder) {
        cancel(context, reminder.id)
        if (!reminder.enabled || reminder.days == 0) return

        val triggerAt = nextTriggerTime(reminder, System.currentTimeMillis()) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, reminder.id)

        // Exact alarms ring on time, even when the phone is idle.
        // If they are not allowed, Android may deliver the alarm up to 10 minutes late.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_MS, pending)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, reminderId))
    }

    // The next moment (today or later this week) that matches the hour and the chosen days
    fun nextTriggerTime(reminder: Reminder, now: Long): Long? {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check today and the next 7 days
        for (i in 0..7) {
            if (i > 0) calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayIndex = toDayIndex(calendar.get(Calendar.DAY_OF_WEEK))
            if (calendar.timeInMillis > now && Reminder.isDaySelected(reminder.days, dayIndex)) {
                return calendar.timeInMillis
            }
        }
        return null
    }

    // Calendar counts Sunday = 1 ... Saturday = 7, we count Monday = 0 ... Sunday = 6
    private fun toDayIndex(calendarDay: Int): Int = (calendarDay + 5) % 7

    // The same request code (the reminder id) lets us find and cancel the alarm later
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