package com.spzgroup.spz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spzgroup.spz.timer.FocusMode
import com.spzgroup.spz.ui.AppViewModelFactory
import com.spzgroup.spz.ui.home.HomeScreen
import com.spzgroup.spz.ui.home.HomeViewModel
import com.spzgroup.spz.ui.library.LibraryScreen
import com.spzgroup.spz.ui.library.LibraryViewModel
import com.spzgroup.spz.ui.player.PlayerScreen
import com.spzgroup.spz.ui.player.PlayerViewModel
import com.spzgroup.spz.ui.reader.ReaderRouter
import com.spzgroup.spz.ui.reminders.RemindersScreen
import com.spzgroup.spz.ui.reminders.RemindersViewModel
import com.spzgroup.spz.ui.theme.SPZTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as SpzApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = AppViewModelFactory(app.repository, applicationContext, app.focusTimer)

        setContent {
            SPZTheme {
                SPZApp(factory)
            }
        }
    }

    // The reading-mode timer only counts while the app is on screen
    override fun onStart() {
        super.onStart()
        app.focusTimer.setAppVisible(true)
    }

    override fun onStop() {
        super.onStop()
        app.focusTimer.setAppVisible(false)
    }
}

@Composable
fun SPZApp(factory: AppViewModelFactory) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    // Id of the book open in a reader or player, or null while we show the tabs
    var openBookId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Created once and kept while the activity lives, so switching tabs keeps their state
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val playerViewModel: PlayerViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val remindersViewModel: RemindersViewModel = viewModel(factory = factory)

    val bookId = openBookId
    if (bookId != null) {
        ReaderRouter(
            bookId = bookId,
            factory = factory,
            onClose = { openBookId = null }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            Icon(
                                painterResource(it.icon),
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                val screenModifier = Modifier.padding(innerPadding)
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(homeViewModel, screenModifier)
                    AppDestinations.PLAYER -> PlayerScreen(
                        viewModel = playerViewModel,
                        onOpenBook = { openBookId = it },
                        modifier = screenModifier
                    )
                    AppDestinations.LIBRARY -> LibraryScreen(
                        viewModel = libraryViewModel,
                        onOpenBook = { openBookId = it },
                        modifier = screenModifier
                    )
                    AppDestinations.REMINDERS -> RemindersScreen(remindersViewModel, screenModifier)
                }
            }
        }
    }

    // The "time's up" pop-up appears on top of any screen, including the readers
    val timer by homeViewModel.timerState.collectAsState()
    if (timer.finished) {
        val activity = if (timer.mode == FocusMode.READING) "reading" else "listening"
        val minutes = timer.totalMs / 60_000
        AlertDialog(
            onDismissRequest = { homeViewModel.dismissTimerPopup() },
            title = { Text("Time's up!") },
            text = { Text("You finished your $minutes-minute $activity session.") },
            confirmButton = {
                TextButton(onClick = { homeViewModel.dismissTimerPopup() }) { Text("OK") }
            }
        )
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    PLAYER("Player", R.drawable.baseline_play_arrow_24),
    LIBRARY("Library", R.drawable.ic_account_box),
    REMINDERS("Reminders", R.drawable.ic_alarm)
}