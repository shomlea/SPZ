package com.spzgroup.spz.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spzgroup.spz.ui.progressLabel

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onOpenBook: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastBook by viewModel.lastBook.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Player", style = MaterialTheme.typography.headlineMedium)

        val book = lastBook
        if (book == null) {
            Text("Nothing opened yet. Pick a book from your library.")
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Continue where you left off", style = MaterialTheme.typography.labelLarge)
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    val progress = book.progressLabel()
                    if (progress.isNotEmpty()) Text(progress)
                }
            }

            Button(
                onClick = { onOpenBook(book.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (book.format == "AUDIO") "Play" else "Read")
            }
        }
    }
}