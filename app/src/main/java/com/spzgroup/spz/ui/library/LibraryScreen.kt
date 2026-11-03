package com.spzgroup.spz.ui.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spzgroup.spz.data.Book
import com.spzgroup.spz.data.Reflection
import com.spzgroup.spz.ui.progressLabel

// Files the picker offers. Generic data is included because some phones
// report EPUB files that way; unsupported files are rejected after picking.
private val BOOK_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/epub+zip",
    "audio/*",
    "application/octet-stream"
)

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.books.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()
    val reflections by viewModel.reflections.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    // System file picker. No storage permission is needed for this.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importBook(uri)
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your library", style = MaterialTheme.typography.headlineMedium)

        Button(
            onClick = { pickFile.launch(BOOK_MIME_TYPES) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add book")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(books, key = { it.id }) { book ->
                // Tap the card to open the book, tap "Details" for reflections and delete
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBook(book.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, style = MaterialTheme.typography.titleMedium)
                            Text(bookDetails(book))
                        }
                        TextButton(onClick = { viewModel.selectBook(book) }) {
                            Text("Details")
                        }
                    }
                }
            }
        }
    }

    selectedBook?.let { book ->
        ReflectionDialog(
            book = book,
            reflections = reflections,
            onSave = { text, rating -> viewModel.saveReflection(text, rating) },
            onDelete = { viewModel.deleteSelectedBook() },
            onClose = { viewModel.selectBook(null) }
        )
    }
}

// "Author · PDF · Page 3 of 120", skipping the parts that are empty
private fun bookDetails(book: Book): String =
    listOf(
        book.author.ifEmpty { "Unknown author" },
        book.format.ifEmpty { book.mediaType },
        book.progressLabel()
    ).filter { it.isNotEmpty() }.joinToString(" · ")

@Composable
private fun ReflectionDialog(
    book: Book,
    reflections: List<Reflection>,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    // Form state lives only while the dialog is open
    var text by remember(book.id) { mutableStateOf("") }
    var rating by remember(book.id) { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(book.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How did this book stay with you?")

                // Tap a star to set the rating
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (value in 1..5) {
                        Text(
                            text = if (value <= rating) "★" else "☆",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { rating = value }
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Your takeaway") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (reflections.isNotEmpty()) {
                    Text("Saved reflections", style = MaterialTheme.typography.titleSmall)
                    Column(
                        modifier = Modifier
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        reflections.forEach { reflection ->
                            Text("${reflection.rating}/5 · ${reflection.text}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text, rating)
                    text = "" // clear the field for the next reflection
                },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete book")
                }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    )
}