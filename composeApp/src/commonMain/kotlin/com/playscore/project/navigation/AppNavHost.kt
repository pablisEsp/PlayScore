package com.playscore.project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import auth.createFirebaseAuth
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ui.login.LoginScreen
import ui.register.RegisterScreen
import ui.home.HomeScreen
import viewmodel.LoginViewModel
import viewmodel.RegisterViewModel
import viewmodel.HomeViewModel

/**
 * Main navigation host that displays the current screen based on destination
 */

@Composable
fun AppNavHost() {
    // Get NavigationManager from Koin
    val navigationManager = koinInject<NavigationManager>()
    val currentDestination by navigationManager.currentDestination.collectAsState()

    // Get Firebase auth instance
    val firebaseAuth = createFirebaseAuth()

    // Check if user is already signed in
    LaunchedEffect(Unit) {
        if (firebaseAuth.getCurrentUser() != null) {
            // User is already signed in, navigate to Home
            navigationManager.navigateTo(Destination.Home)
        }
    }


    when (currentDestination) {
        is Destination.Login -> {
            // Using koinViewModel to get the ViewModel through Koin
            val viewModel = koinViewModel<LoginViewModel>()
            LoginScreen(
                viewModel = viewModel,
                onRegisterClick = { navigationManager.navigateTo(Destination.Register) },
                onLoginSuccess = { navigationManager.navigateTo(Destination.Home) }
            )
        }

        is Destination.Register -> {
            val viewModel = koinViewModel<RegisterViewModel>()
            RegisterScreen(
                viewModel = viewModel,
                onBackToLogin = { navigationManager.navigateTo(Destination.Login) },
                onRegisterSuccess = { navigationManager.navigateTo(Destination.Login) }
            )
        }
        
        is Destination.Home -> {
            val viewModel = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = viewModel,
                onLogout = { navigationManager.navigateTo(Destination.Login) }
            )
        }
    }
}