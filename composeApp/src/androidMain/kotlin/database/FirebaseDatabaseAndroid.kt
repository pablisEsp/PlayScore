package database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import data.model.TeamMembership
import data.model.TeamRole
import data.model.User
import data.model.UserRole
import data.model.UserStats
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
        if (uid.isNullOrEmpty()) {
            Log.e("FirebaseDatabase", "getUserData called with null or empty UID")
            continuation.resume(null)
            return@suspendCoroutine
        }

        Log.d("FirebaseDatabase", "Attempting to get user data for UID: $uid")
        usersRef.child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    try {
                        // Manual mapping from snapshot to User object
                        val id = snapshot.child("id").getValue(String::class.java) ?: uid
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""

                        // Parse global role
                        val roleStr = snapshot.child("globalRole").getValue(String::class.java) ?: "USER"
                        val globalRole = try {
                            UserRole.valueOf(roleStr)
                        } catch (e: Exception) {
                            Log.w("FirebaseDatabase", "Invalid role value: $roleStr, defaulting to USER")
                            UserRole.USER
                        }

                        val profileImage = snapshot.child("profileImage").getValue(String::class.java) ?: ""
                        val createdAt = snapshot.child("createdAt").getValue(String::class.java) ?: ""

                        // Parse team membership if it exists
                        val teamMembershipSnapshot = snapshot.child("teamMembership")
                        val teamMembership = if (teamMembershipSnapshot.exists()) {
                            val teamId = teamMembershipSnapshot.child("teamId").getValue(String::class.java)
                            val teamRoleStr = teamMembershipSnapshot.child("role").getValue(String::class.java)
                            val teamRole = if (teamRoleStr != null) {
                                try {
                                    TeamRole.valueOf(teamRoleStr)
                                } catch (e: Exception) {
                                    Log.w("FirebaseDatabase", "Invalid team role: $teamRoleStr")
                                    null
                                }
                            } else null

                            if (teamId != null) {
                                TeamMembership(teamId, teamRole)
                            } else null
                        } else null

                        // Parse user stats
                        val statsSnapshot = snapshot.child("stats")
                        val userStats = if (statsSnapshot.exists()) {
                            val matchesPlayed = statsSnapshot.child("matchesPlayed").getValue(Int::class.java) ?: 0
                            val goals = statsSnapshot.child("goals").getValue(Int::class.java) ?: 0
                            val assists = statsSnapshot.child("assists").getValue(Int::class.java) ?: 0
                            val mvps = statsSnapshot.child("mvps").getValue(Int::class.java) ?: 0
                            val avgRating = statsSnapshot.child("averageRating").getValue(Double::class.java) ?: 0.0

                            UserStats(
                                matchesPlayed = matchesPlayed,
                                goals = goals,
                                assists = assists,
                                mvps = mvps,
                                averageRating = avgRating
                            )
                        } else {
                            UserStats()
                        }

                        // Create the complete user object
                        val user = User(
                            id = id,
                            name = name,
                            email = email,
                            globalRole = globalRole,
                            teamMembership = teamMembership,
                            profileImage = profileImage,
                            stats = userStats,
                            createdAt = createdAt
                        )

                        Log.d("FirebaseDatabase", "Successfully mapped user data: $user")
                        continuation.resume(user)
                    } catch (e: Exception) {
                        Log.e("FirebaseDatabase", "Error mapping user data: ${e.message}", e)
                        continuation.resume(null)
                    }
                } else {
                    Log.w("FirebaseDatabase", "User data not found for UID: $uid. Creating new user.")

                    // Keep your existing fallback code for when user doesn't exist in database
                    val auth = FirebaseAuth.getInstance()
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val newUser = User(
                            id = uid,
                            name = firebaseUser.displayName ?: "User",
                            email = firebaseUser.email ?: "",
                            createdAt = Date().time.toString()
                        )

                        // Save this minimal user to the database
                        usersRef.child(uid).setValue(newUser)
                            .addOnSuccessListener {
                                Log.d("FirebaseDatabase", "Created new user in database: $newUser")
                                continuation.resume(newUser)
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseDatabase", "Failed to create new user: ${e.message}")
                                continuation.resume(newUser) // Still return the user even if saving failed
                            }
                    } else {
                        Log.e("FirebaseDatabase", "No Firebase Auth user found")
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