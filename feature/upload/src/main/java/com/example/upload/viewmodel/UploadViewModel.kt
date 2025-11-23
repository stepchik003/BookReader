package com.example.upload.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BookRepository
import com.example.domain.util.UploadStatus
import com.example.upload.R
import com.example.upload.mvi.UploadEffect
import com.example.upload.mvi.UploadIntent
import com.example.upload.mvi.UploadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: BookRepository,
    private val authRepository: AuthRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state = _state.asStateFlow()

    private val _intentFlow = MutableSharedFlow<UploadIntent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _effectFlow = MutableSharedFlow<UploadEffect>(replay = 0)
    val effect = _effectFlow.asSharedFlow()

    init {
        _intentFlow.tryEmit(UploadIntent.SetUser(authRepository.currentUser?.id))
        _intentFlow
            .onEach { intent ->
                handleIntent(intent)
            }
            .launchIn(viewModelScope)
    }

    fun processIntent(intent: UploadIntent) {
        _intentFlow.tryEmit(intent)
    }

    private fun handleIntent(intent: UploadIntent) {
        when (intent) {
            is UploadIntent.SelectFile -> selectFile(intent.uri)
            is UploadIntent.ChangeTitle -> updateReadyState {
                it.copy(
                    titleInput = intent.title,
                    error = null,
                    success = false
                )
            }

            is UploadIntent.ChangeAuthor -> updateReadyState {
                it.copy(
                    authorInput = intent.author,
                    error = null,
                    success = false
                )
            }

            UploadIntent.Upload -> submitUpload()
            UploadIntent.Reset -> _state.value = UploadState.Idle
            is UploadIntent.SetUser -> updateReadyState {
                it.copy(
                    userId = intent.userId
                )
            }
        }
    }

    private fun updateReadyState(reducer: (UploadState.ReadyToUpload) -> UploadState.ReadyToUpload) {
        _state.update { currentState ->
            if (currentState is UploadState.ReadyToUpload) {
                reducer(currentState)
            } else {
                currentState
            }
        }
    }

    private fun selectFile(uri: String) {
        val oldState = _state.value as? UploadState.ReadyToUpload

        _state.value = UploadState.ReadyToUpload(
            fileUri = uri,
            titleInput = oldState?.titleInput ?: "",
            authorInput = oldState?.authorInput ?: ""
        )
    }

    private fun submitUpload() {
        val currentState = state.value as? UploadState.ReadyToUpload ?: return

        if (!validateFields(currentState)) return

        viewModelScope.launch {
            updateReadyState { it.copy(isUploading = true, error = null, success = false) }

            val userId = authRepository.currentUser?.id
            if (userId == null) {
                updateReadyState {
                    it.copy(
                        isUploading = false,
                        error = context.getString(R.string.user_not_found)
                    )
                }
                return@launch
            }

            repository.uploadNewBook(
                fileUri = currentState.fileUri,
                title = currentState.titleInput,
                author = currentState.authorInput,
                userId = userId
            )
                .onEach { status ->
                    when (status) {
                        is UploadStatus.Progress -> {
                            updateReadyState { it.copy(progress = status.percent) }
                        }

                        is UploadStatus.Success -> {
                            updateReadyState {
                                it.copy(
                                    isUploading = false,
                                    success = true,
                                    progress = 100
                                )
                            }
                            sendEffect(
                                UploadEffect.ShowSnackBar(
                                    context.getString(
                                        R.string.uploaded,
                                        status.url
                                    )
                                )
                            )
                        }

                        is UploadStatus.Error -> {
                            updateReadyState {
                                it.copy(
                                    isUploading = false,
                                    error = status.message
                                )
                            }
                            sendEffect(
                                UploadEffect.ShowSnackBar(
                                    context.getString(
                                        R.string.error,
                                        status.message
                                    )
                                )
                            )
                        }
                    }
                }
                .catch { _ ->
                    val message = context.getString(R.string.file_upload_error)
                    updateReadyState { it.copy(isUploading = false, error = message) }
                    sendEffect(UploadEffect.ShowSnackBar(message))
                }
                .launchIn(this)
        }
    }

    private fun validateFields(state: UploadState.ReadyToUpload): Boolean {
        if (state.titleInput.isBlank()) {
            updateReadyState { it.copy(error = context.getString(R.string.title_error)) }
            return false
        }
        if (state.authorInput.isBlank()) {
            updateReadyState { it.copy(error = context.getString(R.string.author_error)) }
            return false
        }
        return true
    }

    private fun sendEffect(effect: UploadEffect) {
        viewModelScope.launch {
            _effectFlow.emit(effect)
        }
    }
}