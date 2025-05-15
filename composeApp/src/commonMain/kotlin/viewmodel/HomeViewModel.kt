package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.TokenManager
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.auth.createFirebaseAuth
import firebase.database.FirebaseDatabaseInterface
import firebase.database.createFirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class HomeViewModel(
    private val auth: FirebaseAuthInterface = createFirebaseAuth(),
    private val database: FirebaseDatabaseInterface = createFirebaseDatabase(),
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadCurrentUser()
        }
    }

    private suspend fun loadCurrentUser() {
        try {
            // First try TokenManager
            val savedUser = tokenManager.getValidatedUser()
            if (savedUser != null) {
                withContext(Dispatchers.Main) {
                    _currentUser.value = savedUser
                    _isLoading.value = false
                }
                return
            }

            // Then check Firebase auth
            val authUser = auth.getCurrentUser()
            if (authUser == null) {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
                return
            }

            // Load user data with a timeout
            withTimeout(10000) {
                val userData = database.getUserData(authUser.uid)

                withContext(Dispatchers.Main) {
                    if (userData != null) {
                        tokenManager.saveAuthData(auth.getIdToken() ?: "", userData)
                        _currentUser.value = userData
                    } else {
                        _currentUser.value = User(
                            id = authUser.uid,
                            name = authUser.displayName ?: "User",
                            email = authUser.email ?: "",
                            username = ""
                        )
                    }
                    _isLoading.value = false
                }
            }
        } catch (e: Exception) {
            println("ERROR loading user: ${e.message}")
            auth.getCurrentUser()?.let { authUser ->
                withContext(Dispatchers.Main) {
                    _currentUser.value = User(
                        id = authUser.uid,
                        name = authUser.displayName ?: "User",
                        email = authUser.email ?: "",
                        username = ""
                    )
                    _isLoading.value = false
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            tokenManager.clearAuthData()
            withContext(Dispatchers.Main) {
                _currentUser.value = null
            }
        }
    }
}