package com.example.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {

    @Serializable
    data object AuthGraph : AppRoute

    @Serializable
    data object HomeGraph : AppRoute

    @Serializable
    data object LoginScreen : AppRoute

    @Serializable
    data object RegisterScreen : AppRoute

}