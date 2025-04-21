package viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import data.AuthService
import data.model.LoginRequest
import data.model.User
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
        
    var currentUser by mutableStateOf<User?>(null)
        private set
        
    var isLoggedIn by mutableStateOf(false)
        private set

    init {
        // Check if user is already logged in
        isLoggedIn = authService.isLoggedIn()
        currentUser = authService.getCurrentUser()
    }

    fun onEmailChanged(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChanged(newPassword: String) {
        password = newPassword
    }

    fun login(onLoginSuccess: () -> Unit = {}) {
        isLoading = true
        loginResult = null

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = authService.login(LoginRequest(email, password))
                currentUser = result.user
                isLoggedIn = true
                loginResult = "Login successful!"
                onLoginSuccess()
            } catch (e: Exception) {
                loginResult = "Login failed: ${e.message}"
                isLoggedIn = false
                currentUser = null
            } finally {
                isLoading = false
            }
        }
    }
    
    fun logout() {
        authService.logout()
        isLoggedIn = false
        currentUser = null
        loginResult = null
    }
}