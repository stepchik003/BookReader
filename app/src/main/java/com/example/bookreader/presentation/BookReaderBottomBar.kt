package com.example.bookreader.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.navigation.HomeRoute

private data class NavItem(
    val route: HomeRoute,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BookReaderBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = remember {
        listOf(
            NavItem(HomeRoute.Books, Icons.Filled.Book, "Книги"),
            NavItem(HomeRoute.Upload, Icons.Filled.CloudUpload, "Загрузка"),
            NavItem(HomeRoute.Profile, Icons.Filled.AccountCircle, "Профиль")
        )
    }

    NavigationBar {
        tabs.forEach { navItem ->
            val isSelected = currentRoute == navItem.route::class.qualifiedName

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute == navItem.route::class.qualifiedName) {
                        return@NavigationBarItem
                    }

                    navController.navigate(navItem.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(navItem.icon, contentDescription = navItem.label)
                },
                label = {
                    Text(navItem.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}