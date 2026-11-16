package com.spzgroup.spz.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.spzgroup.spz.MainActivity
import com.spzgroup.spz.R
import com.spzgroup.spz.data.Reminder

object ReminderNotifications {

    // A channel's importance can't be changed later, so the louder channel gets a new id
    private const val CHANNEL_ID = "reading_reminders"
    private const val OLD_CHANNEL_ID = "reminders"
    private const val TEST_NOTIFICATION_ID = 999_999

    // Android 8+ needs a channel before showing notifications. Creating it twice is harmless.
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(OLD_CHANNEL_ID)

            // HIGH importance makes the notification pop up on top of the screen
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reading reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun show(context: Context, reminder: Reminder) {
        val text = if (reminder.type == "LISTENING") {
            "Time to listen to your audiobook"
        } else {
            "Time to read a few pages"
        }
        notify(context, reminder.id.toInt(), reminder.label.ifBlank { "SPZ reminder" }, text)
    }

    // Shows a notification right away, to check that notifications work
    fun showTest(context: Context) {
        notify(context, TEST_NOTIFICATION_ID, "Test reminder", "Notifications are working")
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        // On Android 13+ the user has to allow notifications first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)

        // Tapping the notification opens the app
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                ),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // for Android 7, which has no channels
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}