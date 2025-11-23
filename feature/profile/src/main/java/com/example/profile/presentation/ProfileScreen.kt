package com.example.profile.presentation

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.profile.R
import com.example.profile.mvi.ProfileEffect
import com.example.profile.mvi.ProfileIntent
import com.example.profile.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) viewModel.processIntent(ProfileIntent.SelectPhoto(uri))
        }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowSnackBar -> {}
                ProfileEffect.NavigateToAuth -> onLogout()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                windowInsets = WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)
            )


        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val user = state.user
            if (user != null) {
                Log.e("user", user.toString())
                if (user.photoUrl == null || user.photoUrl == "null") Image(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(300.dp)
                        .clip(CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") }
                )
                else AsyncImage(
                    model = user.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") }
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    Text(stringResource(R.string.name))
                    Spacer(Modifier.width(8.dp))
                    Text(user.displayName ?: stringResource(R.string.no_info))
                }

                Row {
                    Text(stringResource(R.string.email))
                    Spacer(Modifier.width(8.dp))
                    Text(user.email ?: stringResource(R.string.no_info))
                }
                Spacer(Modifier.height(48.dp))
                Button(onClick = { viewModel.processIntent(ProfileIntent.Logout) }) {
                    Text(stringResource(R.string.logout))
                }
            } else {
                Text(stringResource(R.string.no_user_data))
            }
            if (state.error != null) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            if (state.isLoading) {
                CircularProgressIndicator()
            }
        }
    }
}