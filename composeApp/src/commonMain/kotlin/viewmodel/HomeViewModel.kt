package viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import auth.FirebaseAuthInterface
import auth.createFirebaseAuth
import data.TokenManager
import data.model.User
import database.FirebaseDatabaseInterface
import database.createFirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

class HomeViewModel(
    private val coroutineContext: CoroutineContext = Dispatchers.Main,
    private val auth: FirebaseAuthInterface = createFirebaseAuth(),
    private val database: FirebaseDatabaseInterface = createFirebaseDatabase(),
    private val tokenManager: TokenManager
) : ViewModel() {
    var currentUser by mutableStateOf<User?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isLoggedIn by mutableStateOf(true)
        private set

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        // First, try to get user from TokenManager
        val savedUser = tokenManager.getValidatedUser()
        if (savedUser != null) {
            currentUser = savedUser
            isLoading = false
            return
        }

        val authUser = auth.getCurrentUser() ?: run {
            isLoading = false
            isLoggedIn = false
            return
        }

        CoroutineScope(coroutineContext).launch {
            try {
                withTimeout(10000) { // 10 second timeout
                    val userData = database.getUserData(authUser.uid)
                    currentUser = userData
                    isLoggedIn = userData != null

                    // Save to token manager if found
                    if (userData != null) {
                        tokenManager.saveAuthData(
                            token = auth.getIdToken() ?: "",
                            user = userData
                        )
                    }
                }
            } catch (e: Exception) {
                // Create minimal user as fallback
                currentUser = User(
                    id = authUser.uid,
                    name = authUser.displayName ?: "User",
                    email = authUser.email ?: ""
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        tokenManager.clearAuthData() // Make sure to clear saved data
        currentUser = null
        isLoggedIn = false
    }
}