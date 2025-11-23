package com.example.upload.mvi

sealed interface UploadState {

    object Idle : UploadState

    data class ReadyToUpload(
        val userId: String? = null,
        val fileUri: String,
        val titleInput: String = "",
        val authorInput: String = "",

        val isUploading: Boolean = false,
        val progress: Int = 0,
        val error: String? = null,
        val success: Boolean = false
    ) : UploadState
}

sealed interface UploadIntent {

    data class SelectFile(val uri: String) : UploadIntent

    data class SetUser(val userId: String?) : UploadIntent

    object Upload : UploadIntent

    data class ChangeTitle(val title: String) : UploadIntent

    data class ChangeAuthor(val author: String) : UploadIntent

    object Reset : UploadIntent
}

sealed interface UploadEffect {
    data class ShowSnackBar(val message: String) : UploadEffect
}