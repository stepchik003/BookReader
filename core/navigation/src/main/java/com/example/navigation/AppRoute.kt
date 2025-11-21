package com.example.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {

    @Serializable
    data object Loading : AppRoute
    @Serializable
    data object AuthGraph : AppRoute
    @Serializable
    data object HomeGraph : AppRoute

    @Serializable
    data object HomeRoot : AppRoute

}
sealed interface AuthRoute : AppRoute {
    @Serializable
    data object LoginScreen : AuthRoute
    @Serializable
    data object RegisterScreen : AuthRoute
}

sealed interface HomeRoute : AppRoute {
    @Serializable
    data object Books : HomeRoute
    @Serializable
    data object Upload : HomeRoute
    @Serializable
    data object Profile : HomeRoute
}


sealed interface ReaderRoute : AppRoute {
    @Serializable
    data class BookReader(val bookId: String) : ReaderRoute
}

sealed interface ProfileRoute : AppRoute {
    @Serializable
    data object SettingsScreen : ProfileRoute
}
