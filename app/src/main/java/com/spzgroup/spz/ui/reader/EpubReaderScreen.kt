package com.spzgroup.spz.ui.reader

import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.spzgroup.spz.data.Book
import java.io.File

// Shows one EPUB chapter at a time in a WebView, with Previous / Next buttons.
@Composable
fun EpubReaderScreen(book: Book, viewModel: ReaderViewModel, onClose: () -> Unit) {
    var chapters by remember(book.id) { mutableStateOf<List<File>?>(null) }
    var chapterIndex by rememberSaveable(book.id) { mutableIntStateOf(book.position.toInt()) }
    var textZoom by rememberSaveable { mutableIntStateOf(100) } // percent

    TrackReadingTime(viewModel)

    // Unzipping and parsing can take a moment, so it runs off the main thread
    LaunchedEffect(book.id) { chapters = viewModel.loadEpubChapters(book) }

    val chapterFiles = chapters
    val chapterCount = chapterFiles?.size ?: 0
    val currentChapter = chapterIndex.coerceIn(0, (chapterCount - 1).coerceAtLeast(0))

    // Remember where the reader stopped
    LaunchedEffect(chapterFiles, currentChapter) {
        if (!chapterFiles.isNullOrEmpty()) {
            viewModel.saveProgress(book.id, currentChapter.toLong(), chapterFiles.size.toLong())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            TextButton(onClick = onClose) { Text("Back") }
            Text(
                text = book.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { textZoom = (textZoom - 10).coerceAtLeast(70) }) { Text("A-") }
            TextButton(onClick = { textZoom = (textZoom + 10).coerceAtMost(200) }) { Text("A+") }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                chapterFiles == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                chapterFiles.isEmpty() ->
                    Text("Could not read this EPUB", Modifier.align(Alignment.Center))
                else -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = false // book content is not trusted
                            settings.allowFileAccess = true // needed to open the unzipped chapters
                            settings.allowContentAccess = false
                        }
                    },
                    update = { webView ->
                        webView.settings.textZoom = textZoom
                        val url = Uri.fromFile(chapterFiles[currentChapter]).toString()
                        // Only load when the chapter changed, not when just the font size did
                        if (webView.tag != url) {
                            webView.tag = url
                            webView.loadUrl(url)
                        }
                    }
                )
            }
        }

        if (chapterCount > 0) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Button(onClick = { chapterIndex = currentChapter - 1 }, enabled = currentChapter > 0) {
                    Text("Previous")
                }
                Text("Chapter ${currentChapter + 1} of $chapterCount")
                Button(
                    onClick = { chapterIndex = currentChapter + 1 },
                    enabled = currentChapter < chapterCount - 1
                ) {
                    Text("Next")
                }
            }
        }
    }
}