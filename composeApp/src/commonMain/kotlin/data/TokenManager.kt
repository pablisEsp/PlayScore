package data

import data.model.User
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TokenManager {
    private var token: String? = null
    private var tokenExpiration: Long = 0
    var currentUser: User? = null
        private set
    
    val isLoggedIn: Boolean
        get() = token != null && currentUser != null && validateToken()
    
    @OptIn(ExperimentalTime::class)
    fun saveAuthData(token: String, user: User, expiresIn: Long = 3600) {
        this.token = token
        this.currentUser = user
        // Calculate expiration timestamp (current time + expires_in in seconds)
        this.tokenExpiration = Clock.System.now().epochSeconds + expiresIn
    }
    
    fun clearAuthData() {
        token = null
        currentUser = null
        tokenExpiration = 0
    }
    
    // Get the stored token if it's valid
    fun getToken(): String? {
        return if (validateToken()) token else null
    }
    
    // Check if the token is still valid
    @OptIn(ExperimentalTime::class)
    fun validateToken(): Boolean {
        val currentTime = Clock.System.now().epochSeconds
        return token != null && currentTime < tokenExpiration
    }
    
    // Get token expiration time in seconds from now
    @OptIn(ExperimentalTime::class)
    fun getTokenRemainingTime(): Long {
        val currentTime = Clock.System.now().epochSeconds
        return (tokenExpiration - currentTime).coerceAtLeast(0)
    }
}
