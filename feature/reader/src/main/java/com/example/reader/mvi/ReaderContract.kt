package com.example.reader.mvi


sealed interface ReaderIntent {
    data class LoadContent(val localPath: String) : ReaderIntent
    data class LoadInfo(val bookId: String, val bookTitle: String): ReaderIntent
    data class ChangeFontSize(val size: Float) : ReaderIntent
    data class ChangeLineSpacing(val spacing: Float) : ReaderIntent
    data class ChangeTheme(val mode: ThemeMode) : ReaderIntent
    data class UpdatePosition(val position: Int) : ReaderIntent
    data object ToggleSettings : ReaderIntent
    data object Retry : ReaderIntent
    data object DeleteBook : ReaderIntent
}

data class ReaderState(
    val bookTitle: String = "",
    val bookId: String = "",
    val lines: List<String> = emptyList(),  // Incremental lines for TXT
    val fileType: FileType = FileType.UNKNOWN,
    val fontSize: Float = 18f,
    val lineSpacing: Float = 1.5f,
    val themeMode: ThemeMode = ThemeMode.System,
    val progress: Float = 0f,
    val position: Int = 0,
    val total: Int = 1,  // Estimate for TXT, total for PDF
    val isLoading: Boolean = true,
    val error: String? = null,
    val showSettings: Boolean = false,
    val localPath: String? = null
)
sealed interface ReaderEffect {
    data class ShowSnackBar(val message: String) : ReaderEffect
}

enum class FileType { TXT, PDF, EPUB, UNKNOWN }
enum class ThemeMode { Light, Dark, System }