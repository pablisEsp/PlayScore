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

class AuthService {

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
        val response: HttpResponse = client.post("http://10.0.2.2:3000/api/user/login") {
            contentType(ContentType.Application.Json)
            setBody(credentials)
        }

        return response.body()
    }

    // Register request
    suspend fun register(credentials: RegisterRequest): String {
        val response: HttpResponse = client.post("http://10.0.2.2:3000/api/user/register") {
            contentType(ContentType.Application.Json)
            setBody(credentials)
        }

        return response.bodyAsText() // Just returns the backend's message
    }
}
