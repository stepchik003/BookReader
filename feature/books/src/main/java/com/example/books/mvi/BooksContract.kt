package com.example.books.mvi

import com.example.domain.model.Book
import com.example.domain.util.DownloadStatus

sealed interface BooksIntent {
    object PullToRefresh : BooksIntent

    data class SearchBooks(val query: String) : BooksIntent

    data class DownloadBook(val book: Book) : BooksIntent
    data class DeleteLocalFile(val bookId: String) : BooksIntent
}

data class BooksState(
    val userId: String? = null,
    val allBooks: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val syncError: String? = null,
    val downloadStatus: Map<String, DownloadStatus> = emptyMap()
)


sealed interface BooksEffect {
    data class ShowToast(val message: String) : BooksEffect
}