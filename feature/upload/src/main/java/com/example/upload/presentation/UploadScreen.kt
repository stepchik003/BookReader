package com.example.upload.presentation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upload.R
import com.example.upload.mvi.UploadEffect
import com.example.upload.mvi.UploadIntent
import com.example.upload.mvi.UploadState
import com.example.upload.viewmodel.UploadViewModel
import androidx.hilt.navigation.compose.hiltViewModel

private val SUPPORTED_MIME_TYPES = arrayOf("application/pdf", "application/epub+zip", "text/plain")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UploadEffect.ShowSnackBar -> {
                    snackBarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            if (mimeType in SUPPORTED_MIME_TYPES) {
                viewModel.processIntent(UploadIntent.SelectFile(it.toString()))
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.bad_extension),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upload_screen_text)) },
                windowInsets = WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)

            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            FileSelectionSection(
                state = state,
                onSelectFile = { filePickerLauncher.launch(SUPPORTED_MIME_TYPES) }
            )

            Spacer(Modifier.height(32.dp))

            when (state) {
                is UploadState.ReadyToUpload -> MetadataAndUploadSection(
                    state as UploadState.ReadyToUpload,
                    viewModel
                )

                is UploadState.Idle -> Text(
                    stringResource(R.string.pick_file_to_upload),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

        }
    }
}

@Composable
fun FileSelectionSection(state: UploadState, onSelectFile: () -> Unit) {
    val isUploading = (state as? UploadState.ReadyToUpload)?.isUploading ?: false

    val fileName = when (state) {
        is UploadState.ReadyToUpload -> {
            state.fileUri
        }

        UploadState.Idle -> ""
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelectFile,
        enabled = !isUploading
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pick_file),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    fileName.substringAfterLast(":").trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetadataAndUploadSection(
    state: UploadState.ReadyToUpload,
    viewModel: UploadViewModel
) {
    val isLoading = state.isUploading

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = state.titleInput,
            onValueChange = {
                viewModel.processIntent(UploadIntent.ChangeTitle(it))
            },
            label = { Text(stringResource(R.string.book_title)) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.authorInput,
            onValueChange = { viewModel.processIntent(UploadIntent.ChangeAuthor(it)) },
            label = { Text(stringResource(R.string.author)) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(visible = isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = state.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.uploading, state.progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
            }
        }


        if (state.error != null) {
            Text(
                state.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.processIntent(UploadIntent.Upload) },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.repeat_uploading))
            }
        } else if (state.success) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.success),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.book_successfully_uploaded),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.processIntent(UploadIntent.Reset) }) {
                Text(stringResource(R.string.upload_another_one))
            }
        }

        if (!isLoading && !state.success && state.error == null) {
            Button(
                onClick = { viewModel.processIntent(UploadIntent.Upload) },
                enabled = state.titleInput.isNotBlank() && state.authorInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.upload))
            }
        }
    }
}