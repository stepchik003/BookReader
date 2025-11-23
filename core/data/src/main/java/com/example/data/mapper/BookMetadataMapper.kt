package com.example.data.mapper

import com.example.data.database.BookEntity
import com.example.data.firestore.BookMetadata

fun BookMetadata.toEntity(id: String): BookEntity {
    return BookEntity(
        id = id,
        title = this.title,
        author = this.author,
        fileUrl = this.fileUrl,
        userId = this.userId,
        localPath = null
    )
}