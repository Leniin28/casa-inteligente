package com.ecore.demo2.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Contenedor principal: barra inferior + NavHost.
 * También redirige a Login o a Inicio cuando cambia la sesión.
 */
@Composable
fun SmartHomeApp(isLoggedIn: Boolean?) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    LaunchedEffect(isLoggedIn) {
        val loggedIn = isLoggedIn ?: return@LaunchedEffect
        val destination = navController.currentDestination
        val onAuthScreen = destination != null && AUTH_ROUTES.any { destination.hasRoute(it) }
        val onSplash = destination?.hasRoute(Routes.Splash::class) ?: true
        when {
            loggedIn && (onSplash || onAuthScreen) -> navController.navigate(Routes.HomeGraph) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
            !loggedIn && !onAuthScreen -> navController.navigate(Routes.Login) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    val showBottomBar = isLoggedIn == true &&
        TopLevelDestination.entries.any { currentDestination.isInGraph(it) }

    Scaffold(
        // Cada pantalla gestiona su barra superior; aquí solo se reserva la barra inferior.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination.isInGraph(destination),
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private val AUTH_ROUTES = listOf(Routes.Login::class, Routes.Register::class, Routes.ForgotPassword::class)

private fun NavDestination?.isInGraph(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.graphClass) } == true

private fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.graph) {
        // Vuelve a Inicio al pulsar "atrás" y conserva el estado de cada pestaña.
        popUpTo(Routes.Dashboard) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
