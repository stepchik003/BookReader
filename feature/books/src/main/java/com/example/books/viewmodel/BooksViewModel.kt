package com.example.books.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books.R
import com.example.books.mvi.BooksEffect
import com.example.books.mvi.BooksIntent
import com.example.books.mvi.BooksState
import com.example.domain.model.Book
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BookRepository
import com.example.domain.util.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val repository: BookRepository,
    authRepository: AuthRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {


    private val _state = MutableStateFlow(BooksState(isLoading = true))
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<BooksEffect>()
    val effect = _effect.asSharedFlow()

    private val intentFlow = MutableSharedFlow<BooksIntent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        repository.getBooksStream()
            .onEach { books ->
                _state.update {
                    it.copy(allBooks = books, isLoading = false)
                }
            }
            .catch { error ->
                _state.update {
                    it.copy(syncError = context.getString(
                        R.string.getting_list_error,
                        error.message
                    ), isLoading = false)
                }
            }
            .launchIn(viewModelScope)
        authRepository.currentUser.let { user ->
            if (user != null) {
                _state.update {
                    it.copy(userId = user.id)
                }
            }
        }

        _state
            .map { state ->
                val sourceList = state.allBooks

                if (state.searchQuery.isBlank()) {
                    sourceList
                } else {
                    val query = state.searchQuery.trim().lowercase()
                    sourceList.filter { book ->
                        book.title.lowercase().contains(query) ||
                                book.author.lowercase().contains(query)
                    }
                }
            }
            .distinctUntilChanged()
            .onEach { filteredList ->
                _state.update { it.copy(filteredBooks = filteredList) }
            }
            .launchIn(viewModelScope)
        intentFlow
            .onEach { handleIntent(it) }
            .launchIn(viewModelScope)

        processIntent(BooksIntent.PullToRefresh)
    }

    fun processIntent(intent: BooksIntent) {
        intentFlow.tryEmit(intent)
    }

    private fun handleIntent(intent: BooksIntent) {
        viewModelScope.launch {
            try {
                when (intent) {
                    BooksIntent.PullToRefresh -> syncBooks()
                    is BooksIntent.SearchBooks -> updateSearchQuery(intent.query)
                    is BooksIntent.DownloadBook -> downloadBook(intent.book)
                    is BooksIntent.DeleteLocalFile -> deleteLocalFile(intent.bookId)
                }
            } catch (e: Exception) {
                _effect.emit(BooksEffect.ShowToast(context.getString(R.string.error, e.message)))
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update {
            it.copy(searchQuery = query)
        }
    }

    private fun syncBooks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, syncError = null) }
            try {
                if (state.value.userId != null) {
                    repository.syncRemoteBooks(state.value.userId!!)
                    _effect.emit(BooksEffect.ShowToast(context.getString(R.string.synced)))
                }
                else {
                    _state.update { it.copy(syncError = context.getString(R.string.user_not_found)) }
                    _effect.emit(BooksEffect.ShowToast(context.getString(R.string.user_not_found)))

                }

            } catch (e: Exception) {
                val msg = e.message ?: context.getString(R.string.unknown_sync_error)
                _state.update { it.copy(syncError = msg) }
                _effect.emit(BooksEffect.ShowToast(context.getString(R.string.sync_error, msg)))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun downloadBook(book: Book) {
        if (state.value.downloadStatus[book.id] is DownloadStatus.Progress) return

        viewModelScope.launch {
            repository.downloadBook(book)
                .onStart {
                    _state.update { currentState ->
                        currentState.copy(
                            downloadStatus = currentState.downloadStatus + (book.id to DownloadStatus.Progress(0))
                        )
                    }
                }
                .onEach { status ->
                    _state.update { currentState ->
                        currentState.copy(
                            downloadStatus = currentState.downloadStatus + (book.id to status)
                        )
                    }
                    if (status is DownloadStatus.Success) {
                        _effect.emit(BooksEffect.ShowToast(
                            context.getString(
                                R.string.book_downloaded,
                                book.title
                            )))
                    }
                    if (status is DownloadStatus.Error) {
                        _effect.emit(BooksEffect.ShowToast(
                            context.getString(
                                R.string.downloading_error,
                                status.message
                            )))
                    }
                }
                .catch { error ->
                    _state.update { currentState ->
                        currentState.copy(
                            downloadStatus = currentState.downloadStatus + (book.id to DownloadStatus.Error(
                                context.getString(
                                    R.string.not_downloaded, error.message
                                )))
                        )
                    }
                    launch {
                        kotlinx.coroutines.delay(5000)
                        _state.update { it.copy(downloadStatus = it.downloadStatus - book.id) }
                    }
                }
                .launchIn(this)
        }
    }

    private fun deleteLocalFile(bookId: String) {
        viewModelScope.launch {
            val bookTitle = state.value.allBooks.find { it.id == bookId }?.title ?: context.getString(R.string.of_book)

            _state.update { it.copy(downloadStatus = it.downloadStatus - bookId) }

            val success = repository.deleteLocalBook(bookId)

            if (success) {
                _effect.emit(BooksEffect.ShowToast(
                    context.getString(
                        R.string.file_deleted,
                        bookTitle
                    )))
            } else {
                _effect.emit(BooksEffect.ShowToast(context.getString(R.string.deleting_error)))
            }
        }
    }
}