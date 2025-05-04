package firebase.auth

interface FirebaseAuthInterface {
    suspend fun createUser(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun updateUserProfile(displayName: String)
    fun getCurrentUser(): UserInfo?
    fun signOut()
    suspend fun getIdToken(): String
}
