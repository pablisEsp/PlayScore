package viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import auth.FirebaseAuthInterface
import auth.createFirebaseAuth
import data.model.User
import data.model.UserRole
import data.model.UserStats
import kotlinx.datetime.Clock


class HomeViewModel(
    private val auth: FirebaseAuthInterface = createFirebaseAuth()
) : ViewModel() {
    var currentUser by mutableStateOf<User?>(null)
        private set

    var isLoggedIn by mutableStateOf(true)
        private set

    init {
        // Get current user from Firebase Auth
        auth.getCurrentUser()?.let { firebaseUser ->
            currentUser = User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "User",
                email = firebaseUser.email ?: "",
                globalRole = UserRole.USER,
                teamMembership = null,
                profileImage = "", // You could get this from Firebase user photoUrl if needed
                stats = UserStats(),
                createdAt = Clock.System.now().toString() // ISO-8601 format
            )
        } ?: run {
            isLoggedIn = false
        }
    }

    fun logout() {
        auth.signOut()
        isLoggedIn = false
        currentUser = null
    }
}
