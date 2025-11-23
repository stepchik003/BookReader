package com.example.data.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.suspendCancellableCoroutine
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.io.IOException
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class UploadResult {
    data class Success(val fileUrl: String, val localPath: String) : UploadResult()
    data class Error(val throwable: Throwable) : UploadResult()
}

sealed class DownloadResult {
    data class Success(val localPath: String) : DownloadResult()
    data class Error(val throwable: Throwable) : DownloadResult()
}

@Singleton
class YandexStorageSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val s3AsyncClient: S3AsyncClient,
    private val bucketName: String
) {
    suspend fun uploadFile(uri: Uri, remotePath: String): UploadResult {
        val localFileName = remotePath.substringAfterLast('/')
        val localFile = File(context.filesDir, localFileName)

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                localFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return UploadResult.Error(IOException("Failed to get stream"))

            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(remotePath)
                .contentLength(localFile.length())
                .build()

            uploadInternal(putObjectRequest, localFile)

            val fileUrl = "https://storage.yandexcloud.net/$bucketName/$remotePath"
            return UploadResult.Success(fileUrl, localFile.path)

        } catch (e: Exception) {
            localFile.delete()
            return UploadResult.Error(IOException(e))
        } finally {
        }
    }

    private suspend fun uploadInternal(
        request: PutObjectRequest,
        file: File
    ) = suspendCancellableCoroutine { continuation ->

        val future = s3AsyncClient.putObject(
            request,
            AsyncRequestBody.fromFile(file)
        )

        future.whenComplete { response, exception ->
            if (exception != null) {
                continuation.resumeWithException(exception)
            } else if (response != null) {
                continuation.resume(Unit)
            }
        }

        continuation.invokeOnCancellation {
            future.cancel(true)
        }
    }

    suspend fun downloadFile(remotePath: String, userId: String): DownloadResult {

        val localFileName = remotePath.substringAfterLast('/')
        val localFile = File(context.filesDir, localFileName)
        val localPath: Path = localFile.toPath()

        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key("books/$userId/$remotePath")
            .build()


        try {
            s3AsyncClient.getObject(
                getObjectRequest,
                localPath
            ).await()

            return DownloadResult.Success(localFile.path)

        } catch (e: Exception) {
            localFile.delete()
            return DownloadResult.Error(IOException(e))
        }
    }


}