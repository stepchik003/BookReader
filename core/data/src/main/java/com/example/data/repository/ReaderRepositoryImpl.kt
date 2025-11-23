package com.example.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.data.database.BookDao
import com.example.domain.repository.ReaderRepository
import com.example.domain.util.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.inject.Inject

class ReaderRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
    private val dao: BookDao
) : ReaderRepository {

    override suspend fun loadBookLines(localPath: String): AppResult<Flow<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (!file.exists()) return@withContext AppResult.Error("File not found")
                val extension = file.extension.lowercase()
                if (extension == "plain") {
                    val linesFlow = flow {
                        BufferedReader(InputStreamReader(FileInputStream(file), "windows-1251")).use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                emit(line!!)
                            }
                        }
                    }
                    AppResult.Success(linesFlow)
                } else {
                    AppResult.Error("Format not supported: $extension")
                }
            } catch (e: Exception) {
                AppResult.Error(e.message)
            }
        }
    }

    override fun getReadingPosition(bookId: String): Int {
        return prefs.getInt("reader_position_$bookId", 0)
    }

    override fun saveReadingPosition(bookId: String, position: Int) {
        prefs.edit {
            putInt("reader_position_$bookId", position)
        }
    }

    override suspend fun deleteLocalBook(localPath: String, bookId: String): AppResult<Boolean> {
        try {
            val file = File(localPath)
            val deleted = file.delete()

            if (deleted) {
                dao.updateLocalPath(bookId, null)
                return AppResult.Success(true)
            } else {
                return AppResult.Success(false)
            }

        } catch (_: Exception) {
            return AppResult.Success(false)
        }
    }
}