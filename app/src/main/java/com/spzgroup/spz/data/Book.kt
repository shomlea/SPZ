package com.spzgroup.spz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "",
    val mediaType: String, // "EBOOK" or "AUDIOBOOK"
    val format: String = "", // "PDF", "EPUB" or "AUDIO" (empty for the sample books)
    val filePath: String = "", // empty for the sample books
    val fileSizeBytes: Long = 0,
    val position: Long = 0, // PDF page, EPUB chapter or audio position in ms
    val totalUnits: Long = 0, // number of pages, chapters or audio length in ms
    val lastOpenedAt: Long = 0, // 0 = never opened
    val coverPath: String = "" // not used yet
)