package com.example.bookreader.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.auth.mvi.AuthState
import com.example.auth.presentation.LoginScreen
import com.example.auth.viewmodel.AuthViewModel
import com.example.navigation.AppRoute

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val authState by authViewModel.state.collectAsState()

    val currentGraph = when (authState) {
        is AuthState.Loading -> AppRoute.LoadingScreen
        is AuthState.LoggedIn -> AppRoute.HomeGraph
        is AuthState.LoggedOut -> AppRoute.AuthGraph
    }

    if (authState is AuthState.Loading) {
        LoadingScreen()
        return
    }

    LaunchedEffect(currentGraph) {
        if (currentGraph != AppRoute.LoadingScreen) {
            navController.navigate(currentGraph) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.LoadingScreen
    ) {

        composable<AppRoute.LoadingScreen> {
            LoadingScreen()
        }

        composable<AppRoute.HomeGraph> {
            HomeScreen(authState as AuthState.LoggedIn)
        }

        navigation<AppRoute.AuthGraph>(
            startDestination = AppRoute.LoginScreen
        ) {
            composable<AppRoute.LoginScreen> {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToHome = {
                        navController.navigate(AppRoute.HomeGraph) {
                            popUpTo(AppRoute.AuthGraph) { inclusive = true }
                        }
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            composable<AppRoute.RegisterScreen> {
                Text("Экран регистрации", modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center))
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
    }
}


@Composable
fun HomeScreen(state: AuthState.LoggedIn) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Добро пожаловать в Home!", style = MaterialTheme.typography.headlineMedium)
        Text("Email: ${state.user.email}", style = MaterialTheme.typography.bodyLarge)
    }
}