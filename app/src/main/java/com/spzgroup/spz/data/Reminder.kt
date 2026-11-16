package com.spzgroup.spz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val type: String,
    val hour: Int,
    val minute: Int,
    val days: Int,
    val enabled: Boolean = true
) {
    companion object {
        const val EVERY_DAY = 0b1111111

        fun isDaySelected(days: Int, dayIndex: Int): Boolean = (days and (1 shl dayIndex)) != 0
    }
}