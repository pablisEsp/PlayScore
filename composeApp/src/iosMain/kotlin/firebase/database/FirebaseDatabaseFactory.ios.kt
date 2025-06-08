package firebase.database

import cocoapods.FirebaseDatabase.*
import data.model.*
import firebase.auth.createFirebaseAuth
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import platform.Foundation.*
import kotlin.coroutines.resume

class FirebaseDatabaseIOS : FirebaseDatabaseInterface {
    private val database: FIRDatabase
    private val usersRef: FIRDatabaseReference

    init {
        database = FIRDatabase.database(URL = NSURL.URLWithString("https://playscore-88a05-default-rtdb.europe-west1.firebasedatabase.app"))
        usersRef = database.referenceWithPath("users")
    }

    override suspend fun saveUserData(userData: User): Boolean = suspendCancellableCoroutine { continuation ->
        val userRef = usersRef.child(userData.id)

        // Create a dictionary representation of the User object
        val userDict = mapOf(
            "id" to userData.id,
            "name" to userData.name,
            "email" to userData.email,
            "username" to userData.username,
            "globalRole" to userData.globalRole.name,
            "profileImage" to userData.profileImage,
            "createdAt" to userData.createdAt
        )

        // If teamMembership is not null, include it
        val completeDict = if (userData.teamMembership != null) {
            val teamMembershipDict = mapOf(
                "teamId" to (userData.teamMembership.teamId ?: ""),
                "role" to (userData.teamMembership.role?.name ?: "")
            )
            userDict + ("teamMembership" to teamMembershipDict)
        } else {
            userDict
        }

        userRef.setValue(completeDict.toMap()) { error, _ ->
            if (error != null) {
                println("Error saving user data: ${error.localizedDescription}")
                continuation.resume(false)
            } else {
                continuation.resume(true)
            }
        }
    }

