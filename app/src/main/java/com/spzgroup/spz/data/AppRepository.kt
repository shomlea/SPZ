package com.spzgroup.spz.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImportResult { SUCCESS, DUPLICATE, FAILED }

// The only place that talks to the DAOs and to the file system.
// ViewModels go through this class.
class AppRepository(
    database: AppDatabase,
    private val context: Context
) {
    private val bookDao = database.bookDao()
    private val reflectionDao = database.reflectionDao()
    private val progressDao = database.dailyProgressDao()

    // --- Books ---

    val books: Flow<List<Book>> = bookDao.getAll()
    val bookCount: Flow<Int> = bookDao.count()

    suspend fun increasePosition(bookId: Long) = bookDao.increasePosition(bookId)

    suspend fun deleteBook(book: Book) {
        bookDao.delete(book)
        if (book.filePath.isNotEmpty()) {
            withContext(Dispatchers.IO) { File(book.filePath).delete() }
        }
    }

    // --- Reflections ---

    fun reflectionsFor(bookId: Long): Flow<List<Reflection>> = reflectionDao.getForBook(bookId)

    suspend fun addReflection(bookId: Long, text: String, rating: Int) {
        reflectionDao.insert(Reflection(bookId = bookId, text = text, rating = rating))
    }

    // --- Daily progress ---

    fun todayProgress(): Flow<DailyProgress?> = progressDao.observe(today())

    suspend fun addReadingMinutes(minutes: Int) {
        val date = today()
        val existing = progressDao.get(date)
        if (existing == null) {
            progressDao.insert(DailyProgress(date = date, readingMinutes = minutes))
        } else {
            progressDao.update(existing.copy(readingMinutes = existing.readingMinutes + minutes))
        }
    }

    // java.time needs API 26 and our minSdk is 24, so we use SimpleDateFormat
    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // --- Import from the phone ---

    // Copies the picked file into filesDir/books and saves it as a new book.
    suspend fun importBook(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val fileName = (getFileName(uri) ?: "Unknown book").replace("/", "_")
        val title = fileName.substringBeforeLast(".")
        val mimeType = resolver.getType(uri) ?: ""
        val mediaType = if (mimeType.startsWith("audio/")) "AUDIOBOOK" else "EBOOK"

        val booksDir = File(context.filesDir, "books")
        booksDir.mkdirs()
        // The timestamp keeps two files with the same name from overwriting each other
        val target = File(booksDir, "${System.currentTimeMillis()}_$fileName")

        try {
            val input = resolver.openInputStream(uri) ?: return@withContext ImportResult.FAILED
            input.use { source ->
                FileOutputStream(target).use { output -> source.copyTo(output) }
            }
        } catch (e: Exception) {
            target.delete()
            return@withContext ImportResult.FAILED
        }

        // We check for duplicates after copying, because only now we know the real size
        val size = target.length()
        if (bookDao.findByTitleAndSize(title, size) != null) {
            target.delete()
            return@withContext ImportResult.DUPLICATE
        }

        try {
            bookDao.insert(
                Book(
                    title = title,
                    mediaType = mediaType,
                    filePath = target.absolutePath,
                    fileSizeBytes = size
                )
            )
            ImportResult.SUCCESS
        } catch (e: Exception) {
            target.delete()
            ImportResult.FAILED
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        ) ?: return null
        cursor.use {
            return if (it.moveToFirst()) it.getString(0) else null
        }
    }
}

