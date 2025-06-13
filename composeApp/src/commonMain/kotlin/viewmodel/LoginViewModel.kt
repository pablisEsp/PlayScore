package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.TokenManager
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface,
    private val tokenManager: TokenManager,
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loginMessage = MutableStateFlow<String?>(null)
    val loginMessage = _loginMessage.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _isEmailVerificationRequired = MutableStateFlow(false)
    val isEmailVerificationRequired = _isEmailVerificationRequired.asStateFlow()

    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun login() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _loginMessage.value = "Email and password cannot be empty"
            return
        }

        _isLoading.value = true
        _loginMessage.value = null

        viewModelScope.launch {
            try {
                val authResult = auth.signIn(_email.value, _password.value)
                // Inside the try block of the login() method
                if (!authResult.success) {
                    _loginMessage.value = when {
                        authResult.errorMessage?.contains("wrong password", ignoreCase = true) == true ->
                            "Incorrect password. Please try again."
                        authResult.errorMessage?.contains("user not found", ignoreCase = true) == true ->
                            "No account found with this email address."
                        authResult.errorMessage?.contains("invalid email", ignoreCase = true) == true ||
                        authResult.errorMessage?.contains("incorrect", ignoreCase = true) == true ||
                        authResult.errorMessage?.contains("malformed", ignoreCase = true) == true ||
                        authResult.errorMessage?.contains("auth credential", ignoreCase = true) == true ||
                        authResult.errorMessage?.contains("recaptcha", ignoreCase = true) == true -> // Added more specific checks
                            "User not found. Please check and try again."
                        authResult.errorMessage?.contains("too many attempts", ignoreCase = true) == true ->
                            "Too many failed attempts. Please try again later."
                        authResult.errorMessage?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your internet connection."
                        else -> authResult.errorMessage ?: "Login failed. Please try again."
                    }
                    _isLoading.value = false
                    return@launch
                }

                // Check if email is verified
                if (!auth.isEmailVerified()) {
                    _loginMessage.value = "Please verify your email before logging in"
                    _isEmailVerificationRequired.value = true
                    _isLoading.value = false
                    return@launch
                }

                val userData = withContext(Dispatchers.IO) {
                    database.getUserData(authResult.userId)
                }

                if (userData != null) {
                    // Check if user is banned
                    if (userData.isBanned == true) {
                        _loginMessage.value = "Your account has been banned. Please contact support."
                        auth.signOut() // Sign out the user immediately
                        _isLoading.value = false
                        return@launch
                    }

                    withContext(Dispatchers.IO) {
                        val token = auth.getIdToken()
                        tokenManager.saveAuthData(token, userData)
                    }
                    _isLoggedIn.value = true
                } else {
                    _loginMessage.value = "Failed to load user data. Please try again."
                }
            } catch (e: Exception) {
                // More specific exception handling
                _loginMessage.value = when {
                    e.message?.contains("wrong password", ignoreCase = true) == true ->
                        "Incorrect password. Please try again."
                    e.message?.contains("user not found", ignoreCase = true) == true ->
                        "No account found with this email address."
                    e.message?.contains("invalid email", ignoreCase = true) == true ||
                            e.message?.contains("incorrect", ignoreCase = true) == true ||
                            e.message?.contains("malformed", ignoreCase = true) == true ||
                            e.message?.contains("auth credential", ignoreCase = true) == true ||
                            e.message?.contains("recaptcha", ignoreCase = true) == true ->
                        "Invalid email format. Please check and try again."
                    e.message?.contains("too many attempts", ignoreCase = true) == true ->
                        "Too many failed attempts. Please try again later."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection."
                    else -> e.message ?: "Login failed. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Function to resend verification email
    fun resendVerificationEmail() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (auth.sendEmailVerification()) {
                    _loginMessage.value = "Verification email sent"
                } else {
                    _loginMessage.value = "Failed to send verification email"
                }
            } catch (e: Exception) {
                _loginMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetLoginState() {
        _isLoggedIn.value = false
        _loginMessage.value = null
    }

    fun clearForm() {
        _email.value = ""
        _password.value = ""
        _loginMessage.value = null
    }
}