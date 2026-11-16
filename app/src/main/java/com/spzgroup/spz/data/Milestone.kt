package com.spzgroup.spz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestones")
data class Milestone(
    @PrimaryKey val type: String, // READING or LISTENING
    val dailyGoalMinutes: Int // 0 means no goal
) {
    companion object {
        const val READING = "READING"
        const val LISTENING = "LISTENING"
    }
}