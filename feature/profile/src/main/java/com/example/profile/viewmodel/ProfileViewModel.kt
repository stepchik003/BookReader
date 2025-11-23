package com.example.profile.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthRepository
import com.example.profile.mvi.ProfileEffect
import com.example.profile.mvi.ProfileIntent
import com.example.profile.mvi.ProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _intentFlow = MutableSharedFlow<ProfileIntent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _effectFlow = MutableSharedFlow<ProfileEffect>(replay = 0)
    val effect = _effectFlow.asSharedFlow()

    init {
        _intentFlow
            .onEach { intent -> handleIntent(intent) }
            .launchIn(viewModelScope)

        loadUser()
        Log.e("loadUser", state.value.user.toString())
    }

    fun processIntent(intent: ProfileIntent) {
        _intentFlow.tryEmit(intent)
    }

    private fun loadUser() {
        _state.update { it.copy(user = authRepository.currentUser, isLoading = false) }
    }

    private fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Logout -> {
                authRepository.logout()
                sendEffect(ProfileEffect.NavigateToAuth)
            }
            is ProfileIntent.SelectPhoto -> updatePhoto(intent.uri)
        }
    }

    private fun updatePhoto(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                authRepository.uploadPhoto(uri.toString())
                loadUser()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
                sendEffect(ProfileEffect.ShowSnackBar(e.message ?: ""))
            }
        }
    }

    private fun sendEffect(effect: ProfileEffect) {
        viewModelScope.launch {
            _effectFlow.emit(effect)
        }
    }
}