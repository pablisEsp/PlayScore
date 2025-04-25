package viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import auth.FirebaseAuthInterface
import com.playscore.project.navigation.Destination
import com.playscore.project.navigation.NavigationManager
import data.TokenManager
import data.model.User
import database.FirebaseDatabaseInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class LoginViewModel(
    private val coroutineContext: CoroutineContext = Dispatchers.Main,
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface,
    private val tokenManager: TokenManager,
    private val navigationManager: NavigationManager
) : ViewModel() {
    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loginMessage by mutableStateOf<String?>(null)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    fun onEmailChanged(value: String) {
        email = value
    }

    fun onPasswordChanged(value: String) {
        password = value
    }

    fun login(onLoginSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            loginMessage = "Email and password cannot be empty"
            return
        }

        isLoading = true
        loginMessage = null

        CoroutineScope(coroutineContext).launch {
            try {
                val authResult = auth.signIn(email, password)

                if (authResult.success) {
                    println("Authentication successful for user: ${authResult.userId}")

                    withContext(Dispatchers.IO) {
                        try {
                            val userData = database.getUserData(authResult.userId)
                            println("User data loaded: $userData")

                            if (userData != null) {
                                tokenManager.saveAuthData(
                                    token = auth.getIdToken() ?: "",
                                    user = userData as User
                                )

                                withContext(coroutineContext) {
                                    isLoggedIn = true
                                    isLoading = false
                                    navigationManager.navigateToRoot(Destination.Home)
                                }
                            } else {
                                withContext(coroutineContext) {
                                    loginMessage = "Could not load user data. Please try again."
                                    isLoading = false
                                }
                            }
                        } catch (e: Exception) {
                            println("Error loading user data")
                            withContext(coroutineContext) {
                                loginMessage = "Error loading your user profile: ${e.message}"
                                isLoading = false
                            }
                        }
                    }
                } else {
                    loginMessage = authResult.errorMessage ?: "Login failed"
                    isLoading = false
                }
            } catch (e: Exception) {
                println("Login error")
                loginMessage = e.message ?: "An error occurred during login"
                isLoading = false
            }
        }
    }

    fun resetLoginState() {
        loginMessage = null
        isLoggedIn = false
    }

    fun clearForm() {
        email = ""
        password = ""
        loginMessage = null
    }
}