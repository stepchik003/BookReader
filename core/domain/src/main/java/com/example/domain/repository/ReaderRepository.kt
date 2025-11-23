package com.example.domain.repository

import com.example.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

interface ReaderRepository {
    suspend fun loadBookLines(localPath: String): AppResult<Flow<String>>

    fun getReadingPosition(bookId: String): Int

    fun saveReadingPosition(bookId: String, position: Int)

    suspend fun deleteLocalBook(localPath: String, bookId: String): AppResult<Boolean>
}