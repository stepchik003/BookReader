package com.example.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {

    @Serializable
    data object LoadingScreen : AppRoute
    @Serializable
    data object AuthGraph : AppRoute
    @Serializable
    data object MainContentGraph : AppRoute

}
sealed interface AuthRoute : AppRoute {
    @Serializable
    data object LoginScreen : AuthRoute
    @Serializable
    data object RegisterScreen : AuthRoute
}

sealed interface MainContentRoute : AppRoute {
    @Serializable
    data object BooksScreen : MainContentRoute
    @Serializable
    data object UploadScreen : MainContentRoute
    @Serializable
    data object ProfileScreen : MainContentRoute
}


sealed interface ReaderRoute : AppRoute {
    @Serializable
    data class BookReaderScreen(val bookId: String) : ReaderRoute
}

sealed interface ProfileRoute : AppRoute {
    @Serializable
    data object SettingsScreen : ProfileRoute
}
