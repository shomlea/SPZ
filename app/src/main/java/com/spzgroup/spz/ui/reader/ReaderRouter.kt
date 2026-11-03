package com.spzgroup.spz.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spzgroup.spz.data.Book
import com.spzgroup.spz.ui.AppViewModelFactory
import java.io.File

// Loads the book and shows the right screen for its format.
@Composable
fun ReaderRouter(
    bookId: Long,
    factory: AppViewModelFactory,
    onClose: () -> Unit
) {
    val viewModel: ReaderViewModel = viewModel(factory = factory)
    var book by remember(bookId) { mutableStateOf<Book?>(null) }
    var loaded by remember(bookId) { mutableStateOf(false) }

    // The phone's back button closes the reader instead of the app
    BackHandler(onBack = onClose)

    LaunchedEffect(bookId) {
        book = viewModel.openBook(bookId)
        loaded = true
    }

    // The Scaffold keeps the content away from the status and navigation bars
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val currentBook = book
            when {
                !loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                currentBook == null -> Message("This book could not be found", onClose)
                currentBook.filePath.isEmpty() || !File(currentBook.filePath).exists() ->
                    Message("This book has no file. Add one with \"Add book\" in the Library.", onClose)
                currentBook.format == "PDF" -> PdfReaderScreen(currentBook, viewModel, onClose)
                currentBook.format == "EPUB" -> EpubReaderScreen(currentBook, viewModel, onClose)
                currentBook.format == "AUDIO" -> AudioPlayerScreen(currentBook, factory, onClose)
                else -> Message("This file type is not supported", onClose)
            }
        }
    }
}

@Composable
private fun Message(text: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}