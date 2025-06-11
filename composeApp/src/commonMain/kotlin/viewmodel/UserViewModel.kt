package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UserViewModel(
    private val auth: FirebaseAuthInterface,
    private val database: FirebaseDatabaseInterface
) : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBanned = MutableStateFlow(false)
    val isBanned: StateFlow<Boolean> = _isBanned.asStateFlow()

    private var banCheckJob: Job? = null


    init {
        loadCurrentUser()
        // Start periodic ban check
        startBanCheck()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = auth.getCurrentUser()?.uid
                if (uid != null) {
                    _currentUser.value = database.getUserData(uid)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load user data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uid = auth.getCurrentUser()?.uid
                if (uid != null) {
                    val success = database.updateUsername(uid, newUsername)
                    if (success) {
                        loadCurrentUser() // Reload user data with new username
                    } else {
                        _errorMessage.value = "Username already taken or update failed"
                    }
                } else {
                    _errorMessage.value = "User not logged in"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating username: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startBanCheck() {
        banCheckJob = viewModelScope.launch {
            while(isActive) {
                checkUserBanStatus()
                // Check every minute
                delay(60000)
            }
        }
    }

    private suspend fun checkUserBanStatus() {
        try {
            val uid = auth.getCurrentUser()?.uid
            if (uid != null) {
                // Fetch fresh user data directly from database
                val freshUserData = database.getUserData(uid)

                if (freshUserData?.isBanned == true) {
                    println("User is banned, showing dialog")
                    // Important: Set isBanned FIRST, then wait for user to dismiss dialog before logout
                    _isBanned.value = true

                    // Don't sign out here - we'll do it after the user dismisses the dialog
                    // This way the user sees the dialog and we avoid permission errors
                }
            }
        } catch (e: Exception) {
            println("Error checking ban status: ${e.message}")
            _errorMessage.value = "Error checking user status: ${e.message}"
        }
    }

    // Add this new function to handle signout after dialog is dismissed
    fun signOutBannedUser() {
        viewModelScope.launch {
            try {
                println("Signing out banned user")
                auth.signOut()
                _currentUser.value = null
            } catch (e: Exception) {
                println("Error during signout: ${e.message}")
            }
        }
    }

    fun clearBanStatus() {
        _isBanned.value = false
    }

    override fun onCleared() {
        super.onCleared()
        banCheckJob?.cancel()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}