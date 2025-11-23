package com.example.auth.mvi

import com.example.domain.model.User

sealed class AuthState {

    data object Loading : AuthState()

    data class LoggedOut(
        val emailInput: String = "",
        val passwordInput: String = "",
        val isRegisterMode: Boolean = false,
        val isPasswordVisible: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : AuthState()

    data class LoggedIn(
        val user: User
    ) : AuthState()
}

sealed interface AuthIntent {
    data class ChangeEmail(val email: String) : AuthIntent
    data class ChangePassword(val pass: String) : AuthIntent
    object TogglePasswordVisibility : AuthIntent
    object ToggleMode : AuthIntent
    object SubmitAuth : AuthIntent
    object DismissError : AuthIntent
}

sealed interface AuthEffect {
    object NavigateToHome : AuthEffect
    data class ShowSnackBar(val message: String) : AuthEffect
}