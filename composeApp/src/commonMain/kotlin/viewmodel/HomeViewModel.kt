package viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import data.AuthService
import data.model.User

class HomeViewModel(
    private val authService: AuthService
) : ViewModel() {
    // Current user information
    var currentUser by mutableStateOf<User?>(null)
        private set
        
    // Authentication state
    var isLoggedIn by mutableStateOf(false)
        private set
        
    init {
        // Check if user is logged in and get current user
        isLoggedIn = authService.isLoggedIn()
        currentUser = authService.getCurrentUser()
    }
    
    // Logout function
    fun logout() {
        authService.logout()
        isLoggedIn = false
        currentUser = null
    }
}
