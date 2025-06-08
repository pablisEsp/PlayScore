package firebase.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import utils.EnvironmentConfig

@Serializable
data class AuthRequestBody(val email: String, val password: String)

@Serializable
data class ProfileUpdateBody(val displayName: String)

@Serializable
data class AuthResponseBody(
    val success: Boolean,
    val userId: String? = null,
    val token: String? = null,
    val errorMessage: String? = null,
    val email: String? = null,
    val displayName: String? = null
)

@Serializable
private data class PersistedAuthState(
    val userId: String,
    val email: String,
    val displayName: String,
    val tokenExpiry: Long
)

class FirebaseAuthDesktop private constructor() : FirebaseAuthInterface {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        // Logging to debug HTTP requests
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    // Get API URL from environment variable or use default for local development
    private val apiUrl = EnvironmentConfig.AUTH_API_URL
    private var currentUser: UserInfo? = null
    private var idToken: String? = null
    private var tokenExpiry: Long = 0

    // Secure storage keys
    private val KEY_AUTH_STATE = "auth_state"
    private val KEY_TOKEN = "auth_token"

    // Secure storage implementation
    private val secureStorage = createSecureStorage()

    // Singleton pattern implementation
    companion object {
        @Volatile
        private var instance: FirebaseAuthDesktop? = null

        fun getInstance(): FirebaseAuthDesktop {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthDesktop().also {
                    instance = it
                }
            }
        }
    }

    init {
        // Load auth state when initializing the instance
        loadAuthState()
    }

    private fun saveAuthState() {
        try {
            val currentUser = this.currentUser ?: return

            // Save user info
            val authState = PersistedAuthState(
                userId = currentUser.uid,
                email = currentUser.email,
                displayName = currentUser.displayName,
                tokenExpiry = tokenExpiry
            )

            val json = Json.encodeToString(authState)
            secureStorage.saveString(KEY_AUTH_STATE, json)

            // Save token separately for added security
            idToken?.let { token ->
                secureStorage.saveString(KEY_TOKEN, token)
            }

            println("Auth state saved securely")
        } catch (e: Exception) {
            println("Failed to save auth state: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadAuthState() {
        try {
            // Load auth state
            val json = secureStorage.getString(KEY_AUTH_STATE) ?: run {
                println("No saved auth state found")
                return
            }

            val authState = Json.decodeFromString<PersistedAuthState>(json)

            // Load token separately
            val token = secureStorage.getString(KEY_TOKEN)

            // Check if token is still valid (has not expired)
            val currentTimeMillis = System.currentTimeMillis()
            if (authState.tokenExpiry > currentTimeMillis && token != null) {
                currentUser = UserInfo(
                    uid = authState.userId,
                    email = authState.email,
                    displayName = authState.displayName
                )
                idToken = token
                tokenExpiry = authState.tokenExpiry

                println("Auth state loaded successfully. Token valid for ${(tokenExpiry - currentTimeMillis) / 1000} more seconds")
            } else {
                println("Saved token has expired, will try to refresh")
                // We'll keep the user info but mark token as expired
                currentUser = UserInfo(
                    uid = authState.userId,
                    email = authState.email,
                    displayName = authState.displayName
                )
                idToken = null
                tokenExpiry = 0
            }
        } catch (e: Exception) {
            println("Failed to load auth state: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun createUser(email: String, password: String): AuthResult {
        return try {
            println("Attempting to register user with email: $email at $apiUrl/register")
            val response = withContext(Dispatchers.IO) {
                client.post("$apiUrl/register") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequestBody(email, password))
                }
            }

            val responseBody = response.body<AuthResponseBody>()
            println("Registration response: success=${responseBody.success}")

            if (responseBody.success) {
                currentUser = UserInfo(
                    uid = responseBody.userId ?: "",
                    email = responseBody.email ?: email,
                    displayName = responseBody.displayName ?: ""
                )
                idToken = responseBody.token
                // Set token expiry to 1 hour from now
                tokenExpiry = System.currentTimeMillis() + 3600000
                println("Registration successful, token received: ${idToken?.take(10)}...")

                // Save auth state to persist between app restarts
                saveAuthState()

                AuthResult(success = true, userId = responseBody.userId)
            } else {
                println("Registration failed: ${responseBody.errorMessage}")
                AuthResult(
                    success = false,
                    errorMessage = responseBody.errorMessage ?: "Registration failed"
                )
            }
        } catch (e: Exception) {
            println("Registration exception: ${e.message}")
            e.printStackTrace()
            AuthResult(success = false, errorMessage = "Network error: ${e.message}")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            println("Attempting to login with email: $email at $apiUrl/login")
            val response = withContext(Dispatchers.IO) {
                client.post("$apiUrl/login") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequestBody(email, password))
                }
            }
            println("Login response status: ${response.status}")

            val responseBody = response.body<AuthResponseBody>()
            println("Login response: success=${responseBody.success}")

            if (responseBody.success && responseBody.token != null) {
                currentUser = UserInfo(
                    uid = responseBody.userId ?: "",
                    email = responseBody.email ?: email,
                    displayName = responseBody.displayName ?: ""
                )
                idToken = responseBody.token
                // Set token expiry to 1 hour from now
                tokenExpiry = System.currentTimeMillis() + 3600000
                println("Login successful. Full token received (is null: ${idToken == null}, is blank: ${idToken?.isBlank()}): $idToken") // Log the full token
                println("Current user set to: $currentUser")

                // Save auth state to persist between app restarts
                saveAuthState()

                AuthResult(success = true, userId = responseBody.userId)
            } else {
                println("Login failed: ${responseBody.errorMessage}")
                AuthResult(
                    success = false,
                    errorMessage = responseBody.errorMessage ?: "Login failed"
                )
            }
        } catch (e: Exception) {
            println("Login exception: ${e.message}")
            e.printStackTrace()
            AuthResult(success = false, errorMessage = "Network error: ${e.message}")
        }
    }

    private suspend fun refreshToken(): Boolean {
        // Only try to refresh if we have a token to refresh
        if (currentUser == null) return false

        try {
            println("Attempting to refresh token for user: ${currentUser?.email}")

            val response = withContext(Dispatchers.IO) {
                client.post("$apiUrl/refresh-token") {
                    contentType(ContentType.Application.Json)
                    // Send the current token in the Authorization header
                    header("Authorization", "Bearer $idToken")
                }
            }

            val responseBody = response.body<AuthResponseBody>()
            if (responseBody.success && responseBody.token != null) {
                idToken = responseBody.token
                // Set new expiry time - 1 hour from now
                tokenExpiry = System.currentTimeMillis() + 3600000
                saveAuthState()
                println("Token refreshed successfully")
                return true
            } else {
                println("Token refresh failed: ${responseBody.errorMessage}")
                return false
            }
        } catch (e: Exception) {
            println("Token refresh exception: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    override suspend fun updateUserProfile(displayName: String) {
        if (idToken == null) {
            println("Cannot update profile: No authentication token")
            return
        }

        try {
            println("Updating profile with displayName: $displayName")
            println("Using token: ${idToken?.take(10)}...")

            val response = withContext(Dispatchers.IO) {
                client.post("$apiUrl/update-profile") {
                    contentType(ContentType.Application.Json)
                    setBody(ProfileUpdateBody(displayName))
                    header("Authorization", "Bearer $idToken")
                }
            }

            val responseBody = response.body<AuthResponseBody>()
            if (responseBody.success) {
                currentUser = currentUser?.copy(displayName = displayName)
                println("Profile updated successfully")

                // Save updated user info
                saveAuthState()
            } else {
                println("Failed to update profile: ${responseBody.errorMessage}")
            }
        } catch (e: Exception) {
            println("Profile update exception: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getCurrentUser(): UserInfo? {
        println("getCurrentUser called, returning: $currentUser")
        return currentUser
    }

    override fun signOut() {
        println("Signing out user: ${currentUser?.email}")
        currentUser = null
        idToken = null
        tokenExpiry = 0

        // Delete the saved auth state
        secureStorage.clear()
        println("Auth state cleared")
    }

    override suspend fun getIdToken(): String {
        println("getIdToken called, token ${if (idToken == null) "is null" else "exists"}")

        // Check if token has expired or is about to expire
        if (idToken != null) {
            val currentTime = System.currentTimeMillis()
            val timeRemaining = tokenExpiry - currentTime

            // If token has less than 5 minutes remaining, refresh it
            if (timeRemaining < 300000) {
                println("Token expired or about to expire, attempting refresh")
                if (!refreshToken()) {
                    println("Token refresh failed, user will need to login again")
                    // Clear token but keep user info
                    idToken = null
                }
            }
        } else if (currentUser != null) {
            // We have user info but no token, try to refresh
            refreshToken()
        }

        return idToken ?: ""
    }

    override suspend fun sendEmailVerification(): Boolean {
        val token = getIdToken()
        if (token.isBlank()) {
            println("Cannot send verification email: No authentication token")
            return false
        }

        try {
            println("Sending verification email to user: ${currentUser?.email}")
            val response = withContext(Dispatchers.IO) {
                client.post("$apiUrl/send-verification-email") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $token")
                }
            }

            val responseBody = response.body<AuthResponseBody>()
            return if (responseBody.success) {
                println("Verification email sent successfully")
                true
            } else {
                println("Failed to send verification email: ${responseBody.errorMessage}")
                false
            }
        } catch (e: Exception) {
            println("Send verification email exception: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}

actual fun createFirebaseAuth(): FirebaseAuthInterface = FirebaseAuthDesktop.getInstance()