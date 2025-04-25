package viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import auth.FirebaseAuthInterface
import auth.createFirebaseAuth
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
    private val database: FirebaseDatabaseInterface = createFirebaseDatabase()
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
        val authUser = auth.getCurrentUser() ?: run {
            isLoading = false
            isLoggedIn = false
            return
        }

        CoroutineScope(coroutineContext).launch {
            try {
                withTimeout(10000) { // 5 second timeout
                    val userData = database.getUserData(authUser.uid)
                    currentUser = userData
                    isLoggedIn = true
                }
            } catch (e: Exception) {
                // Failed to get user data, but we still have an auth user
                // Create a minimal user object with the auth data
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
        currentUser = null
        isLoggedIn = false
    }
}