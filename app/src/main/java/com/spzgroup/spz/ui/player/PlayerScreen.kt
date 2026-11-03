package com.spzgroup.spz.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spzgroup.spz.data.Book

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val books by viewModel.books.collectAsState()
    val selectedId by viewModel.selectedBookId.collectAsState()
    val selectedBook = books.find { it.id == selectedId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Player", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedBook == null) {
                    Text("Pick a book from the list below")
                } else {
                    Text(selectedBook.title, style = MaterialTheme.typography.titleLarge)
                    Text("Position: ${selectedBook.position}")
                }
            }
        }

        Button(
            onClick = { viewModel.nextPage() },
            enabled = selectedBook != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next page")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(books, key = { it.id }) { book ->
                BookRow(
                    book = book,
                    isSelected = book.id == selectedId,
                    onClick = { viewModel.selectBook(book.id) }
                )
            }
        }
    }
}

@Composable
private fun BookRow(book: Book, isSelected: Boolean, onClick: () -> Unit) {
    val colors = if (isSelected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    Card(
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium)
            Text("${book.mediaType} · position ${book.position}")
        }
    }
}