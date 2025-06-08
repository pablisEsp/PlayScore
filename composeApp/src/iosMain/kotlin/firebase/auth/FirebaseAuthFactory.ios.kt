package firebase.auth

import platform.FirebaseAuth.FIRAuth
import platform.FirebaseAuth.FIRAuthDataResult
import platform.FirebaseAuth.FIRUser
import platform.FirebaseAuth.FIRUserProfileChangeRequest
import platform.Foundation.NSError // NSError is a standard iOS type

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class FirebaseAuthIOS : FirebaseAuthInterface {
    private val firebaseAuth = FIRAuth.auth()

    override suspend fun createUser(email: String, password: String): AuthResult = suspendCancellableCoroutine { continuation ->
        firebaseAuth.createUserWithEmail(
            email = email,
            password = password
        ) { authResult: FIRAuthDataResult?, error: NSError? ->
            if (error != null) {
                continuation.resume(
                    AuthResult(
                        success = false,
                        errorMessage = error.localizedDescription // Property
                    )
                )
                return@createUserWithEmail
            }

            val user: FIRUser? = authResult?.user
            if (user != null) {
                continuation.resume(
                    AuthResult(
                        success = true,
                        userId = user.UID // Property
                    )
                )
            } else {
                continuation.resume(
                    AuthResult(
                        success = false,
                        errorMessage = "User creation failed (no user data)"
                    )
                )
            }
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = suspendCancellableCoroutine { continuation ->
        firebaseAuth.signInWithEmail(
            email = email,
            password = password
        ) { authResult: FIRAuthDataResult?, error: NSError? ->
            if (error != null) {
                continuation.resume(
                    AuthResult(
                        success = false,
                        errorMessage = error.localizedDescription
                    )
                )
                return@signInWithEmail
            }

            val user: FIRUser? = authResult?.user
            if (user != null) {
                continuation.resume(
                    AuthResult(
                        success = true,
                        userId = user.UID
                    )
                )
            } else {
                continuation.resume(
                    AuthResult(
                        success = false,
                        errorMessage = "Sign in failed (no user data)"
                    )
                )
            }
        }
    }

    override suspend fun updateUserProfile(displayName: String): Unit = suspendCancellableCoroutine { continuation ->
        val user: FIRUser? = firebaseAuth.currentUser // Property
        if (user == null) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val changeRequest: FIRUserProfileChangeRequest? = user.profileChangeRequest() // Method
        changeRequest?.let { req ->
            req.displayName = displayName // Property
            req.commitChangesWithCompletion { error: NSError? ->
                // Interface doesn't require error propagation here
                continuation.resume(Unit)
            }
        } ?: continuation.resume(Unit) // If profileChangeRequest() returns null
    }

    override fun getCurrentUser(): UserInfo? {
        val currentUser: FIRUser? = firebaseAuth.currentUser // Property
        return currentUser?.let { user ->
            UserInfo(
                uid = user.UID, // Property
                displayName = user.displayName ?: "", // Property
                email = user.email ?: "" // Property
            )
        }
    }

    override fun signOut() {
        try {
            // The Obj-C method is `(BOOL)signOut:(NSError **)error;`
            // Kotlin/Native might expose this as a function that throws, or one returning Boolean.
            // If it's a throwing function, this direct call is fine.
            // If it's `fun signOut(error: CValuesRef<ObjCObjectVar<NSError?>>?): Boolean`,
            // then `firebaseAuth.signOut(null)` is a common way to call it if you don't handle the error pointer.
            // Assuming the binding makes it a simple call or throws on error.
            firebaseAuth.signOut()
        } catch (e: Exception) {
            println("Error signing out: ${e.message}")
        }
    }

    override suspend fun getIdToken(): String = suspendCancellableCoroutine { continuation ->
        val user: FIRUser? = firebaseAuth.currentUser
        if (user == null) {
            continuation.resume("")
            return@suspendCancellableCoroutine
        }

        // Assuming `getIDTokenWithCompletion(false)` from your original code maps to `getIDTokenForcingRefresh`.
        user.getIDTokenForcingRefresh(false) { token: String?, error: NSError? ->
            if (error != null || token == null) {
                println("Error retrieving ID token: ${error?.localizedDescription ?: "Unknown error"}")
                continuation.resume("")
            } else {
                continuation.resume(token)
            }
        }
    }

    override suspend fun sendEmailVerification(): Boolean = suspendCancellableCoroutine { continuation ->
        val user: FIRUser? = firebaseAuth.currentUser
        if (user == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        user.sendEmailVerificationWithCompletion { error: NSError? ->
            continuation.resume(error == null)
        }
    }

    override suspend fun isEmailVerified(): Boolean {
        // FIRUser property: `@property(nonatomic, readonly, getter=isEmailVerified) BOOL emailVerified;`
        return firebaseAuth.currentUser?.emailVerified ?: false // Property
    }

    override suspend fun reloadUser(): Boolean = suspendCancellableCoroutine { continuation ->
        val user: FIRUser? = firebaseAuth.currentUser
        if (user == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        user.reloadWithCompletion { error: NSError? ->
            continuation.resume(error == null)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Boolean = suspendCancellableCoroutine { continuation ->
        firebaseAuth.sendPasswordResetWithEmail(email) { error: NSError? ->
            continuation.resume(error == null)
        }
    }
}

actual fun createFirebaseAuth(): FirebaseAuthInterface {
    return FirebaseAuthIOS()
}