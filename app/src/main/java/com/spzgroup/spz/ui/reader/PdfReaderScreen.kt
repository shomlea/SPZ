package com.spzgroup.spz.ui.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spzgroup.spz.data.Book
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

// Shows one PDF page at a time, drawn with Android's built-in PdfRenderer.
@Composable
fun PdfReaderScreen(book: Book, viewModel: ReaderViewModel, onClose: () -> Unit) {
    val widthPx = LocalContext.current.resources.displayMetrics.widthPixels

    // PdfRenderer can only draw one page at a time, so we take turns with a lock
    val renderLock = remember { Mutex() }

    // Stays null if the file is broken
    val renderer = remember(book.id) { openRenderer(File(book.filePath)) }

    DisposableEffect(renderer) {
        onDispose {
            if (renderer != null) {
                // Wait for a page that may still be drawing, then close
                CoroutineScope(Dispatchers.IO).launch { renderLock.withLock { renderer.close() } }
            }
        }
    }

    TrackReadingTime(viewModel)

    val pageCount = renderer?.pageCount ?: 0
    var pageIndex by rememberSaveable(book.id) { mutableIntStateOf(book.position.toInt()) }
    val currentPage = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

    var pageImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val scrollState = rememberScrollState()

    // Draw the page again every time the page number changes
    LaunchedEffect(renderer, currentPage) {
        if (renderer == null || pageCount == 0) return@LaunchedEffect
        pageImage = withContext(Dispatchers.IO) {
            renderLock.withLock { renderPage(renderer, currentPage, widthPx) }
        }
        scrollState.scrollTo(0)
        viewModel.saveProgress(book.id, currentPage.toLong(), pageCount.toLong())
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
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val image = pageImage
            when {
                renderer == null || pageCount == 0 ->
                    Text("Could not open this PDF", Modifier.align(Alignment.Center))
                image == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Image(
                    bitmap = image,
                    contentDescription = "Page ${currentPage + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                )
            }
        }

        if (pageCount > 0) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Button(onClick = { pageIndex = currentPage - 1 }, enabled = currentPage > 0) {
                    Text("Previous")
                }
                Text("Page ${currentPage + 1} of $pageCount")
                Button(onClick = { pageIndex = currentPage + 1 }, enabled = currentPage < pageCount - 1) {
                    Text("Next")
                }
            }
        }
    }
}

private fun openRenderer(file: File): PdfRenderer? =
    try {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
    } catch (e: Exception) {
        null
    }

// Draws one page as an image that is as wide as the screen
private fun renderPage(renderer: PdfRenderer, index: Int, targetWidth: Int): ImageBitmap {
    val page = renderer.openPage(index)
    try {
        val scale = targetWidth.toFloat() / page.width
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE) // pages are transparent unless we paint the background
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap.asImageBitmap()
    } finally {
        page.close()
    }
}