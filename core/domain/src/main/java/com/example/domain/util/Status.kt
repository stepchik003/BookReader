package com.example.domain.util

sealed class UploadStatus {
    data object Idle : UploadStatus()
    data class Progress(val value: Float) : UploadStatus()
    data class Success(val remoteId: String) : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}

sealed class DownloadStatus {
    data class Progress(val value: Float) : DownloadStatus()
    data object Success : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}