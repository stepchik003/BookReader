package com.example.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.mvi.AuthEffect
import com.example.auth.mvi.AuthIntent
import com.example.auth.mvi.AuthState
import com.example.domain.repository.AuthRepository
import com.example.domain.util.AppResult
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
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state = _state.asStateFlow()

    private val _intentFlow = MutableSharedFlow<AuthIntent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _effectFlow = MutableSharedFlow<AuthEffect>(replay = 0)
    val effect = _effectFlow.asSharedFlow()

    init {
        initAuthStatusCheck()

        _intentFlow
            .onEach { intent -> handleIntent(intent) }
            .launchIn(viewModelScope)
    }

    private fun initAuthStatusCheck() {
        viewModelScope.launch {
            repository.isAuthenticated.value.let { isLoggedIn ->
                val user = if (isLoggedIn) repository.currentUser else null
                if (user != null) {
                    _state.value = AuthState.LoggedIn(user)
                } else {
                    _state.value = AuthState.LoggedOut()
                }
            }
        }
    }

    fun processIntent(intent: AuthIntent) {
        _intentFlow.tryEmit(intent)
    }

    private fun handleIntent(intent: AuthIntent) {
        if (state.value is AuthState.LoggedOut) {
            when (intent) {
                is AuthIntent.ChangeEmail -> updateLoggedOutState { it.copy(emailInput = intent.email, error = null) }
                is AuthIntent.ChangePassword -> updateLoggedOutState { it.copy(passwordInput = intent.pass, error = null) }
                AuthIntent.TogglePasswordVisibility -> updateLoggedOutState { it.copy(isPasswordVisible = !it.isPasswordVisible) }
                AuthIntent.ToggleMode -> updateLoggedOutState { it.copy(isRegisterMode = !it.isRegisterMode, error = null, passwordInput = "") }
                AuthIntent.SubmitAuth -> submitAuthentication()
                AuthIntent.DismissError -> updateLoggedOutState { it.copy(error = null) }
            }
        }
    }

    private fun updateLoggedOutState(reducer: (AuthState.LoggedOut) -> AuthState.LoggedOut) {
        _state.update { currentState ->
            if (currentState is AuthState.LoggedOut) {
                reducer(currentState)
            } else {
                currentState
            }
        }
    }

    private fun sendEffect(effect: AuthEffect) {
        viewModelScope.launch {
            _effectFlow.emit(effect)
        }
    }

    private fun submitAuthentication() {
        val currentState = state.value as? AuthState.LoggedOut ?: return

        if (!validateFields(currentState)) return

        viewModelScope.launch {
            updateLoggedOutState { it.copy(isLoading = true) }

            val result = if (currentState.isRegisterMode) {
                repository.register(currentState.emailInput, currentState.passwordInput)
            } else {
                repository.login(currentState.emailInput, currentState.passwordInput)
            }

            when (result) {
                is AppResult.Success -> {
                    repository.currentUser?.let { user ->
                        _state.value = AuthState.LoggedIn(user)
                    }
                    sendEffect(AuthEffect.NavigateToHome)
                }
                is AppResult.Error -> {
                    val msg = result.message
                    updateLoggedOutState { it.copy(isLoading = false, error = msg) }
                    sendEffect(AuthEffect.ShowSnackBar(msg ?: ""))
                }
            }
        }
    }

    private fun validateFields(state: AuthState.LoggedOut): Boolean {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.emailInput).matches()) {
            updateLoggedOutState { it.copy(error = "Некорректный формат Email") }
            return false
        }
        if (state.passwordInput.length < 6) {
            updateLoggedOutState { it.copy(error = "Пароль должен быть не менее 6 символов") }
            return false
        }
        return true
    }
}