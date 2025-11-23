package com.example.data.mapper

import com.example.data.database.BookEntity
import com.example.domain.model.Book

fun BookEntity.toDomain(): Book {
    return Book(
        id = this.id,
        title = this.title,
        author = this.author,
        fileUrl = this.fileUrl,
        localPath = this.localPath,
        userId = this.userId
    )
}
