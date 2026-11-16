package com.spzgroup.spz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Book::class,
        Reflection::class,
        DailyProgress::class,
        Milestone::class,
        Reminder::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun dailyProgressDao(): DailyProgressDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "spz.db")
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(seedCallback)
                .build()

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `milestones` (" +
                            "`type` TEXT NOT NULL, `dailyGoalMinutes` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`type`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`label` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                            "`hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, " +
                            "`days` INTEGER NOT NULL, `enabled` INTEGER NOT NULL)"
                )
                insertDefaultGoals(db)
            }
        }

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
                insertDefaultGoals(db)
            }
        }

        private fun insertDefaultGoals(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT OR IGNORE INTO milestones (type, dailyGoalMinutes) VALUES ('READING', 20)")
            db.execSQL("INSERT OR IGNORE INTO milestones (type, dailyGoalMinutes) VALUES ('LISTENING', 20)")
        }
    }
}