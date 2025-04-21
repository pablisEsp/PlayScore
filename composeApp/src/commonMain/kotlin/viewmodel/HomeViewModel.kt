package viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import data.AuthService
import data.model.User

class HomeViewModel(
    private val authService: AuthService
) : ViewModel() {
    // Use mutableStateOf for better interoperability with your existing code
    var currentUser by mutableStateOf<User?>(null)
        private set
        
    var isLoggedIn by mutableStateOf(false)
        private set
        
    init {
        // Load current user on initialization
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        // Get current user from auth service
        currentUser = authService.getCurrentUser()
        isLoggedIn = authService.isLoggedIn()
        
        // If not logged in but trying to access home, log this issue
        if (!isLoggedIn) {
            println("Warning: Accessing HomeViewModel without being logged in")
        }
    }
    
    fun logout() {
        authService.logout()
        currentUser = null
        isLoggedIn = false
    }
}
