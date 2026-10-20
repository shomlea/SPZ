package com.spzgroup.spz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.spzgroup.spz.ui.theme.SPZTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SPZTheme {
                SPZApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun SPZApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

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
            // Switch between screens based on the currentDestination state
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.PLAYER -> PlayerScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.LIBRARY -> LibraryScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
)
{
    HOME("Home", R.drawable.ic_home),
    PLAYER("Player", R.drawable.ic_favorite), // Placeholder for Player
    LIBRARY("Library", R.drawable.ic_account_box) // Placeholder for Library
}

// Component 1: Home / Dashboard Fragment
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Home Screen: Daily Progress Rings and Timers will go here.")
    }
}

// Component 2: Media Consumption Activity (Reader & Player)
@Composable
fun PlayerScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Player Screen: Audio Player and Ebook Reader with Hardware Sensors.")
    }
}

// Component 3: Library & Reflection Fragment
@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Library Screen: Downloaded Books and Learning Mode Reflections.")
    }
}

@Preview(showBackground = true)
@Composable
fun SPZAppPreview() {
    SPZTheme {
        SPZApp()
    }
}