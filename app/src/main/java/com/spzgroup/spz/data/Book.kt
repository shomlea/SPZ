package com.spzgroup.spz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "",
    val mediaType: String, // "EBOOK" or "AUDIOBOOK"
    val filePath: String = "", // empty for the sample books
    val fileSizeBytes: Long = 0,
    val position: Int = 0
)