package com.spzgroup.spz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// One row per day.
@Entity(tableName = "daily_progress")
data class DailyProgress(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val readingMinutes: Int = 0,
    val listeningMinutes: Int = 0
)