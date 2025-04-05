package viewmodel

import androidx.compose.runtime.*
import data.AuthService
import data.model.RegisterRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authService: AuthService = AuthService()
) {
    var name by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var registerResult by mutableStateOf<String?>(null)
        private set

    fun onNameChanged(newName: String) {
        name = newName
    }

    fun onEmailChanged(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChanged(newPassword: String) {
        password = newPassword
    }

    fun register() {
        isLoading = true
        registerResult = null

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = authService.register(
                    RegisterRequest(name, email, password)
                )
                registerResult = "Registered successfully!"
            } catch (e: Exception) {
                registerResult = "Register failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}