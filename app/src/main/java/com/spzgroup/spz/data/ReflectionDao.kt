package com.spzgroup.spz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {

    @Query("SELECT * FROM reflections WHERE bookId = :bookId ORDER BY id DESC")
    fun getForBook(bookId: Long): Flow<List<Reflection>>

    @Insert
    suspend fun insert(reflection: Reflection)
}