    override suspend fun getUserData(uid: String?): User? = suspendCancellableCoroutine { continuation ->
        if (uid.isNullOrEmpty()) {
            println("getUserData called with null or empty UID")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        usersRef.child(uid).observeSingleEventOfType(FIRDataEventTypeValue) { snapshot, error ->
            if (error != null) {
                println("Error fetching user data: ${error.localizedDescription}")
                continuation.resume(null)
                return@observeSingleEventOfType
            }

            if (snapshot == null || !snapshot.exists()) {
                // Handle new user creation similar to Android implementation
                val auth = createFirebaseAuth()
                val firebaseUser = auth.getCurrentUser()
                if (firebaseUser != null) {
                    val newUser = User(
                        id = uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        username = firebaseUser.email ?: "",
                        createdAt = NSDate().timeIntervalSince1970.toString()
                    )

                    // Save the new user to database
                    usersRef.child(uid).setValue(
                        mapOf(
                            "id" to newUser.id,
                            "name" to newUser.name,
                            "email" to newUser.email,
                            "username" to newUser.username,
                            "globalRole" to newUser.globalRole.name,
                            "createdAt" to newUser.createdAt
                        )
                    )

                    continuation.resume(newUser)
                } else {
                    continuation.resume(null)
                }
                return@observeSingleEventOfType
            }

            // Extract user data from snapshot
            try {
                val valueDict = snapshot.value as? Map<String, Any?>
                if (valueDict != null) {
                    val id = valueDict["id"] as? String ?: uid
                    val name = valueDict["name"] as? String ?: ""
                    val email = valueDict["email"] as? String ?: ""
                    val username = valueDict["username"] as? String ?: ""
                    val profileImage = valueDict["profileImage"] as? String ?: ""
                    val createdAt = valueDict["createdAt"] as? String ?: ""

                    // Parse global role
                    val roleStr = valueDict["globalRole"] as? String ?: "USER"
                    val globalRole = try {
                        UserRole.valueOf(roleStr)
                    } catch (e: Exception) {
                        println("Invalid role value: $roleStr, defaulting to USER")
                        UserRole.USER
                    }

                    // Extract team membership data if it exists
                    val teamMembershipDict = valueDict["teamMembership"] as? Map<String, Any?>
                    val teamMembership = if (teamMembershipDict != null) {
                        val teamId = teamMembershipDict["teamId"] as? String
                        val teamRoleStr = teamMembershipDict["role"] as? String
                        val teamRole = try {
                            if (teamRoleStr != null) TeamRole.valueOf(teamRoleStr) else null
                        } catch (e: Exception) {
                            println("Invalid team role value: $teamRoleStr")
                            null
                        }
                        TeamMembership(teamId = teamId, role = teamRole)
                    } else null

                    val user = User(
                        id = id,
                        name = name,
                        email = email,
                        username = username,
                        globalRole = globalRole,
                        teamMembership = teamMembership,
                        profileImage = profileImage,
                        createdAt = createdAt
                    )

                    continuation.resume(user)
                } else {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                println("Error parsing user data: ${e.message}")
                continuation.resume(null)
            }
        }
    }

    override suspend fun updateUserData(uid: String, updates: Map<String, Any?>): Boolean =
        suspendCancellableCoroutine { continuation ->
            usersRef.child(uid).updateChildValues(updates) { error, _ ->
                continuation.resume(error == null)
            }
        }

    override suspend fun updateUsername(userId: String, username: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            // First check if username is already taken
            database.referenceWithPath("usernames").child(username)
                .observeSingleEventOfType(FIRDataEventTypeValue) { snapshot, error ->
                    if (error != null) {
                        continuation.resume(false)
                        return@observeSingleEventOfType
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Username already taken
                        continuation.resume(false)
                    } else {
                        // Username is available, update in two places
                        val updates = mapOf(
                            "users/$userId/username" to username,
                            "usernames/$username" to userId
                        )

                        database.reference().updateChildValues(updates) { error, _ ->
                            continuation.resume(error == null)
                        }
                    }
                }
        }

    override suspend fun checkUsernameAvailable(username: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            if (username.isBlank()) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            database.referenceWithPath("usernames").child(username)
                .observeSingleEventOfType(FIRDataEventTypeValue) { snapshot, error ->
                    if (error != null) {
                        // During initial app usage, consider usernames available
                        continuation.resume(true)
                        return@observeSingleEventOfType
                    }

                    val isAvailable = snapshot == null || !snapshot.exists()
                    continuation.resume(isAvailable)
                }
        }

    override suspend fun deleteUser(uid: String): Boolean = suspendCancellableCoroutine { continuation ->
        usersRef.child(uid).removeValue { error, _ ->
            continuation.resume(error == null)
        }
    }

    // Implementation for generic collection methods
    override suspend fun <T> getCollection(path: String, serializer: KSerializer<List<T>>): List<T> =
        suspendCancellableCoroutine { continuation ->
            database.referenceWithPath(path)
                .observeSingleEventOfType(FIRDataEventTypeValue) { snapshot, error ->
                    if (error != null || snapshot == null) {
                        continuation.resume(emptyList())
                        return@observeSingleEventOfType
                    }

                    try {
                        val items = deserializeSnapshotToList<T>(snapshot, serializer.descriptor)
                        continuation.resume(items)
                    } catch (e: Exception) {
                        println("Error deserializing collection: ${e.message}")
                        continuation.resume(emptyList())
                    }
                }
        }

    // Other interface implementations...

    private fun <T> deserializeSnapshotToList(snapshot: FIRDataSnapshot, descriptor: SerialDescriptor): List<T> {
        val elementType = descriptor.getElementDescriptor(0).serialName
        val items = mutableListOf<T>()

        snapshot.children.forEach { childSnapshot ->
            val child = childSnapshot as? FIRDataSnapshot ?: return@forEach
            val key = child.key ?: return@forEach

            when {
                elementType.endsWith("data.model.Post") -> {
                    val valueDict = child.value as? Map<String, Any?>
                    if (valueDict != null) {
                        val post = Post(
                            id = key,
                            authorId = valueDict["authorId"] as? String ?: "",
                            authorName = valueDict["authorName"] as? String ?: "",
                            content = valueDict["content"] as? String ?: "",
                            mediaUrls = valueDict["mediaUrls"] as? List<String> ?: emptyList(),
                            likeCount = (valueDict["likeCount"] as? NSNumber)?.intValue ?: 0,
                            parentPostId = valueDict["parentPostId"] as? String,
                            createdAt = valueDict["createdAt"] as? String ?: ""
                        )
                        items.add(post as T)
                    }
                }
                // Handle other model types similarly...
                else -> println("Unsupported element type: $elementType")
            }
        }

        return items
    }

    // Add stubs for remaining methods
    override suspend fun <T> getCollectionFiltered(path: String, field: String, value: Any?, serializer: KSerializer<List<T>>): List<T> {
        // Implementation similar to getCollection but with query filters
        return emptyList() // Placeholder
    }

    override suspend fun <T> getDocument(path: String): T? {
        // Implementation for getting a single document
        return null // Placeholder
    }

    override suspend fun <T> createDocument(path: String, data: T): String {
        // Implementation for creating a document
        return "" // Placeholder
    }

    override suspend fun <T> updateDocument(path: String, id: String, data: T): Boolean {
        // Implementation for updating a document
        return false // Placeholder
    }

    override suspend fun updateFields(collectionPath: String, documentId: String, fields: Map<String, Any?>): Boolean {
        // Implementation for updating specific fields
        return false // Placeholder
    }

    override suspend fun deleteDocument(path: String, id: String): Boolean {
        // Implementation for deleting a document
        return false // Placeholder
    }

    // Like-related methods
    override suspend fun createLike(like: Like): String {
        // Implementation for creating a like
        return "" // Placeholder
    }

    override suspend fun deleteLike(userId: String, postId: String): Boolean {
        // Implementation for deleting a like
        return false // Placeholder
    }

    override suspend fun getLikesForPost(postId: String): List<Like> {
        // Implementation for getting likes for a post
        return emptyList() // Placeholder
    }

    override suspend fun isPostLikedByUser(userId: String, postId: String): Boolean {
        // Implementation for checking if a post is liked by a user
        return false // Placeholder
    }

    override suspend fun getPostsLikedByUser(userId: String): List<String> {
        // Implementation for getting posts liked by a user
        return emptyList() // Placeholder
    }
}

actual fun createFirebaseDatabase(): FirebaseDatabaseInterface {
    return FirebaseDatabaseIOS()
}