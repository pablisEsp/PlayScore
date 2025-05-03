package firebase.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
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

class FirebaseAuthDesktop : FirebaseAuthInterface {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    // Get API URL from environment variable or use default for local development
    private val apiUrl = EnvironmentConfig.AUTH_API_URL
    private var currentUser: UserInfo? = null
    private var idToken: String? = null

    override suspend fun createUser(email: String, password: String): AuthResult {
        return try {
            val response = withContext(Dispatchers.IO) {
                client.post("$apiUrl/register") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequestBody(email, password))
                }
            }

            val responseBody = response.body<AuthResponseBody>()

            if (responseBody.success) {
                currentUser = UserInfo(
                    uid = responseBody.userId ?: "",
                    email = responseBody.email ?: email,
                    displayName = responseBody.displayName ?: ""
                )
                idToken = responseBody.token

                AuthResult(success = true, userId = responseBody.userId)
            } else {
                AuthResult(
                    success = false,
                    errorMessage = responseBody.errorMessage ?: "Registration failed"
                )
            }
        } catch (e: Exception) {
            AuthResult(success = false, errorMessage = "Network error: ${e.message}")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val response = withContext(Dispatchers.IO) {
                client.post("$apiUrl/login") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequestBody(email, password))
                }
            }

            val responseBody = response.body<AuthResponseBody>()

            if (responseBody.success) {
                currentUser = UserInfo(
                    uid = responseBody.userId ?: "",
                    email = responseBody.email ?: email,
                    displayName = responseBody.displayName ?: ""
                )
                idToken = responseBody.token

                AuthResult(success = true, userId = responseBody.userId)
            } else {
                AuthResult(
                    success = false,
                    errorMessage = responseBody.errorMessage ?: "Login failed"
                )
            }
        } catch (e: Exception) {
            AuthResult(success = false, errorMessage = "Network error: ${e.message}")
        }
    }

    override suspend fun updateUserProfile(displayName: String) {
        if (idToken == null) return

        try {
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
            }
        } catch (e: Exception) {
            println("Failed to update profile: ${e.message}")
        }
    }

    override fun getCurrentUser(): UserInfo? = currentUser

    override fun signOut() {
        currentUser = null
        idToken = null
    }

    override fun getIdToken(): String = idToken ?: ""
}

actual fun createFirebaseAuth(): FirebaseAuthInterface = FirebaseAuthDesktop()