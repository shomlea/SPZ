package com.spzgroup.spz.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY hour, minute")
    fun getAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllOnce(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getById(reminderId: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)
}