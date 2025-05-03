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

    private val apiBaseUrl = EnvironmentConfig.getEnv("DATABASE_API_URL") ?: "http://localhost:3000/api/database"

    override suspend fun saveUserData(userData: User): Boolean {
        try {
            val response = client.post("$apiBaseUrl/user") {
                contentType(ContentType.Application.Json)
                setBody(userData)
                headers {
                    append("Authorization", "Bearer ${auth.getIdToken()}")
                }
            }
            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error saving user data: ${e.message}")
            return false
        }
    }

    override suspend fun getUserData(uid: String?): User? {
        val userId = uid ?: auth.getCurrentUser()?.uid ?: return null

        try {
            val response = client.get("$apiBaseUrl/user/$userId") {
                headers {
                    append("Authorization", "Bearer ${auth.getIdToken()}")
                }
            }

            if (response.status.isSuccess()) {
                return response.body<User>()
            }
            return null
        } catch (e: Exception) {
            println("Error getting user data: ${e.message}")
            return null
        }
    }

    override suspend fun updateUserData(uid: String, updates: Map<String, Any>): Boolean {
        try {
            val response = client.patch("$apiBaseUrl/user/$uid") {
                contentType(ContentType.Application.Json)
                setBody(updates)
                headers {
                    append("Authorization", "Bearer ${auth.getIdToken()}")
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
            val response = client.delete("$apiBaseUrl/user/$uid") {
                headers {
                    append("Authorization", "Bearer ${auth.getIdToken()}")
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