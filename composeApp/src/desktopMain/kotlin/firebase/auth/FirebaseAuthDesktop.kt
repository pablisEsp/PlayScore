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
import util.EnvironmentConfig

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

class FirebaseAuthDesktop private constructor() : FirebaseAuthInterface {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        // Add logging to debug HTTP requests
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    // Get API URL from environment variable or use default for local development
    private val apiUrl = EnvironmentConfig.AUTH_API_URL
    private var currentUser: UserInfo? = null
    private var idToken: String? = null

    // Singleton pattern implementation
    companion object {
        @Volatile
        private var instance: FirebaseAuthDesktop? = null

        fun getInstance(): FirebaseAuthDesktop {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthDesktop().also { instance = it }
            }
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
                println("Registration successful, token received: ${idToken?.take(10)}...")

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
                println("Login successful, token received: ${idToken?.take(10)}...")
                println("Current user set to: $currentUser")

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
    }

    override suspend fun getIdToken(): String {
        println("getIdToken called, token ${if (idToken == null) "is null" else "exists"}")
        return idToken ?: ""
    }

}

actual fun createFirebaseAuth(): FirebaseAuthInterface = FirebaseAuthDesktop.getInstance()