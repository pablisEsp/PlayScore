package auth

actual fun createFirebaseAuth(): FirebaseAuthInterface = object : FirebaseAuthInterface {
    override suspend fun createUser(email: String, password: String): AuthResult {
        return AuthResult(success = false, errorMessage = "Not implemented for Desktop yet")
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return AuthResult(success = false, errorMessage = "Not implemented for Desktop yet")
    }

    override suspend fun updateUserProfile(displayName: String) {
        // Not implemented
    }

    override fun getCurrentUser(): UserInfo? = null

    override fun signOut() {
        // Not implemented
    }
}
