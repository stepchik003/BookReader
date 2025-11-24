package com.example.reader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.ReaderRepository
import com.example.domain.util.AppResult
import com.example.reader.R
import com.example.reader.mvi.FileType
import com.example.reader.mvi.ReaderEffect
import com.example.reader.mvi.ReaderIntent
import com.example.reader.mvi.ReaderState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ReaderRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state = _state.asStateFlow()

    private val _intentFlow = MutableSharedFlow<ReaderIntent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _effectFlow = MutableSharedFlow<ReaderEffect>(replay = 0)
    val effect = _effectFlow.asSharedFlow()

    init {
        _intentFlow
            .onEach { intent -> handleIntent(intent) }
            .launchIn(viewModelScope)
    }

    fun processIntent(intent: ReaderIntent) {
        _intentFlow.tryEmit(intent)
    }

    private suspend fun handleIntent(intent: ReaderIntent) {
        val currentState = state.value
        when (intent) {
            is ReaderIntent.LoadContent -> {
                _state.value = currentState.copy(isLoading = true, error = null, localPath = intent.localPath, lines = emptyList())
                val fileType = determineFileType(intent.localPath)
                if (fileType == FileType.PDF){
                    _state.update { it.copy(isLoading = false, fileType = fileType) }
                    return
                }
                val position = repository.getReadingPosition(currentState.bookId)
                val total = if (fileType == FileType.TXT) estimateTotalLines(intent.localPath) else 1
                val linesResult = if (fileType == FileType.TXT) repository.loadBookLines(intent.localPath) else AppResult.Error(
                    context.getString(
                        R.string.only_txt
                    ))
                when (linesResult) {
                    is AppResult.Success -> {
                        _state.value = currentState.copy(
                            fileType = fileType,
                            position = position,
                            total = total,
                            isLoading = false
                        )
                        viewModelScope.launch {
                            linesResult.data.collect { line ->
                                _state.update { it.copy(lines = it.lines + line) }
                                val progress = state.value.lines.size.toFloat() / total.coerceAtLeast(1)
                                _state.update { it.copy(progress = progress) }
                            }
                        }
                    }
                    is AppResult.Error -> {
                        _state.value = currentState.copy(error = linesResult.message, isLoading = false)
                        sendEffect(ReaderEffect.ShowSnackBar(
                            context.getString(
                                R.string.downloading_error,
                                linesResult.message ?: ""
                            )))
                    }
                }
            }
            is ReaderIntent.LoadInfo -> _state.update { it.copy(bookTitle = intent.bookTitle, bookId = intent.bookId) }
            is ReaderIntent.ChangeFontSize -> _state.update { it.copy(fontSize = intent.size) }
            is ReaderIntent.ChangeLineSpacing -> _state.update { it.copy(lineSpacing = intent.spacing) }
            is ReaderIntent.ChangeTheme -> _state.update { it.copy(themeMode = intent.mode) }
            is ReaderIntent.UpdatePosition -> {
                repository.saveReadingPosition(currentState.bookTitle, intent.position)
                val progress = intent.position.toFloat() / currentState.total.coerceAtLeast(1).toFloat()
                _state.update { it.copy(progress = progress, position = intent.position) }
            }
            ReaderIntent.ToggleSettings -> _state.update { it.copy(showSettings = !it.showSettings) }
            ReaderIntent.Retry -> if (currentState.localPath != null) processIntent(ReaderIntent.LoadContent(currentState.localPath)) else sendEffect(ReaderEffect.ShowSnackBar(
                context.getString(
                    R.string.not_successful
                )))
            ReaderIntent.DeleteBook -> {
                if (currentState.localPath != null) {
                    val result = repository.deleteLocalBook(currentState.localPath, currentState.bookTitle)
                    if (result is AppResult.Success) {
                        sendEffect(ReaderEffect.ShowSnackBar(context.getString(R.string.deleted_book)))
                    } else {
                        sendEffect(ReaderEffect.ShowSnackBar(context.getString(R.string.error_deleting)))
                    }
                } else {
                    sendEffect(ReaderEffect.ShowSnackBar(context.getString(R.string.no_path)))
                }
            }
        }
    }

    private fun sendEffect(effect: ReaderEffect) {
        viewModelScope.launch {
            _effectFlow.emit(effect)
        }
    }

    private fun determineFileType(localPath: String): FileType {
        return when (File(localPath).extension.lowercase()) {
            "plain" -> FileType.TXT
            "pdf" -> FileType.PDF
            "epub+zip" -> FileType.EPUB
            else -> FileType.UNKNOWN
        }
    }

    private fun estimateTotalLines(localPath: String): Int {
        return try {
            val file = File(localPath)
            file.bufferedReader().use { reader ->
                reader.lineSequence().count()
            }
        } catch (_: IOException) {
            1
        }
    }
}