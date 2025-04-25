package viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import auth.FirebaseAuthInterface
import auth.createFirebaseAuth
import data.model.User
import data.model.UserRole
import database.FirebaseDatabaseInterface
import database.createFirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext

class RegisterViewModel(
    private val coroutineContext: CoroutineContext = Dispatchers.Main,
    private val auth: FirebaseAuthInterface = createFirebaseAuth(),
    private val database: FirebaseDatabaseInterface = createFirebaseDatabase()
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

                    // Save additional user data to Realtime Database
                    result.userId?.let { uid ->
                        val userData = User(
                            id = uid,
                            name = name,
                            email = email,
                            globalRole = UserRole.USER,
                            createdAt = Clock.System.now().toString(), // ISO-8601 format
                        )

                        val saveResult = database.saveUserData(userData)
                        if (!saveResult) {
                            // Log error but don't fail registration
                            println("Warning: Failed to save user data to database")
                        }
                    }

                    isRegistered = true
                    registerResult = "Registered successfully"
                    onRegisterSuccess()
                } else {
                    // Extract more specific error codes if needed
                    registerResult = when {
                        result.errorMessage?.contains("email already in use", ignoreCase = true) == true -> "Email already in use"
                        result.errorMessage?.contains("password is invalid", ignoreCase = true) == true -> "Password should be at least 6 characters"
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