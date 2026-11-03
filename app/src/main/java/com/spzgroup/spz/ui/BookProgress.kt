package com.spzgroup.spz.ui

import com.spzgroup.spz.data.Book
import java.util.Locale

// Short progress text for lists, like "Page 3 of 120" or "12:30 / 45:00"
fun Book.progressLabel(): String {
    if (totalUnits <= 0) return ""
    return when (format) {
        "PDF" -> "Page ${position + 1} of $totalUnits"
        "EPUB" -> "Chapter ${position + 1} of $totalUnits"
        "AUDIO" -> "${formatTime(position)} / ${formatTime(totalUnits)}"
        else -> ""
    }
}

// 75000 ms -> "1:15", 3725000 ms -> "1:02:05"
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}