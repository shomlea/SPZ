package com.spzgroup.spz.ui.reminders

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spzgroup.spz.data.Reminder
import com.spzgroup.spz.reminders.ReminderNotifications
import java.util.Locale

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun RemindersScreen(viewModel: RemindersViewModel, modifier: Modifier = Modifier) {
    val reminders by viewModel.reminders.collectAsState()
    val context = LocalContext.current

    // null = no dialog open. A reminder with id 0 means "new reminder".
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var notificationsAllowed by remember { mutableStateOf(ReminderNotifications.canNotify(context)) }

    // Android 13+ asks the user before an app can show notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsAllowed = granted }

    val askForNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reminders", style = MaterialTheme.typography.headlineMedium)

        if (!notificationsAllowed) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notifications are off, so your reminders can't appear.")
                    TextButton(onClick = askForNotifications) { Text("Allow notifications") }
                }
            }
        }

        Button(
            onClick = {
                askForNotifications()
                editing = Reminder(
                    label = "Time to read",
                    type = "READING",
                    hour = 21,
                    minute = 0,
                    days = Reminder.EVERY_DAY
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add reminder")
        }

        if (reminders.isEmpty()) {
            Text("No reminders yet. Add one for your commute or before bed.")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onClick = { editing = reminder },
                    onToggle = { enabled ->
                        if (enabled) askForNotifications()
                        viewModel.setEnabled(reminder, enabled)
                    }
                )
            }
        }
    }

    editing?.let { reminder ->
        ReminderDialog(
            reminder = reminder,
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(reminder)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatClock(reminder.hour, reminder.minute), style = MaterialTheme.typography.headlineSmall)
                Text(reminder.label)
                Text(
                    "${typeLabel(reminder.type)} · ${daysLabel(reminder.days)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = reminder.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ReminderDialog(
    reminder: Reminder,
    onSave: (Reminder) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Form state lives only while the dialog is open
    var label by remember { mutableStateOf(reminder.label) }
    var type by remember { mutableStateOf(reminder.type) }
    var hour by remember { mutableIntStateOf(reminder.hour) }
    var minute by remember { mutableIntStateOf(reminder.minute) }
    var days by remember { mutableIntStateOf(reminder.days) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reminder.id == 0L) "New reminder" else "Edit reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeButton("Reading", type == "READING", Modifier.weight(1f)) { type = "READING" }
                    TypeButton("Listening", type == "LISTENING", Modifier.weight(1f)) { type = "LISTENING" }
                }

                // Android's own clock picker
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            hour = h
                            minute = m
                        }, hour, minute, true).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Time: ${formatClock(hour, minute)}")
                }

                Text("Days", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (dayIndex in 0..6) {
                        DayToggle(
                            letter = DAY_LETTERS[dayIndex],
                            selected = Reminder.isDaySelected(days, dayIndex),
                            // xor flips the bit of this day on or off
                            onClick = { days = days xor (1 shl dayIndex) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        reminder.copy(
                            label = label.trim().ifEmpty { "Reminder" },
                            type = type,
                            hour = hour,
                            minute = minute,
                            days = days,
                            enabled = true
                        )
                    )
                },
                enabled = days != 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                // Only a reminder that is already saved can be deleted
                if (reminder.id != 0L) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun TypeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}

// A small round button for one day of the week
@Composable
private fun DayToggle(letter: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Text(letter, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}

private fun formatClock(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)

private fun typeLabel(type: String): String = if (type == "LISTENING") "Listening" else "Reading"

// "Every day", "Weekdays", "Weekends" or a list like "Mon, Wed, Fri"
private fun daysLabel(days: Int): String = when (days) {
    Reminder.EVERY_DAY -> "Every day"
    0b0011111 -> "Weekdays"
    0b1100000 -> "Weekends"
    else -> (0..6).filter { Reminder.isDaySelected(days, it) }.joinToString(", ") { DAY_NAMES[it] }
}