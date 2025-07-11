package firebase.auth

import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth as AndroidFirebaseAuth

class FirebaseAuthAndroid : FirebaseAuthInterface {
    private val firebaseAuth = AndroidFirebaseAuth.getInstance()

    override suspend fun createUser(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                AuthResult(
                    success = true,
                    userId = it.uid
                )
            } ?: AuthResult(success = false, errorMessage = "User creation failed")
        } catch (e: Exception) {
            AuthResult(success = false, errorMessage = e.message)
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                AuthResult(
                    success = true,
                    userId = it.uid
                )
            } ?: AuthResult(success = false, errorMessage = "Sign in failed")
        } catch (e: Exception) {
            AuthResult(success = false, errorMessage = e.message)
        }
    }

    override suspend fun updateUserProfile(displayName: String) {
        val user = firebaseAuth.currentUser ?: return
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates).await()
    }

    override fun getCurrentUser(): UserInfo? {
        return firebaseAuth.currentUser?.let { user ->
            UserInfo(
                uid = user.uid,
                displayName = user.displayName.toString(),
                email = user.email.toString()
            )
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getIdToken(): String {
        val firebaseUser = firebaseAuth.currentUser ?: return ""
        return try {
            firebaseUser.getIdToken(false).await().token ?: run {
                println("Warning: No token retrieved for user")
                ""
            }
        } catch (e: Exception) {
            println("Error retrieving ID token: ${e.message}")
            ""
        }
    }

    override suspend fun sendEmailVerification(): Boolean {
        return try {
            val user = firebaseAuth.currentUser ?: return false
            user.sendEmailVerification().await()
            true
        } catch (e: Exception) {
            println("Error sending verification email: ${e.message}")
            false
        }
    }

    override suspend fun isEmailVerified(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return user.isEmailVerified
    }

    override suspend fun reloadUser(): Boolean {
        return try {
            val user = firebaseAuth.currentUser ?: return false
            user.reload().await()
            true
        } catch (e: Exception) {
            println("Error reloading user: ${e.message}")
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            println("Error sending password reset email: ${e.message}")
            false
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Boolean {
        val user = firebaseAuth.currentUser ?: return false

        // Re-authenticate user first
        try {
            val email = user.email ?: return false
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)

            // Re-authenticate
            user.reauthenticate(credential).await()

            // Then update password
            user.updatePassword(newPassword).await()
            return true
        } catch (e: Exception) {
            println("Error updating password: ${e.message}")
            return false
        }
    }
}

actual fun createFirebaseAuth(): FirebaseAuthInterface {
    return FirebaseAuthAndroid()
}