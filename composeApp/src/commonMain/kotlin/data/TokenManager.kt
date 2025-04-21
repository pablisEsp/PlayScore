package data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.User

/**
 * Manages authentication tokens and user session information
 */
class TokenManager {
    // Current authentication token
    var token by mutableStateOf<String?>(null)
        private set
    
    // Current logged in user
    var currentUser by mutableStateOf<User?>(null)
        private set
    
    // Authentication state
    val isLoggedIn: Boolean
        get() = token != null && currentUser != null
    
    // Save authentication data
    fun saveAuthData(newToken: String, user: User?) {
        token = newToken
        currentUser = user
    }
    
    // Clear authentication data (logout)
    fun clearAuthData() {
        token = null
        currentUser = null
    }
}
