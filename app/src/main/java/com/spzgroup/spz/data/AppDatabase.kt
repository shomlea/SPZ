package com.spzgroup.spz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Book::class, Reflection::class, DailyProgress::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun dailyProgressDao(): DailyProgressDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "spz.db")
                // We only have test data, so a schema change just starts a new database
                .fallbackToDestructiveMigration()
                .addCallback(seedCallback)
                .build()

        // Runs only once, right after the database file is created
        private val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO books (title, author, mediaType, format, filePath, fileSizeBytes, " +
                            "position, totalUnits, lastOpenedAt, coverPath) " +
                            "VALUES ('The Art of Noticing', 'Rob Walker', 'EBOOK', '', '', 0, 0, 0, 0, '')"
                )
                db.execSQL(
                    "INSERT INTO books (title, author, mediaType, format, filePath, fileSizeBytes, " +
                            "position, totalUnits, lastOpenedAt, coverPath) " +
                            "VALUES ('Stillness Is the Key', 'Ryan Holiday', 'AUDIOBOOK', '', '', 0, 0, 0, 0, '')"
                )
            }
        }
    }
}