package auth

actual fun createFirebaseAuth(): FirebaseAuthInterface = object : FirebaseAuthInterface {
    override suspend fun createUser(email: String, password: String): AuthResult {
        println("Desktop auth: createUser with email: $email (not implemented)")
        return AuthResult(success = false, errorMessage = "Not implemented for Desktop yet")
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        println("Desktop auth: signIn with email: $email")

        // For desktop testing, accept any credentials with "@test.com"
        if (email.endsWith("@test.com")) {
            return AuthResult(
                success = true,
                userId = "test-user-id",
                errorMessage = null
            )
        }

        return AuthResult(success = false, errorMessage = "Desktop login only works with @test.com emails for testing")
    }

    override suspend fun updateUserProfile(displayName: String) {
        println("Desktop auth: updateUserProfile to: $displayName (not implemented)")
    }

    override fun getCurrentUser(): UserInfo? {
        println("Desktop auth: getCurrentUser (returning test user)")
        // Return a test user for desktop development
        return UserInfo(uid = "test-user-id", email = "test@test.com", displayName = "Test User")
    }

    override fun signOut() {
        println("Desktop auth: signOut (not fully implemented)")
    }

    override fun getIdToken(): String {
        println("Desktop auth: getIdToken (returning test token)")
        return "desktop-test-token"
    }
}