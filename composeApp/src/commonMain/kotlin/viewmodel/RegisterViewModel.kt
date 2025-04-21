package viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import data.AuthService
import data.model.RegisterRequest
import data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authService: AuthService
) : ViewModel() {
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
        
    var currentUser by mutableStateOf<User?>(null)
        private set
        
    var isRegistered by mutableStateOf(false)
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

    fun register(onRegisterSuccess: () -> Unit = {}) {
        isLoading = true
        registerResult = null

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = authService.register(
                    RegisterRequest(name, email, password)
                )
                currentUser = result.user
                isRegistered = true
                registerResult = "Registered successfully!"
                onRegisterSuccess()
            } catch (e: Exception) {
                registerResult = "Registration failed: ${e.message}"
                isRegistered = false
                currentUser = null
            } finally {
                isLoading = false
            }
        }
    }
}