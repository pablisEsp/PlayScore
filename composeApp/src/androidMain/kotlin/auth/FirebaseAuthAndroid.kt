package firebase.auth

import auth.AuthResult
import auth.FirebaseAuthInterface
import auth.UserInfo
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
                displayName = user.displayName,
                email = user.email
            )
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }


    override fun getIdToken(): String {
        val firebaseUser = firebaseAuth.currentUser
        return firebaseUser?.getIdToken(false)?.result?.token ?: ""
    }
}
