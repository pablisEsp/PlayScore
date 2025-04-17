package viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import data.AuthService
import data.model.LoginRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService
) : ViewModel() {
    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loginResult by mutableStateOf<String?>(null)
        private set

    fun onEmailChanged(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChanged(newPassword: String) {
        password = newPassword
    }

    fun login() {
        isLoading = true
        loginResult = null

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = authService.login(LoginRequest(email, password))
                loginResult = "Login successful! Token: ${result.token}"
            } catch (e: Exception) {
                loginResult = "Login failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}