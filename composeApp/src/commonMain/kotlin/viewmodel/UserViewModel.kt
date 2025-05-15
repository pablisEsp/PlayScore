package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        loadCurrentUser()
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

    fun clearError() {
        _errorMessage.value = null
    }
}