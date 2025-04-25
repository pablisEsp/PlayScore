package database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import data.model.User
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseDatabaseAndroid : FirebaseDatabaseInterface {
    private val database = FirebaseDatabase.getInstance("https://playscore-88a05-default-rtdb.europe-west1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    override suspend fun saveUserData(userData: User): Boolean = suspendCoroutine { continuation ->
        usersRef.child(userData.id).setValue(userData)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                // It's good practice to log the error!
                Log.e("FirebaseDatabaseAndroid", "Failed to save user data", e)
                continuation.resume(false)
            }
    }

    override suspend fun getUserData(uid: String?): User? = suspendCoroutine { continuation ->
        usersRef.child(uid ?: "").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    try {
                        val user = snapshot.getValue(User::class.java)
                        continuation.resume(user)
                    } catch (e: Exception) {
                        Log.e("FirebaseDatabase", "Error deserializing user: ${e.message}")
                        continuation.resume(null)
                    }
                } else {
                    Log.e("FirebaseDatabase", "User document not found for ID: $uid")
                    // If no user data exists, create a minimal user profile
                    val auth = FirebaseAuth.getInstance()
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val newUser = User(
                            id = uid ?: "",
                            name = firebaseUser.displayName ?: "User",
                            email = firebaseUser.email ?: "",
                            createdAt = Date().time.toString()
                        )
                        // Save this minimal user to the database
                        usersRef.child(uid?: "").setValue(newUser)
                            .addOnSuccessListener {
                                continuation.resume(newUser)
                            }
                            .addOnFailureListener {
                                continuation.resume(newUser)
                            }
                    } else {
                        continuation.resume(null)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to get user data: ${e.message}")
                continuation.resume(null)
            }
    }


    override suspend fun updateUserData(uid: String, updates: Map<String, Any>): Boolean = suspendCoroutine { continuation ->
        usersRef.child(uid).updateChildren(updates)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    override suspend fun deleteUser(uid: String): Boolean = suspendCoroutine { continuation ->
        usersRef.child(uid).removeValue()
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }
}