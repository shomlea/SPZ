package com.spzgroup.spz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyProgressDao {

    // Flow version for the UI, so the screen updates by itself
    @Query("SELECT * FROM daily_progress WHERE date = :date")
    fun observe(date: String): Flow<DailyProgress?>

    @Query("SELECT * FROM daily_progress WHERE date = :date")
    suspend fun get(date: String): DailyProgress?

    @Insert
    suspend fun insert(progress: DailyProgress)

    @Update
    suspend fun update(progress: DailyProgress)
}