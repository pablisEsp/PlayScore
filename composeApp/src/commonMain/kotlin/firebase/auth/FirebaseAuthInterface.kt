package firebase.auth

interface FirebaseAuthInterface {
    fun getCurrentUser(): UserInfo?
    fun signOut()
    suspend fun getIdToken(): String
    suspend fun createUser(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun updateUserProfile(displayName: String)
    suspend fun sendEmailVerification(): Boolean
    suspend fun isEmailVerified(): Boolean
    suspend fun reloadUser(): Boolean
}
