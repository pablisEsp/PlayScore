package auth

data class AuthResult(
    val success: Boolean,
    val userId: String? = null,
    val errorMessage: String? = null
)

data class UserInfo(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null
)

interface FirebaseAuthInterface {
    suspend fun createUser(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun updateUserProfile(displayName: String)
    fun getCurrentUser(): UserInfo?
    fun signOut()
    fun getIdToken(): String
}