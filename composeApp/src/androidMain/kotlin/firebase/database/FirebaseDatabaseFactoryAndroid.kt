package firebase.database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ServerValue
import data.model.Like
import data.model.Post
import data.model.TeamMembership
import data.model.TeamRole
import data.model.User
import data.model.UserRole
import data.model.UserStats
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.collections.emptyList
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

    override suspend fun getUserData(uid: String?): User? {
        if (uid.isNullOrEmpty()) {
            Log.e("FirebaseDatabase", "getUserData called with null or empty UID")
            return null
        }

        Log.d("FirebaseDatabase", "Attempting to get user data for UID: $uid")
        try {
            val snapshot = usersRef.child(uid).get().await()

            if (snapshot.exists()) {
                try {
                    // Manual mapping from snapshot to User object
                    val id = snapshot.child("id").getValue(String::class.java) ?: uid
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""

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

                    // Rest of your mapping code...

                    // Create the complete user object
                    val user = User(
                        id = id,
                        name = name,
                        email = email,
                        username = username,
                        globalRole = globalRole,
                        profileImage = profileImage,
                        createdAt = createdAt
                    )

                    Log.d("FirebaseDatabase", "Successfully mapped user data: $user")
                    return user
                } catch (e: Exception) {
                    Log.e("FirebaseDatabase", "Error mapping user data: ${e.message}", e)
                    return null
                }
            } else {
                Log.w("FirebaseDatabase", "User data not found for UID: $uid. Creating new user.")

                // Handle case where user doesn't exist in database
                val auth = FirebaseAuth.getInstance()
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val newUser = User(
                        id = uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        username = firebaseUser.email ?: "", // Using email as default username
                        createdAt = Date().time.toString()
                    )

                    // Save this minimal user to the database
                    try {
                        usersRef.child(uid).setValue(newUser).await()
                        Log.d("FirebaseDatabase", "Created new user in database: $newUser")
                        return newUser
                    } catch (e: Exception) {
                        Log.e("FirebaseDatabase", "Failed to create new user: ${e.message}")
                        return newUser // Still return the user even if saving failed
                    }
                } else {
                    Log.e("FirebaseDatabase", "No Firebase Auth user found")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseDatabase", "Failed to get user data: ${e.message}")
            return null
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

    override suspend fun updateUsername(userId: String, username: String): Boolean = suspendCoroutine { continuation ->
        // First check if username is already taken
        database.getReference("usernames").child(username).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Username already taken
                    continuation.resume(false)
                } else {
                    // Username is available, update in two places:
                    // 1. In the user document
                    // 2. In a usernames collection for uniqueness checking
                    val updates = hashMapOf<String, Any>(
                        "users/$userId/username" to username,
                        "usernames/$username" to userId
                    )

                    database.reference.updateChildren(updates)
                        .addOnSuccessListener {
                            continuation.resume(true)
                        }
                        .addOnFailureListener {
                            continuation.resume(false)
                        }
                }
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    override suspend fun checkUsernameAvailable(username: String): Boolean {
        if (username.isBlank()) {
            return false
        }

        return try {
            // Query the usernames collection to check if this username exists
            val snapshot = database.reference
                .child("usernames")
                .child(username)
                .get()
                .await()

            // If snapshot doesn't exist, username is available
            !snapshot.exists()
        } catch (e: Exception) {
            println("Error checking username availability: ${e.message}")
            false // Default to unavailable in case of errors
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

    // Replace the existing generic methods with these properly implemented versions

    override suspend fun <T> getCollection(path: String): List<T> = suspendCoroutine { continuation ->
        val ref = database.getReference(path)
        ref.get().addOnSuccessListener { snapshot ->
            val items = mutableListOf<T>()
            for (childSnapshot in snapshot.children) {
                try {
                    // Create a Post object manually from the individual fields
                    // For example, if T is Post:
                    if (path == "posts") {
                        val id = childSnapshot.key ?: ""
                        val authorId = childSnapshot.child("authorId").getValue(String::class.java) ?: ""
                        val authorName = childSnapshot.child("authorName").getValue(String::class.java) ?: ""
                        val content = childSnapshot.child("content").getValue(String::class.java) ?: ""
                        val mediaUrls = childSnapshot.child("mediaUrls").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                        val likeCount = childSnapshot.child("likeCount").getValue(Int::class.java) ?: 0
                        val parentPostId = childSnapshot.child("parentPostId").getValue(String::class.java)
                        val createdAt = childSnapshot.child("createdAt").getValue(String::class.java) ?: ""

                        val post = Post(
                            id = id,
                            authorId = authorId,
                            authorName = authorName,
                            content = content,
                            mediaUrls = mediaUrls,
                            likeCount = likeCount,
                            parentPostId = parentPostId,
                            createdAt = createdAt
                        ) as T

                        items.add(post)
                    } else {
                        // Handle other collection types
                        val item = childSnapshot.getValue(Object::class.java) as? T
                        if (item != null) {
                            items.add(item)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseDatabase", "Error deserializing item: ${e.message}")
                }
            }
            continuation.resume(items)
        }.addOnFailureListener { e ->
            Log.e("FirebaseDatabase", "Error fetching collection: ${e.message}")
            continuation.resume(emptyList())
        }
    }

    override suspend fun <T> getCollectionFiltered(path: String, field: String, value: Any?): List<T> = suspendCoroutine { continuation ->
        val query = when (value) {
            is String -> database.getReference(path).orderByChild(field).equalTo(value)
            is Boolean -> database.getReference(path).orderByChild(field).equalTo(value)
            is Double -> database.getReference(path).orderByChild(field).equalTo(value)
            is Int -> database.getReference(path).orderByChild(field).equalTo(value.toDouble())
            else -> database.getReference(path).orderByChild(field)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val items = mutableListOf<T>()
                for (childSnapshot in snapshot.children) {
                    try {
                        val key = childSnapshot.key ?: ""
                        val valueMap = childSnapshot.getValue(Map::class.java) as? Map<String, Any>
                        val item = deserializeToType<T>(valueMap, key)
                        item?.let { items.add(it) }
                    } catch (e: Exception) {
                        Log.e("FirebaseDatabase", "Error deserializing filtered item: ${e.message}")
                    }
                }
                continuation.resume(items)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to get filtered collection: ${e.message}")
                continuation.resume(emptyList())
            }
    }

    override suspend fun <T> getDocument(path: String): T? = suspendCoroutine { continuation ->
        database.getReference(path).get()
            .addOnSuccessListener { snapshot ->
                try {
                    val key = path.split("/").last()
                    val valueMap = snapshot.getValue(Map::class.java) as? Map<String, Any>
                    val item = deserializeToType<T>(valueMap, key)
                    continuation.resume(item)
                } catch (e: Exception) {
                    Log.e("FirebaseDatabase", "Error deserializing document: ${e.message}")
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to get document: ${e.message}")
                continuation.resume(null)
            }
    }

    override suspend fun <T> createDocument(path: String, data: T): String = suspendCoroutine { continuation ->
        val ref = database.getReference(path).push()
        val id = ref.key ?: ""

        // Add id to the data if it's a Map
        val dataWithId = when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (data as Map<String, Any>).toMutableMap().apply { put("id", id) }
            }
            else -> data
        }

        ref.setValue(dataWithId)
            .addOnSuccessListener {
                continuation.resume(id)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to create document: ${e.message}")
                continuation.resume(id) // Still return the ID even if there was an error
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> deserializeToType(map: Map<String, Any>?, id: String): T? {
        if (map == null) return null

        return try {
            // Add ID to map if not present
            val mutableMap = map.toMutableMap()
            if (!mutableMap.containsKey("id")) {
                mutableMap["id"] = id
            }

            // Create the appropriate object based on class name reflection
            val className = mutableMap["class"] as? String ?: "data.model.Post"

            when (className) {
                "data.model.Post" -> {
                    data.model.Post(
                        id = mutableMap["id"] as? String ?: id,
                        authorId = mutableMap["authorId"] as? String ?: "",
                        authorName = mutableMap["authorName"] as? String ?: "",
                        content = mutableMap["content"] as? String ?: "",
                        mediaUrls = mutableMap["mediaUrls"] as? List<String> ?: emptyList(),
                        likeCount = (mutableMap["likeCount"] as? Number)?.toInt() ?: 0,
                        parentPostId = mutableMap["parentPostId"] as? String,
                        createdAt = mutableMap["createdAt"] as? String ?: ""
                    ) as T
                }
                // Add other model cases as needed
                else -> null
            }
        } catch (e: Exception) {
            Log.e("FirebaseDatabase", "Error converting map to object: ${e.message}")
            null
        }
    }

    override suspend fun deleteDocument(path: String, id: String): Boolean = suspendCoroutine { continuation ->
        database.getReference("$path/$id").removeValue()
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to delete document: ${e.message}")
                continuation.resume(false)
            }
    }


    override suspend fun <T> updateDocument(path: String, id: String, data: T): Boolean = suspendCoroutine { continuation ->
        database.getReference("$path/$id").setValue(data)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to update document: ${e.message}")
                continuation.resume(false)
            }
    }


    override suspend fun createLike(like: Like): String = suspendCoroutine { continuation ->
        // Use multi-path updates to write to both collections and update the post counter atomically
        val updates = hashMapOf<String, Any>(
            "userLikes/${like.userId}/${like.postId}" to true,
            "postLikes/${like.postId}/${like.userId}" to true,
            "posts/${like.postId}/likeCount" to ServerValue.increment(1)
        )

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("FirebaseDatabase", "Like created for post ${like.postId} by user ${like.userId}")
                continuation.resume(like.postId)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to create like: ${e.message}")
                continuation.resume("")
            }
    }

    override suspend fun deleteLike(userId: String, postId: String): Boolean = suspendCoroutine { continuation ->
        // Use multi-path updates to remove from both collections and decrement the counter atomically
        val updates = hashMapOf<String, Any?>(  // Changed from Any to Any?
            "userLikes/$userId/$postId" to null,
            "postLikes/$postId/$userId" to null,
            "posts/$postId/likeCount" to ServerValue.increment(-1)
        )

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("FirebaseDatabase", "Like removed for post $postId by user $userId")
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to delete like: ${e.message}")
                continuation.resume(false)
            }
    }

    override suspend fun isPostLikedByUser(userId: String, postId: String): Boolean = suspendCoroutine { continuation ->
        // Check the user-centric collection for better performance
        val likeRef = database.getReference("userLikes/$userId/$postId")

        likeRef.get()
            .addOnSuccessListener { snapshot ->
                val exists = snapshot.exists()
                continuation.resume(exists)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to check if post is liked: ${e.message}")
                continuation.resume(false)
            }
    }

    override suspend fun getLikesForPost(postId: String): List<Like> = suspendCoroutine { continuation ->
        val likesRef = database.getReference("postLikes/$postId")

        likesRef.get()
            .addOnSuccessListener { snapshot ->
                val likes = mutableListOf<Like>()
                val currentTime = Date().time.toString()

                for (childSnapshot in snapshot.children) {
                    try {
                        val userId = childSnapshot.key ?: continue
                        // We're only storing boolean flags, not full Like objects
                        likes.add(Like(userId, postId, currentTime))
                    } catch (e: Exception) {
                        Log.e("FirebaseDatabase", "Error parsing like: ${e.message}")
                    }
                }
                continuation.resume(likes)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to get likes for post: ${e.message}")
                continuation.resume(emptyList())
            }
    }

    // New method to get posts liked by a user
    override suspend fun getPostsLikedByUser(userId: String): List<String> = suspendCoroutine { continuation ->
        val userLikesRef = database.getReference("userLikes/$userId")

        userLikesRef.get()
            .addOnSuccessListener { snapshot ->
                val likedPostIds = mutableListOf<String>()
                for (childSnapshot in snapshot.children) {
                    val postId = childSnapshot.key
                    if (postId != null) {
                        likedPostIds.add(postId)
                    }
                }
                continuation.resume(likedPostIds)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to get posts liked by user: ${e.message}")
                continuation.resume(emptyList())
            }
    }
}

actual fun createFirebaseDatabase(): FirebaseDatabaseInterface {
    return FirebaseDatabaseAndroid()
}