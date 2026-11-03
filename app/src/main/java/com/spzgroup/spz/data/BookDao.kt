package com.spzgroup.spz.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY title")
    fun getAll(): Flow<List<Book>>

    @Query("SELECT COUNT(*) FROM books")
    fun count(): Flow<Int>

    @Query("SELECT * FROM books WHERE title = :title AND fileSizeBytes = :size LIMIT 1")
    suspend fun findByTitleAndSize(title: String, size: Long): Book?

    @Insert
    suspend fun insert(book: Book): Long

    @Delete
    suspend fun delete(book: Book)

    @Query("UPDATE books SET position = position + 1 WHERE id = :bookId")
    suspend fun increasePosition(bookId: Long)
}