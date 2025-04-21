package data.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    val user: User,
    val expiresIn: Long = 3600 // Default expiration time in seconds (1 hour)
)