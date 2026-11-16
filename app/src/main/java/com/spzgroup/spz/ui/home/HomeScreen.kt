package com.spzgroup.spz.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spzgroup.spz.timer.FocusMode
import com.spzgroup.spz.timer.FocusTimerState
import com.spzgroup.spz.ui.formatTime

@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val timer by viewModel.timerState.collectAsState()
    var editingGoals by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your daily space", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Books in your library", style = MaterialTheme.typography.labelLarge)
                Text("${state.bookCount}", style = MaterialTheme.typography.headlineSmall)
            }
        }

        // --- Milestones ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Daily milestones",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { editingGoals = true }) { Text("Edit goals") }
        }
        GoalCard(label = "Reading", minutes = state.readingMinutes, goal = state.readingGoal)
        GoalCard(label = "Listening", minutes = state.listeningMinutes, goal = state.listeningGoal)

        OutlinedButton(
            onClick = { viewModel.addReadingMinutes() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add 5 reading minutes")
        }

        // --- Reading mode ---
        Text("Reading mode", style = MaterialTheme.typography.titleMedium)
        ReadingModeCard(
            timer = timer,
            onStart = { mode, minutes -> viewModel.startTimer(mode, minutes) },
            onStop = { viewModel.stopTimer() }
        )
    }

    if (editingGoals) {
        EditGoalsDialog(
            readingGoal = state.readingGoal,
            listeningGoal = state.listeningGoal,
            onSave = { reading, listening ->
                viewModel.setGoals(reading, listening)
                editingGoals = false
            },
            onDismiss = { editingGoals = false }
        )
    }
}

@Composable
private fun GoalCard(label: String, minutes: Int, goal: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (goal == 0) {
                Text("$minutes min today · no daily goal")
            } else {
                Text("$minutes / $goal min today", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(
                    progress = { (minutes.toFloat() / goal).coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (minutes >= goal) {
                    Text("Goal reached, well done!", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ReadingModeCard(
    timer: FocusTimerState,
    onStart: (FocusMode, Int) -> Unit,
    onStop: () -> Unit
) {
    // Choices kept while the user moves between tabs
    var mode by rememberSaveable { mutableStateOf(FocusMode.READING) }
    var minutes by rememberSaveable { mutableIntStateOf(25) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timer.isActive) {
                val title = if (timer.mode == FocusMode.READING) "Reading session" else "Listening session"
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(formatTime(timer.remainingMs), style = MaterialTheme.typography.displaySmall)
                Text(timerStatus(timer))
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text("Stop")
                }
            } else {
                // Explain the rules up front, so a paused timer is not a surprise
                Text(
                    "Reading counts down only while a PDF or EPUB is open in the reader. " +
                            "Listening counts down only while an audiobook is playing. " +
                            "Both pause when you leave the app."
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceButton("Reading", mode == FocusMode.READING, Modifier.weight(1f)) {
                        mode = FocusMode.READING
                    }
                    ChoiceButton("Listening", mode == FocusMode.LISTENING, Modifier.weight(1f)) {
                        mode = FocusMode.LISTENING
                    }
                }

                MinutesStepper(label = "Duration", value = minutes, min = 5, max = 120) {
                    minutes = it
                }

                Button(onClick = { onStart(mode, minutes) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start")
                }
            }
        }
    }
}

// Tells the user why the timer is paused and what to do to keep it going
private fun timerStatus(timer: FocusTimerState): String = when {
    timer.isTicking -> "Counting down"
    timer.mode == FocusMode.READING ->
        "Paused. Open a PDF or EPUB from the Library to keep counting."
    else -> "Paused. Play an audiobook from the Library to keep counting."
}

// A filled button when selected, an outlined one otherwise
@Composable
private fun ChoiceButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}

// "-" and "+" buttons that change a number of minutes in steps of 5
@Composable
private fun MinutesStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange((value - 5).coerceAtLeast(min)) }, enabled = value > min) {
                Text("-")
            }
            Text(
                text = if (value == 0) "Off" else "$value min",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            OutlinedButton(onClick = { onChange((value + 5).coerceAtMost(max)) }, enabled = value < max) {
                Text("+")
            }
        }
    }
}

@Composable
private fun EditGoalsDialog(
    readingGoal: Int,
    listeningGoal: Int,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var reading by remember { mutableIntStateOf(readingGoal) }
    var listening by remember { mutableIntStateOf(listeningGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("How many minutes do you want to read and listen each day?")
                MinutesStepper(label = "Reading", value = reading, min = 0, max = 300) { reading = it }
                MinutesStepper(label = "Listening", value = listening, min = 0, max = 300) { listening = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(reading, listening) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}