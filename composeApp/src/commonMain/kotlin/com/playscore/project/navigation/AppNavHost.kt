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

    // Add debug logging
    LaunchedEffect(currentDestination) {
        println("Navigation changed to: $currentDestination")
    }

    // Check if user is already signed in
    LaunchedEffect(Unit) {
        println("Checking if user is already signed in")
        if (firebaseAuth.getCurrentUser() != null) {
            println("User is already signed in, navigating to Home")
            // User is already signed in, navigate to Home as root
            navigationManager.navigateToRoot(Destination.Home)
        } else {
            println("No signed-in user found")
        }
    }

    when (currentDestination) {
        Destination.Login -> {
            println("Rendering Login screen")
            // Using koinViewModel to get the ViewModel through Koin
            val viewModel = koinViewModel<LoginViewModel>()

            // Observe login state changes and navigate when isLoggedIn becomes true
            val isLoggedIn = viewModel.isLoggedIn
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    println("Login state changed to true - navigating to Home")
                    navigationManager.navigateToRoot(Destination.Home)
                    // Reset login state after navigation
                    viewModel.resetLoginState()
                }
            }

            LoginScreen(
                viewModel = viewModel,
                onRegisterClick = {
                    println("Register button clicked")
                    navigationManager.navigateTo(Destination.Register)
                }
                // No onLoginSuccess parameter here anymore
            )
        }

        Destination.Register -> {
            println("Rendering Register screen")
            val viewModel = koinViewModel<RegisterViewModel>()

            // Same pattern here - observe registration success state
            val isRegistrationComplete = viewModel.isRegistrationComplete
            LaunchedEffect(isRegistrationComplete) {
                if (isRegistrationComplete) {
                    println("Registration complete - navigating to Login")
                    navigationManager.navigateTo(Destination.Login)
                    // Reset registration state
                    viewModel.resetRegistrationState()
                }
            }

            RegisterScreen(
                viewModel = viewModel,
                onBackToLogin = {
                    println("Back to login button clicked")
                    navigationManager.navigateTo(Destination.Login)
                }
                // No onRegisterSuccess parameter here anymore
            )
        }

        Destination.Home -> {
            println("Rendering Home screen")
            val viewModel = koinViewModel<HomeViewModel>()

            // Observe logout state
            val isLoggedIn = viewModel.isLoggedIn
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    println("User logged out - navigating to Login")
                    navigationManager.navigateToRoot(Destination.Login)
                }
            }

            HomeScreen(
                viewModel = viewModel
                // No onLogout parameter here anymore
            )
        }
    }
}
