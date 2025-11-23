package com.example.domain.repository

import com.example.domain.model.Book
import com.example.domain.util.DownloadStatus
import com.example.domain.util.UploadStatus
import kotlinx.coroutines.flow.Flow

interface BookRepository {


    fun getBooksStream(): Flow<List<Book>>

    suspend fun syncRemoteBooks(userId: String)

    fun downloadBook(book: Book): Flow<DownloadStatus>

    suspend fun deleteLocalBook(bookId: String): Boolean

    fun uploadNewBook(
        fileUri: String,
        title: String,
        author: String,
        userId: String
    ): Flow<UploadStatus>

}