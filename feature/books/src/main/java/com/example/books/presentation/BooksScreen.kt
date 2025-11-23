package com.example.books.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.books.R
import com.example.books.mvi.BooksEffect
import com.example.books.mvi.BooksIntent
import com.example.books.viewmodel.BooksViewModel
import com.example.domain.model.Book
import com.example.domain.util.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BooksScreen(
    viewModel: BooksViewModel = hiltViewModel(),
    onBookReadClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(0)
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect {
            when (it) {
                is BooksEffect.ShowToast ->
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { viewModel.processIntent(BooksIntent.PullToRefresh) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_books)) },
                windowInsets = WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .pullRefresh(pullRefreshState)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.processIntent(BooksIntent.SearchBooks(it)) },
                    label = { Text(stringResource(R.string.search_books)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                when {
                    state.isLoading && state.allBooks.isEmpty() -> {
                        LoadingPlaceholder()
                    }
                    state.syncError != null -> {
                        ErrorPlaceholder(state.syncError!!) {
                            viewModel.processIntent(BooksIntent.PullToRefresh)
                        }
                    }
                    state.allBooks.isEmpty() -> {
                        EmptyLibraryPlaceholder()
                    }
                    state.filteredBooks.isEmpty() && state.searchQuery.isNotBlank() -> {
                        NothingFoundPlaceholder()
                    }
                    else -> {
                        LazyColumn(state = listState) {
                            items(state.filteredBooks, key = { it.id }) { book ->
                                val status = state.downloadStatus[book.id] ?: DownloadStatus.Idle

                                BookItem(
                                    book = book,
                                    downloadStatus = status,
                                    onDownloadClick = { viewModel.processIntent(BooksIntent.DownloadBook(book)) },
                                    onDeleteClick = { viewModel.processIntent(BooksIntent.DeleteLocalFile(book.id)) },
                                    onReadClick = { onBookReadClick(book.id) }
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}


@Composable
fun BookItem(
    book: Book,
    downloadStatus: DownloadStatus,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReadClick: () -> Unit
) {
    val actionEnabled = downloadStatus !is DownloadStatus.Progress

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = {
                if (book.localPath != null) onReadClick()
            }),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled. MenuBook,
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                if (downloadStatus is DownloadStatus.Progress) {
                    LinearProgressIndicator(
                        progress = downloadStatus.percent / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    Text("${downloadStatus.percent}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.width(8.dp))

            when {
                downloadStatus is DownloadStatus.Progress -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                book.localPath != null -> {
                    IconButton(onClick = onDeleteClick, enabled = actionEnabled) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
                else -> {
                    IconButton(onClick = onDownloadClick, enabled = actionEnabled) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.download))
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyLibraryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.pull_to_refresh), color = Color.Gray)
    }
}

@Composable
fun NothingFoundPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.not_found), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.try_new_search), color = Color.Gray)
    }
}

@Composable
fun ErrorPlaceholder(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.download_error), style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}