package com.example.reader.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.readium.r2.shared.ExperimentalReadiumApi
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.reader.R
import com.example.reader.mvi.FileType
import com.example.reader.mvi.ReaderEffect
import com.example.reader.mvi.ReaderIntent
import com.example.reader.mvi.ThemeMode
import com.example.reader.viewmodel.ReaderViewModel

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
    bookId: String,
    localPath: String,
    bookTitle: String,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.processIntent(ReaderIntent.LoadContent(localPath))
        viewModel.processIntent(ReaderIntent.LoadInfo(bookId, bookTitle))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ReaderEffect.ShowSnackBar -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    val colors = when (state.themeMode) {
        ThemeMode.Light -> lightColorScheme()
        ThemeMode.Dark -> darkColorScheme()
        ThemeMode.System -> MaterialTheme.colorScheme
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.bookTitle) },
                    actions = {
                        IconButton(onClick = { viewModel.processIntent(ReaderIntent.ToggleSettings) }) {
                            Text("АА")  // Или Icon
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(progress = { state.progress })
                    Text(stringResource(R.string.read_percent, (state.progress * 100).toInt()))
                }
            }
        ) { padding ->
            Box(Modifier
                .padding(padding)
                .fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (state.error != null) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.error, state.error!!), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.processIntent(ReaderIntent.Retry) }) { Text(
                            stringResource(R.string.retry)
                        ) }
                        Button(onClick = { viewModel.processIntent(ReaderIntent.DeleteBook) }) { Text("Удалить") }
                    }
                } else {
                    when (state.fileType) {
                        FileType.TXT -> {
                            val lazyListState = rememberLazyListState(state.position)
                            LaunchedEffect(lazyListState.firstVisibleItemIndex) {
                                viewModel.processIntent(ReaderIntent.UpdatePosition(lazyListState.firstVisibleItemIndex))
                            }
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                items(state.lines.size) { index ->
                                    Text(
                                        text = state.lines[index],
                                        fontSize = state.fontSize.sp,
                                        lineHeight = (state.fontSize * state.lineSpacing).sp
                                    )
                                }
                            }
                        }
                        FileType.PDF -> {
//                            PdfVue(
//                                file = File(localPath),
//                                initialPage = state.position,
//                                onPageChanged = { page, total ->
//                                    viewModel.processIntent(ReaderIntent.UpdatePosition(page))
//                                    _state.update { it.copy(total = total) }
//                                },
//                                modifier = Modifier.fillMaxSize()
//                            )
                        }
                        FileType.EPUB -> Text("EPUB не поддерживается", Modifier.align(Alignment.Center))
                        FileType.UNKNOWN -> Text("Неизвестный формат", Modifier.align(Alignment.Center))
                    }
                }
            }

        }

        if (state.showSettings) {
            ModalBottomSheet(onDismissRequest = { viewModel.processIntent(ReaderIntent.ToggleSettings) }) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
                    Divider(Modifier.padding(vertical = 8.dp))

                    SettingsRow(title = stringResource(R.string.font_size)) {
                        FilterChip(
                            selected = state.fontSize == 14f,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeFontSize(14f)) },
                            label = { Text(stringResource(R.string.small)) }
                        )
                        FilterChip(
                            selected = state.fontSize == 18f,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeFontSize(18f)) },
                            label = { Text(stringResource(R.string.mid)) }
                        )
                        FilterChip(
                            selected = state.fontSize == 22f,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeFontSize(22f)) },
                            label = { Text(stringResource(R.string.big)) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    SettingsRow(title = stringResource(R.string.interval)) {
                        FilterChip(
                            selected = state.lineSpacing == 1.0f,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeLineSpacing(1.0f)) },
                            label = { Text(stringResource(R.string.dense)) }
                        )
                        FilterChip(
                            selected = state.lineSpacing == 1.5f,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeLineSpacing(1.5f)) },
                            label = { Text(stringResource(R.string.normal)) }
                        )
                        FilterChip(
                            selected = state.lineSpacing == 2.0f,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeLineSpacing(2.0f)) },
                            label = { Text(stringResource(R.string.free)) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    SettingsRow(title = stringResource(R.string.theme)) {
                        FilterChip(
                            selected = state.themeMode == ThemeMode.Light,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeTheme(ThemeMode.Light)) },
                            label = { Text(stringResource(R.string.light)) }
                        )
                        FilterChip(
                            selected = state.themeMode == ThemeMode.Dark,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeTheme(ThemeMode.Dark)) },
                            label = { Text(stringResource(R.string.dark)) }
                        )
                        FilterChip(
                            selected = state.themeMode == ThemeMode.System,
                            onClick = { viewModel.processIntent(ReaderIntent.ChangeTheme(ThemeMode.System)) },
                            label = { Text(stringResource(R.string.system)) }
                        )
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRow(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}