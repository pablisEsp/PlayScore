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
        if (_name.value.isBlank() || _email.value.isBlank() || _password.value.isBlank()) {
            _registerResult.value = "All fields are required"
            return
        }

        _isLoading.value = true
        _registerResult.value = null

        CoroutineScope(coroutineContext).launch {
            try {
                val result = auth.createUser(_email.value, _password.value)

                if (result.success) {
                    auth.updateUserProfile(_name.value)

                    result.userId?.let { uid ->
                        val userData = User(
                            id = uid,
                            name = _name.value,
                            email = _email.value,
                            globalRole = UserRole.USER,
                            createdAt = Clock.System.now().toString()
                        )

                        val saveResult = database.saveUserData(userData)
                        if (!saveResult) {
                            println("Warning: Failed to save user data to database")
                        }
                    }

                    _isRegistrationComplete.value = true
                    _registerResult.value = "Registered successfully"
                } else {
                    _registerResult.value = when {
                        result.errorMessage?.contains("email already in use", ignoreCase = true) == true -> "Email already in use"
                        result.errorMessage?.contains("password is invalid", ignoreCase = true) == true -> "Password should be at least 6 characters"
                        else -> "Registration failed: ${result.errorMessage}"
                    }
                    _isRegistrationComplete.value = false
                }
            } catch (e: Exception) {
                _registerResult.value = "Registration failed: ${e.message}"
                _isRegistrationComplete.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetRegistrationState() {
        _isRegistrationComplete.value = false
        _registerResult.value = null
    }
}