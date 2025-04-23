package viewmodel

import auth.createFirebaseAuth
import auth.FirebaseAuthInterface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class RegisterViewModel(
    private val coroutineContext: CoroutineContext = Dispatchers.Main,
    private val auth: FirebaseAuthInterface = createFirebaseAuth()
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

    var isRegistered by mutableStateOf(false)
        private set

    fun onNameChanged(value: String) {
        name = value
    }

    fun onEmailChanged(value: String) {
        email = value
    }

    fun onPasswordChanged(value: String) {
        password = value
    }

    fun register(onRegisterSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            registerResult = "All fields are required"
            return
        }

        isLoading = true
        registerResult = null

        CoroutineScope(coroutineContext).launch {
            try {
                // Create user with email and password
                val result = auth.createUser(email, password)

                if (result.success) {
                    // Update user profile with display name
                    auth.updateUserProfile(name)
                    isRegistered = true
                    registerResult = "Registered successfully"
                    onRegisterSuccess()
                } else {
                    // Extract more specific error codes if needed
                    registerResult = when {
                        result.errorMessage?.contains("email already in use", ignoreCase = true) == true -> "Email already in use"
                        else -> "Registration failed: ${result.errorMessage}"
                    }
                    isRegistered = false
                }
            } catch (e: Exception) {
                registerResult = "Registration failed: ${e.message}"
                isRegistered = false
            } finally {
                isLoading = false
            }
        }
    }

    fun resetRegistrationState() {
        isRegistered = false
        registerResult = null
    }
}
