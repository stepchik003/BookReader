package com.example.domain.util

sealed class UploadStatus {
    data class Progress(val percent: Int) : UploadStatus()
    data class Success(val url: String) : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}

sealed interface DownloadStatus {
    data class Progress(val percent: Int) : DownloadStatus
    data class Success(val localPath: String) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}
