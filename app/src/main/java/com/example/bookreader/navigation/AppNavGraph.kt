package com.example.bookreader.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.auth.mvi.AuthIntent
import com.example.auth.mvi.AuthState
import com.example.auth.presentation.LoginScreen
import com.example.auth.viewmodel.AuthViewModel
import com.example.bookreader.presentation.BookReaderBottomBar
import com.example.books.presentation.BooksScreen
import com.example.navigation.AppRoute
import com.example.navigation.AuthRoute
import com.example.navigation.HomeRoute
import com.example.navigation.ReaderRoute
import com.example.profile.presentation.ProfileScreen
import com.example.reader.presentation.ReaderScreen
import com.example.upload.presentation.UploadScreen

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val snackBarHostState = remember { SnackbarHostState() }

    val authState by authViewModel.state.collectAsState()

    val startDestination = when (authState) {
        is AuthState.Loading -> LoadingScreen()
        is AuthState.LoggedIn -> AppRoute.HomeGraph
        is AuthState.LoggedOut -> AppRoute.AuthGraph
    }


    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<AppRoute.Loading> { LoadingScreen() }

        authGraph(navController, authViewModel, snackBarHostState)
        homeGraph(navController, authViewModel)
        readerGraph()
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

fun NavGraphBuilder.authGraph(
    navController: NavController,
    authViewModel: AuthViewModel,
    snackBarHostState: SnackbarHostState
) {
    navigation<AppRoute.AuthGraph>(
        startDestination = AuthRoute.LoginScreen
    ) {

        composable<AuthRoute.LoginScreen> {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate(AppRoute.HomeGraph) {
                        popUpTo(AppRoute.AuthGraph) { inclusive = true }
                    }
                },
                snackBarHostState = snackBarHostState
            )

        }
    }
}

fun NavGraphBuilder.homeGraph(
    parentNavController: NavController,
    authViewModel: AuthViewModel
) {
    navigation<AppRoute.HomeGraph>(
        startDestination = AppRoute.HomeRoot
    ) {

        composable<AppRoute.HomeRoot> {
            val tabNavController = rememberNavController()

            Scaffold(
                bottomBar = { BookReaderBottomBar(tabNavController) }
            ) { padding ->

                NavHost(
                    navController = tabNavController,
                    startDestination = HomeRoute.Books,
                    modifier = Modifier.padding(padding)
                ) {

                    composable<HomeRoute.Books> {
                        BooksScreen { id, path, title ->
                            parentNavController.navigate(ReaderRoute.BookReader(id, path, title))
                        }
                    }

                    composable<HomeRoute.Upload> {
                        UploadScreen()
                    }

                    composable<HomeRoute.Profile> {
                        ProfileScreen {
                            authViewModel.processIntent(AuthIntent.Logout)
                            parentNavController.navigate(AppRoute.AuthGraph) {
                                popUpTo(AppRoute.HomeGraph) { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun NavGraphBuilder.readerGraph() {
    composable<ReaderRoute.BookReader> { entry ->
        val bookId = entry.arguments?.getString("bookId")!!
        val localPath = entry.arguments?.getString("localPath")!!
        val bookTitle = entry.arguments?.getString("bookTitle")!!
        ReaderScreen(bookId, localPath, bookTitle)
    }
}