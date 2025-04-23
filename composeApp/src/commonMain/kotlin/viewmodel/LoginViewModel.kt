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

class LoginViewModel(
    private val coroutineContext: CoroutineContext = Dispatchers.Main,
    private val auth: FirebaseAuthInterface = createFirebaseAuth()
) : ViewModel() {
    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loginResult by mutableStateOf<String?>(null)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    init {
        // Check if user is already logged in
        isLoggedIn = auth.getCurrentUser() != null
    }

    fun onEmailChanged(value: String) {
        email = value
    }

    fun onPasswordChanged(value: String) {
        password = value
    }

    fun login(onLoginSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            loginResult = "Email and password cannot be empty"
            return
        }

        isLoading = true
        loginResult = null

        CoroutineScope(coroutineContext).launch {
            val result = auth.signIn(email, password)

            if (result.success) {
                isLoggedIn = true
                loginResult = "Login successful"
                onLoginSuccess()
            } else {
                // Extract more specific error codes if needed
                loginResult = when {
                    result.errorMessage?.contains("no user record", ignoreCase = true) == true -> "User not found"
                    result.errorMessage?.contains("password is invalid", ignoreCase = true) == true -> "Invalid credentials"
                    else -> "Login failed: ${result.errorMessage}"
                }
                isLoggedIn = false
            }

            isLoading = false
        }
    }

    fun resetLoginState() {
        isLoggedIn = false
        loginResult = null
    }
}
