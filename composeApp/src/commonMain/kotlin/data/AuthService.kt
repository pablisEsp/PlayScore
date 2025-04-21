package data

import data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AuthService(private val tokenManager: TokenManager) {

    // Base URL for API requests
    private val baseUrl = "http://10.0.2.2:3000/api"

    // Ktor HTTP client with JSON serialization support
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true               // For easier debugging
                isLenient = true                 // Accepts relaxed JSON formats
                ignoreUnknownKeys = true         // Ignores unexpected fields in response
            })
        }
    }

    // Login request
    suspend fun login(credentials: LoginRequest): AuthResponse {
        val response: AuthResponse = client.post("$baseUrl/user/login") {
            contentType(ContentType.Application.Json)
            setBody(credentials)
        }.body()
        
        // Save auth data
        tokenManager.saveAuthData(response.token, response.user)
        
        return response
    }

    // Register request
    suspend fun register(credentials: RegisterRequest): AuthResponse {
        val response: AuthResponse = client.post("$baseUrl/user/register") {
            contentType(ContentType.Application.Json)
            setBody(credentials)
        }.body()
        
        // Save auth data
        tokenManager.saveAuthData(response.token, response.user)
        
        return response
    }
    
    // Logout
    fun logout() {
        tokenManager.clearAuthData()
    }
    
    // Check if user is logged in
    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn
    
    // Get current user
    fun getCurrentUser(): User? = tokenManager.currentUser
}
