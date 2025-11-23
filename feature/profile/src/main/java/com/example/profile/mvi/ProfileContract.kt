package com.example.profile.mvi

import android.net.Uri
import com.example.domain.model.User

sealed interface ProfileIntent {
    object Logout : ProfileIntent
    data class SelectPhoto(val uri: Uri) : ProfileIntent
}

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ProfileEffect {
    data class ShowSnackBar(val message: String) : ProfileEffect
    object NavigateToAuth : ProfileEffect
}