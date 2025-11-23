package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.BookDao
import com.example.data.database.BookEntity
import com.example.data.firestore.BookMetadata
import com.example.data.firestore.FirestoreDataSource
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.storage.DownloadResult
import com.example.data.storage.UploadResult
import com.example.data.storage.YandexStorageSource
import com.example.domain.model.Book
import com.example.domain.repository.BookRepository
import com.example.domain.util.DownloadStatus
import com.example.domain.util.UploadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val storage: YandexStorageSource,
    private val firestore: FirestoreDataSource,
    private val dao: BookDao,
    @param:ApplicationContext private val context: Context
) : BookRepository {
    override fun getBooksStream(): Flow<List<Book>> {
        return dao.getAllBooks()
            .map { entityList ->
                entityList.map { it.toDomain() }
            }
    }

    override suspend fun syncRemoteBooks(userId: String) {
        try {
            val remoteDataMap = firestore.getAllUserBooks(userId)

            val localBooks = dao.getAllBooks().first()

            remoteDataMap.forEach { (id, metadata) ->
                val existingLocalBook = localBooks.find { it.id == id }

                val entity = metadata.toEntity(id).copy(
                    localPath = existingLocalBook?.localPath
                )

                dao.insertBooks(entity)
            }

            val remoteIds = remoteDataMap.keys

            localBooks.forEach { localBook ->
                if (localBook.id !in remoteIds) {
                    deleteLocalBook(localBook.id)
                    dao.deleteBook(localBook.id)
                }
            }

        } catch (e: Exception) {
            throw e
        }
    }

    override fun downloadBook(book: Book): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Progress(0))

        val remotePath = book.fileUrl.substringAfterLast("/")
        val userId = book.userId

        try {
            when (val result = storage.downloadFile(remotePath, userId)) {
                is DownloadResult.Success -> {
                    val localPath = result.localPath

                    dao.updateLocalPath(book.id, localPath)

                    emit(DownloadStatus.Success(localPath))
                }
                is DownloadResult.Error -> {
                    emit(DownloadStatus.Error(result.throwable.message ?: "Downloading error"))
                }
            }
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.message ?: "Downloading error"))
        }
    }

    override suspend fun deleteLocalBook(bookId: String): Boolean {
        val book = dao.getBookById(bookId)
        val localPath = book?.localPath

        if (localPath != null) {
            try {
                val file = File(localPath)
                val deleted = file.delete()

                if (deleted) {
                    dao.updateLocalPath(bookId, null)
                    return true
                } else {
                    return false
                }

            } catch (_: Exception) {
                return false
            }
        }
        return false
    }

    override fun uploadNewBook(
        fileUri: String,
        title: String,
        author: String,
        userId: String
    ): Flow<UploadStatus> = flow {
        emit(UploadStatus.Progress(0))

        try {
            val uri = Uri.parse(fileUri)

            if (!isValidFileType(uri)) {
                emit(UploadStatus.Error("Bad extension"))
                return@flow
            }

            val fileExtension = getFileExtension(context, uri)
            val remoteFileName = "books/${userId}/${title.replace(" ", "_")}.$fileExtension"

            emit(UploadStatus.Progress(20))

            when (val uploadResult = storage.uploadFile(uri, remoteFileName)) {
                is UploadResult.Success -> {
                    emit(UploadStatus.Progress(80))

                    val metadata = createBookMetadata(
                        title = title,
                        author = author,
                        fileUrl = uploadResult.fileUrl,
                        userId = userId
                    )
                    val bookId = firestore.saveBookMetadata((metadata))

                    emit(UploadStatus.Progress(90))
                    val entity = BookEntity(
                        id = bookId,
                        title = metadata.title,
                        author = metadata.author,
                        fileUrl = metadata.fileUrl,
                        userId = metadata.userId,
                        localPath = uploadResult.localPath
                    )
                    dao.insertBooks(entity)
                    emit(UploadStatus.Progress(100))

                    emit(UploadStatus.Success(uploadResult.fileUrl))

                }
                is UploadResult.Error -> {
                    emit(UploadStatus.Error(uploadResult.throwable.message ?: "Uploading error"))
                }
            }

        } catch (e: Exception) {
            emit(UploadStatus.Error(e.message ?: "Error"))
        }
    }


    private fun getFileExtension(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)?.substringAfter("/")
            ?: uri.lastPathSegment?.substringAfterLast(".", "")
            ?: "bin"
    }

    private fun isValidFileType(uri: Uri): Boolean {
        val fileExtension = getFileExtension(context = context, uri = uri).lowercase()
        return fileExtension in listOf("plain", "epub+zip", "pdf")
    }

    private fun createBookMetadata(
        title: String,
        author: String,
        fileUrl: String,
        userId: String,
    ): BookMetadata {
        return BookMetadata(
            title = title,
            author = author,
            fileUrl = fileUrl,
            userId = userId
        )
    }
}