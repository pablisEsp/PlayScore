package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.TokenManager
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
                if (!authResult.success) {
                    _loginMessage.value = authResult.errorMessage ?: "Login failed"
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
                    withContext(Dispatchers.IO) {
                        val token = auth.getIdToken() ?: ""
                        tokenManager.saveAuthData(token, userData)
                    }
                    _isLoggedIn.value = true
                } else {
                    _loginMessage.value = "Failed to load user data"
                }
            } catch (e: Exception) {
                _loginMessage.value = "Login error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add a new function to resend verification email
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