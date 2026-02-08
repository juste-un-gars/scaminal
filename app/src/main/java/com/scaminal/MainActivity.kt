/**
 * @file MainActivity.kt
 * @description Activité principale hébergeant la navigation Compose avec bottom bar.
 */
package com.scaminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scaminal.ui.favorites.FavoritesScreen
import com.scaminal.ui.scanner.ScannerScreen
import com.scaminal.ui.shortcuts.ShortcutsScreen
import com.scaminal.ui.terminal.TerminalScreen
import com.scaminal.ui.theme.ScaminalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScaminalTheme {
                ScaminalApp()
            }
        }
    }
}

@Composable
private fun ScaminalApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("scanner", "favorites", "shortcuts")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo("scanner") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "scanner"
        ) {
            composable("scanner") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ScannerScreen(
                        onNavigateToTerminal = { ip ->
                            navController.navigate("terminal/$ip")
                        }
                    )
                }
            }
            composable("favorites") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    FavoritesScreen(
                        onNavigateToTerminal = { ip ->
                            navController.navigate("terminal/$ip")
                        }
                    )
                }
            }
            composable("shortcuts") {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ShortcutsScreen()
                }
            }
            composable("terminal/{hostIp}") {
                TerminalScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Item de la barre de navigation inférieure. */
private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem("Scanner", Icons.Default.Search, "scanner"),
    BottomNavItem("Favoris", Icons.Default.Favorite, "favorites"),
    BottomNavItem("Raccourcis", Icons.AutoMirrored.Filled.List, "shortcuts")
)
