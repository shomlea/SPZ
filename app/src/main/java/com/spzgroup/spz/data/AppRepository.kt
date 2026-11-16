package com.spzgroup.spz.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.spzgroup.spz.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImportResult { SUCCESS, DUPLICATE, UNSUPPORTED, FAILED }

class AppRepository(
    database: AppDatabase,
    private val context: Context
) {
    private val bookDao = database.bookDao()
    private val reflectionDao = database.reflectionDao()
    private val progressDao = database.dailyProgressDao()
    private val milestoneDao = database.milestoneDao()
    private val reminderDao = database.reminderDao()


    val books: Flow<List<Book>> = bookDao.getAll()
    val bookCount: Flow<Int> = bookDao.count()
    val lastOpenedBook: Flow<Book?> = bookDao.getLastOpened()

    suspend fun getBook(bookId: Long): Book? = bookDao.getById(bookId)

    suspend fun markOpened(bookId: Long) = bookDao.markOpened(bookId, System.currentTimeMillis())

    suspend fun saveProgress(bookId: Long, position: Long, totalUnits: Long) =
        bookDao.updateProgress(bookId, position, totalUnits)

    suspend fun increasePosition(bookId: Long) = bookDao.increasePosition(bookId)

    suspend fun deleteBook(book: Book) {
        bookDao.delete(book)
        if (book.filePath.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                File(book.filePath).delete()
                epubFolder(book).deleteRecursively()
            }
        }
    }


    fun reflectionsFor(bookId: Long): Flow<List<Reflection>> = reflectionDao.getForBook(bookId)

    suspend fun addReflection(bookId: Long, text: String, rating: Int) {
        reflectionDao.insert(Reflection(bookId = bookId, text = text, rating = rating))
    }


    fun todayProgress(): Flow<DailyProgress?> = progressDao.observe(today())

    suspend fun addReadingMinutes(minutes: Int) = addMinutes(reading = minutes, listening = 0)

    suspend fun addListeningMinutes(minutes: Int) = addMinutes(reading = 0, listening = minutes)

    private suspend fun addMinutes(reading: Int, listening: Int) {
        val date = today()
        val existing = progressDao.get(date)
        if (existing == null) {
            progressDao.insert(
                DailyProgress(date = date, readingMinutes = reading, listeningMinutes = listening)
            )
        } else {
            progressDao.update(
                existing.copy(
                    readingMinutes = existing.readingMinutes + reading,
                    listeningMinutes = existing.listeningMinutes + listening
                )
            )
        }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())


    val milestones: Flow<List<Milestone>> = milestoneDao.getAll()

    suspend fun setDailyGoal(type: String, minutes: Int) =
        milestoneDao.save(Milestone(type = type, dailyGoalMinutes = minutes))


    val reminders: Flow<List<Reminder>> = reminderDao.getAll()

    suspend fun getReminder(reminderId: Long): Reminder? = reminderDao.getById(reminderId)

    suspend fun saveReminder(reminder: Reminder) {
        val saved = if (reminder.id == 0L) {
            reminder.copy(id = reminderDao.insert(reminder))
        } else {
            reminderDao.update(reminder)
            reminder
        }
        ReminderScheduler.schedule(context, saved)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        ReminderScheduler.cancel(context, reminder.id)
        reminderDao.delete(reminder)
    }

    suspend fun rescheduleReminders() {
        reminderDao.getAllOnce().forEach { ReminderScheduler.schedule(context, it) }
    }

    suspend fun loadEpubChapters(book: Book): List<File> = withContext(Dispatchers.IO) {
        try {
            EpubReader.open(File(book.filePath), epubFolder(book))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun epubFolder(book: Book) = File(context.filesDir, "epub/${File(book.filePath).name}")


    suspend fun importBook(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val fileName = (getFileName(uri) ?: "Unknown book").replace("/", "_")
        val title = fileName.substringBeforeLast(".")
        val format = detectFormat(fileName) ?: return@withContext ImportResult.UNSUPPORTED
        val mediaType = if (format == "AUDIO") "AUDIOBOOK" else "EBOOK"

        val booksDir = File(context.filesDir, "books")
        booksDir.mkdirs()
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
                    format = format,
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

    private fun detectFormat(fileName: String): String? =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "PDF"
            "epub" -> "EPUB"
            "mp3", "m4a", "m4b", "aac", "ogg", "wav", "flac" -> "AUDIO"
            else -> null
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