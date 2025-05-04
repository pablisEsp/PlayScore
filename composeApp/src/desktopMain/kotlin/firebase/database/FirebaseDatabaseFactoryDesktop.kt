package firebase.database

import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.auth.createFirebaseAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import util.EnvironmentConfig

class FirebaseDatabaseDesktop(private val auth: FirebaseAuthInterface) : FirebaseDatabaseInterface {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val apiBaseUrl = EnvironmentConfig.getEnv("DATABASE_API_URL") ?: "http://localhost:3000/api"

    override suspend fun saveUserData(userData: User): Boolean {
        // You may want to add a POST /user endpoint on your server for this to work
        try {
            val idToken = auth.getIdToken()
            val response = client.post("$apiBaseUrl/user") {
                contentType(ContentType.Application.Json)
                setBody(userData)
                headers {
                    append("Authorization", "Bearer $idToken")
                }
            }
            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error saving user data: ${e.message}")
            return false
        }
    }

    private suspend fun ensureValidToken(): String {
        val token = auth.getIdToken()
        if (token.isBlank()) {
            println("No valid token available for API request")
        }
        return token
    }

    override suspend fun getUserData(uid: String?): User? {
        val userId = uid ?: auth.getCurrentUser()?.uid ?: return null

        try {
            println("Making GET request to $apiBaseUrl/user/getOne/$userId")

            // Get a fresh token before making the request
            val token = ensureValidToken()
            if (token.isBlank()) {
                println("Unable to get valid token, aborting request")
                return null
            }

            val response = client.get("$apiBaseUrl/user/getOne/$userId") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            println("Response status: ${response.status}")
            if (response.status.isSuccess()) {
                val user = response.body<User>()
                println("User data from response: $user")
                return user
            }
            println("Response not successful: ${response.status}")
            return null
        } catch (e: Exception) {
            println("Error getting user data: ${e.message}")
            return null
        }
    }


    override suspend fun updateUserData(uid: String, updates: Map<String, Any>): Boolean {
        try {
            val response = client.patch("$apiBaseUrl/user/update/$uid") {
                contentType(ContentType.Application.Json)
                setBody(updates)
                val idToken = auth.getIdToken()
                headers {
                    append("Authorization", "Bearer $idToken")
                }
            }
            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error updating user data: ${e.message}")
            return false
        }
    }

    override suspend fun deleteUser(uid: String): Boolean {
        try {
            val idToken = auth.getIdToken()
            val response = client.delete("$apiBaseUrl/user/delete/$uid") {
                headers {
                    append("Authorization", "Bearer $idToken")
                }
            }
            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error deleting user: ${e.message}")
            return false
        }
    }
}

actual fun createFirebaseDatabase(): FirebaseDatabaseInterface {
    val auth = createFirebaseAuth()
    return FirebaseDatabaseDesktop(auth)
}