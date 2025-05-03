package firebase.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResult(
    val success: Boolean,
    val userId: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class UserInfo(
    val uid: String,
    val email: String,
    val displayName: String = ""
)