package firebase.database

import data.model.Like
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.auth.createFirebaseAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import utils.EnvironmentConfig

class FirebaseDatabaseDesktop(private val auth: FirebaseAuthInterface) : FirebaseDatabaseInterface {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // Logging block
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    // helper method
    private fun logRequestDetails(methodName: String, path: String) {
        println("⭐⭐⭐ $methodName called for path: $path ⭐⭐⭐")
        println("Current user: ${auth.getCurrentUser()}")
    }

    private val apiBaseUrl = "http://localhost:3000/api"

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
        } else {
            //println("Full token for testing: $token")
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


    override suspend fun updateUserData(uid: String, updates: Map<String, Any?>): Boolean {
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

    override suspend fun updateUsername(userId: String, username: String): Boolean {
        try {
            val token = ensureValidToken()
            val response = client.patch("$apiBaseUrl/user/username/$userId") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("username" to username))
                headers {
                    append("Authorization", "Bearer $token")
                }
            }
            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error updating username: ${e.message}")
            return false
        }
    }

    override suspend fun checkUsernameAvailable(username: String): Boolean {
        try {
            val token = ensureValidToken()
            val response = client.get("$apiBaseUrl/user/check-username/$username") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            if (response.status.isSuccess()) {
                return response.body<Boolean>()
            }
            return false
        } catch (e: Exception) {
            println("Error checking username availability: ${e.message}")
            return false
        }
    }


    // Then implement in your desktop class
    override suspend fun <T> getCollection(path: String, serializer: KSerializer<List<T>>): List<T> {
        try {
            logRequestDetails("getCollection", path)
            val token = ensureValidToken()

            val fullUrl = "$apiBaseUrl/user/db/collection/$path"
            println("Full API URL: $fullUrl")

            val response = client.get(fullUrl) {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            println("getCollection response status: ${response.status}")

            return if (response.status.isSuccess()) {
                // Use the provided serializer to deserialize the response
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString(serializer, response.body())
            } else {
                println("Failed to get collection: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            println("Error getting collection: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    override suspend fun <T> getCollectionFiltered(
        path: String,
        field: String,
        value: Any?,
        serializer: KSerializer<List<T>>
    ): List<T> {
        try {
            logRequestDetails("getCollectionFiltered", "path=$path, field=$field, value=$value")
            val token = ensureValidToken()

            // Convert value to string for the server
            val stringValue = when (value) {
                is String -> value
                is Number, is Boolean -> value.toString()
                null -> "null"
                else -> value.toString()
            }

            println("Making filtered collection request to: $apiBaseUrl/user/db/collection/$path/filter")
            println("Parameters: field=$field, value=$stringValue")

            val response = client.get("$apiBaseUrl/user/db/collection/$path/filter") {
                headers {
                    append("Authorization", "Bearer $token")
                }
                parameter("field", field)
                parameter("value", stringValue)
            }

            println("getCollectionFiltered response status: ${response.status}")

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Response body: $responseText")

                // Create a Json instance and use it with the provided serializer
                val json = Json { ignoreUnknownKeys = true }
                return json.decodeFromString(serializer, responseText)
            } else {
                val errorBody = response.bodyAsText()
                println("Error response: ${response.status} - $errorBody")
                return emptyList()
            }
        } catch (e: Exception) {
            println("Error getting filtered collection: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    override suspend fun <T> getDocument(path: String): T? {
        try {
            val token = ensureValidToken()
            val response = client.get("$apiBaseUrl/db/document/$path") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return if (response.status.isSuccess()) {
                @Suppress("UNCHECKED_CAST")
                response.body<Any>() as T?
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error getting document: ${e.message}")
            return null
        }
    }

    override suspend fun <T> createDocument(path: String, data: T): String {
        try {
            val token = ensureValidToken()
            val response = client.post("$apiBaseUrl/db/document/$path") {
                contentType(ContentType.Application.Json)
                @Suppress("UNCHECKED_CAST")
                setBody(data as Any)
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return if (response.status.isSuccess()) {
                response.body<Map<String, String>>()["id"] ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            println("Error creating document: ${e.message}")
            return ""
        }
    }

    override suspend fun <T> updateDocument(path: String, id: String, data: T): Boolean {
        try {
            val token = ensureValidToken()
            val response = client.put("$apiBaseUrl/db/document/$path/$id") {
                contentType(ContentType.Application.Json)
                @Suppress("UNCHECKED_CAST")
                setBody(data as Any)
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error updating document: ${e.message}")
            return false
        }
    }

    override suspend fun updateFields(collectionPath: String, documentId: String, fields: Map<String, Any?>): Boolean {
        try {
            val token = ensureValidToken()
            val response = client.patch("$apiBaseUrl/db/document/$collectionPath/$documentId") {
                contentType(ContentType.Application.Json)
                setBody(fields)
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error updating fields: ${e.message}")
            return false
        }
    }

    override suspend fun deleteDocument(path: String, id: String): Boolean {
        try {
            val token = ensureValidToken()
            val response = client.delete("$apiBaseUrl/db/document/$path/$id") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error deleting document: ${e.message}")
            return false
        }
    }

    // Like-related methods
    override suspend fun createLike(like: Like): String {
        try {
            val token = ensureValidToken()
            val response = client.post("$apiBaseUrl/likes/create") {
                contentType(ContentType.Application.Json)
                setBody(like)
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return if (response.status.isSuccess()) {
                like.postId
            } else {
                ""
            }
        } catch (e: Exception) {
            println("Error creating like: ${e.message}")
            return ""
        }
    }

    override suspend fun deleteLike(userId: String, postId: String): Boolean {
        try {
            val token = ensureValidToken()
            val response = client.delete("$apiBaseUrl/likes/delete") {
                parameter("userId", userId)
                parameter("postId", postId)
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return response.status.isSuccess()
        } catch (e: Exception) {
            println("Error deleting like: ${e.message}")
            return false
        }
    }

    override suspend fun isPostLikedByUser(userId: String, postId: String): Boolean {
        try {
            val token = ensureValidToken()
            val response = client.get("$apiBaseUrl/likes/check") {
                parameter("userId", userId)
                parameter("postId", postId)
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return if (response.status.isSuccess()) {
                response.body<Boolean>()
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error checking if post is liked: ${e.message}")
            return false
        }
    }

    override suspend fun getLikesForPost(postId: String): List<Like> {
        try {
            val token = ensureValidToken()
            val response = client.get("$apiBaseUrl/likes/post/$postId") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return if (response.status.isSuccess()) {
                response.body()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Error getting likes for post: ${e.message}")
            return emptyList()
        }
    }

    override suspend fun getPostsLikedByUser(userId: String): List<String> {
        try {
            val token = ensureValidToken()
            val response = client.get("$apiBaseUrl/likes/user/$userId") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }

            return if (response.status.isSuccess()) {
                response.body()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Error getting posts liked by user: ${e.message}")
            return emptyList()
        }
    }

}

actual fun createFirebaseDatabase(): FirebaseDatabaseInterface {
    val auth = createFirebaseAuth()
    return FirebaseDatabaseDesktop(auth)
}