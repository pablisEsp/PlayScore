package viewmodel

import androidx.lifecycle.ViewModel
import data.model.User
import data.model.UserRole
import firebase.auth.FirebaseAuthInterface
import firebase.auth.createFirebaseAuth
import firebase.database.FirebaseDatabaseInterface
import firebase.database.createFirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.compareTo
import kotlin.coroutines.CoroutineContext

class RegisterViewModel(
    private val coroutineContext: CoroutineContext,
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface
) : ViewModel() {
    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _registerResult = MutableStateFlow<String?>(null)
    val registerResult = _registerResult.asStateFlow()

    private val _isRegistrationComplete = MutableStateFlow(false)
    val isRegistrationComplete = _isRegistrationComplete.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _isEmailVerificationRequired = MutableStateFlow(false)
    val isEmailVerificationRequired = _isEmailVerificationRequired.asStateFlow()

    private val _confirmEmail = MutableStateFlow("")
    val confirmEmail = _confirmEmail.asStateFlow()

    fun onConfirmEmailChanged(value: String) {
        _confirmEmail.value = value
    }

    fun onUsernameChanged(value: String) {
        _username.value = value
    }

    fun onNameChanged(value: String) {
        _name.value = value
    }

    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun register() {
        if (_name.value.isBlank() || _email.value.isBlank() || _password.value.isBlank() || _username.value.isBlank()) {
            _registerResult.value = "All fields including username are required"
            return
        }

        if (_email.value != _confirmEmail.value) {
            _registerResult.value = "Email addresses don't match"
            return
        }

        // Username validation
        if (_username.value.length < 3) {
            _registerResult.value = "Username must be at least 3 characters"
            return
        }

        if (!_username.value.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _registerResult.value = "Username can only contain letters, numbers and underscore"
            return
        }

        // Email format validation
        if (!isValidEmail(_email.value)) {
            _registerResult.value = "Please enter a valid email address"
            return
        }

        _isLoading.value = true
        _registerResult.value = null

        CoroutineScope(coroutineContext).launch {
            try {
                // First check if username is available
                val isUsernameAvailable = database.checkUsernameAvailable(_username.value)
                if (!isUsernameAvailable) {
                    _registerResult.value = "Username already taken"
                    _isLoading.value = false
                    return@launch
                }

                val result = auth.createUser(_email.value, _password.value)

                if (result.success) {
                    // Send email verification
                    auth.sendEmailVerification()

                    auth.updateUserProfile(_name.value)

                    result.userId?.let { uid ->
                        val userData = User(
                            id = uid,
                            name = _name.value,
                            email = _email.value,
                            username = _username.value,
                            globalRole = UserRole.USER,
                            createdAt = Clock.System.now().toString()
                        )

                        val saveResult = database.saveUserData(userData)
                        // Make sure to store the username in the username registry
                        val usernameResult = database.updateUsername(uid, _username.value)

                        if (!saveResult || !usernameResult) {
                            println("Warning: Failed to save user data or username")
                        }
                    }

                    // Indicate verification required instead of complete
                    _registerResult.value = "Registered successfully. Please verify your email."
                    _isEmailVerificationRequired.value = true
                } else {
                    // Handle specific Firebase auth errors
                    val errorMessage = when {
                        result.errorMessage?.contains("email address is badly formatted") == true ->
                            "Please enter a valid email address"
                        result.errorMessage?.contains("password is invalid") == true ->
                            "Password must be at least 6 characters"
                        result.errorMessage?.contains("email address is already in use") == true ->
                            "Email address is already in use"
                        result.errorMessage?.contains("network error") == true ->
                            "Network error. Please check your connection and try again"
                        else -> result.errorMessage ?: "Registration failed"
                    }
                    _registerResult.value = errorMessage
                    _isRegistrationComplete.value = false
                }
            } catch (e: Exception) {
                // Extract meaningful error messages from exceptions
                val errorMessage = when {
                    e.message?.contains("badly formatted") == true ->
                        "The email address is badly formatted"
                    e.message?.contains("password") == true ->
                        "Password must be at least 6 characters"
                    e.message?.contains("already in use") == true ->
                        "Email address is already in use"
                    e.message?.contains("network") == true ->
                        "Network error. Please check your connection"
                    e.message?.contains("too many requests") == true ->
                        "Too many attempts. Please try again later"
                    else -> "Registration failed: ${e.message}"
                }
                _registerResult.value = errorMessage
                _isRegistrationComplete.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add this helper function to validate email format
    private fun isValidEmail(email: String): Boolean {
        val pattern = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
        )
        return pattern.matches(email)
    }

    fun resetRegistrationState() {
        _isRegistrationComplete.value = false
        _registerResult.value = null
    }
}