package org.example.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ui.login.LoginScreen
import ui.register.RegisterScreen

enum class Screen {
    Login,
    Register
}

@Composable
fun Navigation() {
    var currentScreen by remember { mutableStateOf(Screen.Login) }

    when (currentScreen) {
        Screen.Login -> LoginScreen(
            onRegisterClick = { currentScreen = Screen.Register }
        )

        Screen.Register -> RegisterScreen(
            onBackToLogin = { currentScreen = Screen.Login }
        )
    }
}