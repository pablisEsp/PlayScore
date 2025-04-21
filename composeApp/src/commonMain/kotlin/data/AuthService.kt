package data

import data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class AuthService(private val tokenManager: TokenManager) {

    // Base URL for API requests
    private val baseUrl = "http://10.0.2.2:3000/api"

    // Ktor HTTP client with JSON serialization support
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true               
                isLenient = true                 
                ignoreUnknownKeys = true         
            })
        }
    }

    // Login request
    suspend fun login(credentials: LoginRequest): AuthResponse {
        try {
            // Get the raw response
            val httpResponse = client.post("$baseUrl/user/login") {
                contentType(ContentType.Application.Json)
                setBody(credentials)
            }
            
            // Get the response body as text
            val responseText = httpResponse.bodyAsText()
            
            // Check if the response is successful
            if (httpResponse.status.isSuccess()) {
                try {
                    // Parse the response
                    val jsonElement = Json.parseToJsonElement(responseText)
                    
                    // Extract the token - this is the only field your server returns
                    val token = jsonElement.jsonObject["token"]?.jsonPrimitive?.content
                        ?: throw Exception("Token missing in response")
                    
                    // Since your server doesn't return user info, we need to decode the JWT token
                    // JWT tokens are in the format: header.payload.signature
                    val userData = extractUserDataFromToken(token)
                    
                    // Create a User object with data from token
                    val user = User(
                        id = userData["id"]?.jsonPrimitive?.content ?: "",
                        name = userData["name"]?.jsonPrimitive?.content ?: "",
                        email = userData["email"]?.jsonPrimitive?.content ?: "",
                        globalRole = parseUserRole(userData["globalRole"]?.jsonPrimitive?.content),
                        teamMembership = parseTeamMembership(userData["teamMembership"]),
                        profileImage = "", // Not in token
                        stats = UserStats(), // Not in token
                        createdAt = "" // Not in token
                    )
                    
                    // Create the response object with default expiration (1 hour)
                    val authResponse = AuthResponse(token, user, 3600L)
                    
                    // Save auth data
                    tokenManager.saveAuthData(authResponse.token, authResponse.user, authResponse.expiresIn)
                    
                    return authResponse
                } catch (e: Exception) {
                    // Check if it's a success message without proper data structure
                    val message = extractMessageFromResponse(responseText)
                    if (message != null && message.contains("success", ignoreCase = true)) {
                        throw Exception("Login successful but response format is incorrect: $responseText")
                    }
                    throw Exception("Failed to parse login response: ${e.message}")
                }
            } else {
                // Handle error response
                val message = extractMessageFromResponse(responseText)
                if (message != null) {
                    throw Exception(message)
                }
                throw Exception("Login failed: $responseText")
            }
        } catch (e: Exception) {
            throw Exception("Login failed: ${e.message}")
        }
    }

    // Register request
    suspend fun register(credentials: RegisterRequest): AuthResponse {
        try {
            // First get the raw response to handle potential errors
            val httpResponse = client.post("$baseUrl/user/register") {
                contentType(ContentType.Application.Json)
                setBody(credentials)
            }
            
            // Get the response body as text
            val responseText = httpResponse.bodyAsText()
            
            // Check if the response is successful
            if (httpResponse.status.isSuccess()) {
                try {
                    // Your server returns { message: "User registered successfully." }
                    // So we need to create a minimal user object and redirect to login
                    
                    // Create a minimal user object
                    val user = User(
                        id = "",
                        name = credentials.name,
                        email = credentials.email,
                        globalRole = UserRole.USER,
                        teamMembership = null,
                        profileImage = "",
                        stats = UserStats(),
                        createdAt = ""
                    )
                    
                    // Return a response with empty token (user will need to login)
                    return AuthResponse("", user, 0)
                    
                } catch (e: Exception) {
                    // Check if it's a success message
                    val message = extractMessageFromResponse(responseText)
                    if (message != null && message.contains("success", ignoreCase = true)) {
                        // Create a minimal response with default values
                        val user = User(
                            id = "",
                            name = credentials.name,
                            email = credentials.email,
                            globalRole = UserRole.USER,
                            teamMembership = null,
                            profileImage = "",
                            stats = UserStats(),
                            createdAt = ""
                        )
                        
                        return AuthResponse("", user, 0) // Empty token since we'll need to login
                    }
                    
                    throw Exception("Failed to parse registration response: ${e.message}")
                }
            } else {
                // Handle error response
                val message = extractMessageFromResponse(responseText)
                
                // Check for email already in use error
                if (message != null && (message.contains("email", ignoreCase = true) && 
                    (message.contains("already", ignoreCase = true) || message.contains("exists", ignoreCase = true) || 
                     message.contains("in use", ignoreCase = true) || message.contains("duplicate", ignoreCase = true)))) {
                    throw Exception("Email already in use. Please use a different email address.")
                }
                
                if (message != null) {
                    throw Exception(message)
                }
                
                throw Exception("Registration failed: $responseText")
            }
        } catch (e: Exception) {
            // Check for specific error messages
            val errorMsg = e.message ?: ""
            if (errorMsg.contains("email", ignoreCase = true) && 
                (errorMsg.contains("already", ignoreCase = true) || errorMsg.contains("exists", ignoreCase = true) || 
                 errorMsg.contains("in use", ignoreCase = true) || errorMsg.contains("duplicate", ignoreCase = true))) {
                throw Exception("Email already in use. Please use a different email address.")
            }
            
            throw Exception("Registration failed: ${e.message}")
        }
    }
    
    // Helper function to extract user data from JWT token
    @OptIn(ExperimentalEncodingApi::class)
    private fun extractUserDataFromToken(token: String): JsonObject {
        try {
            // JWT tokens are in the format: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) {
                return JsonObject(emptyMap())
            }
            
            // Decode the payload (second part)
            val payload = parts[1]
            
            // JWT uses base64url encoding (with - and _ instead of + and /)
            val base64 = payload.replace("-", "+").replace("_", "/")
            
            // Add padding if needed
            val paddedBase64 = when (base64.length % 4) {
                0 -> base64
                2 -> "$base64=="
                3 -> "$base64="
                else -> base64
            }
            
            // Decode the base64 string
            val decodedBytes = Base64.decode(paddedBase64)
            val decodedString = decodedBytes.decodeToString()
            
            // Parse the JSON
            return Json.parseToJsonElement(decodedString).jsonObject
        } catch (e: Exception) {
            // If we can't decode the token, return an empty object
            return JsonObject(emptyMap())
        }
    }
    
    // Helper function to extract message from response
    private fun extractMessageFromResponse(responseText: String): String? {
        return try {
            val jsonElement = Json.parseToJsonElement(responseText)
            
            // Try different common message fields
            jsonElement.jsonObject["message"]?.jsonPrimitive?.content
                ?: jsonElement.jsonObject["msg"]?.jsonPrimitive?.content
                ?: jsonElement.jsonObject["error"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
    
    // Helper function to parse UserRole
    private fun parseUserRole(roleStr: String?): UserRole {
        return when (roleStr?.uppercase()) {
            "ADMIN" -> UserRole.ADMIN
            "SUPER_ADMIN" -> UserRole.SUPER_ADMIN
            else -> UserRole.USER
        }
    }
    
    // Helper function to parse TeamMembership
    private fun parseTeamMembership(teamMembershipJson: JsonElement?): TeamMembership? {
        if (teamMembershipJson == null || teamMembershipJson is JsonNull) return null
        
        try {
            val teamMembershipObj = teamMembershipJson.jsonObject
            val teamId = teamMembershipObj["teamId"]?.jsonPrimitive?.content
            val roleStr = teamMembershipObj["role"]?.jsonPrimitive?.content
            
            val role = when (roleStr?.uppercase()) {
                "CAPTAIN" -> TeamRole.CAPTAIN
                "VICE_PRESIDENT" -> TeamRole.VICE_PRESIDENT
                "PRESIDENT" -> TeamRole.PRESIDENT
                else -> TeamRole.PLAYER
            }
            
            return TeamMembership(teamId, role)
        } catch (e: Exception) {
            return null
        }
    }
    
    // Helper function to parse UserStats
    private fun parseUserStats(statsJson: JsonElement?): UserStats {
        if (statsJson == null || statsJson is JsonNull) return UserStats()
        
        try {
            val statsObj = statsJson.jsonObject
            return UserStats(
                matchesPlayed = statsObj["matchesPlayed"]?.jsonPrimitive?.int ?: 0,
                goals = statsObj["goals"]?.jsonPrimitive?.int ?: 0,
                assists = statsObj["assists"]?.jsonPrimitive?.int ?: 0,
                mvps = statsObj["mvps"]?.jsonPrimitive?.int ?: 0,
                averageRating = statsObj["averageRating"]?.jsonPrimitive?.double ?: 0.0
            )
        } catch (e: Exception) {
            return UserStats()
        }
    }
    
    // Authenticated request helper
    private suspend inline fun <reified T> authenticatedRequest(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        if (!tokenManager.validateToken()) {
            throw UnauthorizedException("Your session has expired. Please log in again.")
        }
        
        return client.request("$baseUrl$path") {
            this.method = method
            
            // Add authorization header with the token
            tokenManager.getToken()?.let { token ->
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            
            // Apply additional request configurations
            block()
        }.body()
    }
    
    // Logout
    fun logout() {
        tokenManager.clearAuthData()
    }
    
    // Check if user is logged in
    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn
    
    // Get current user
    fun getCurrentUser(): User? = tokenManager.currentUser
    
    // Check if token is valid
    fun isTokenValid(): Boolean = tokenManager.validateToken()
}

// Custom exception for unauthorized requests
class UnauthorizedException(message: String) : Exception(message)
