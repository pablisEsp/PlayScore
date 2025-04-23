package auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthAndroid : FirebaseAuthInterface {
    private val auth = FirebaseAuth.getInstance()

    override suspend fun createUser(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            AuthResult(
                success = true,
                userId = result.user?.uid
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                errorMessage = e.message ?: "Unknown error occurred"
            )
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            AuthResult(
                success = true,
                userId = result.user?.uid
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                errorMessage = e.message ?: "Unknown error occurred"
            )
        }
    }

    override suspend fun updateUserProfile(displayName: String) {
        val user = auth.currentUser ?: return
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates).await()
    }

    override fun getCurrentUser(): UserInfo? {
        return auth.currentUser?.let {
            UserInfo(
                uid = it.uid,
                displayName = it.displayName,
                email = it.email
            )
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}
