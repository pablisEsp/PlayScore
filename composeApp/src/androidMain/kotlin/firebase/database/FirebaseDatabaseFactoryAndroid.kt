package firebase.database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ServerValue
import data.model.ApplicationStatus
import data.model.BracketType
import data.model.CreatorType
import data.model.Like
import data.model.Post
import data.model.RequestStatus
import data.model.Team
import data.model.TeamApplication
import data.model.TeamJoinRequest
import data.model.TeamMembership
import data.model.TeamRole
import data.model.Tournament
import data.model.TournamentStatus
import data.model.User
import data.model.UserRole
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
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

                    // Extract team membership data if it exists
                    val teamMembershipSnapshot = snapshot.child("teamMembership")
                    val teamMembership = if (teamMembershipSnapshot.exists()) {
                        val teamId = teamMembershipSnapshot.child("teamId").getValue(String::class.java)
                        val roleStr = teamMembershipSnapshot.child("role").getValue(String::class.java)
                        val role = try {
                            if (roleStr != null) TeamRole.valueOf(roleStr) else null
                        } catch (e: Exception) {
                            Log.w("FirebaseDatabase", "Invalid team role value: $roleStr, defaulting to null")
                            null
                        }
                        TeamMembership(teamId = teamId, role = role)
                    } else null

                    // Create user object with the extracted teamMembership
                    val user = User(
                        id = id,
                        name = name,
                        email = email,
                        username = username,
                        globalRole = globalRole,
                        teamMembership = teamMembership,  // Include the teamMembership
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

    override suspend fun updateFields(collectionPath: String, documentId: String, fields: Map<String, Any?>): Boolean = suspendCoroutine { continuation ->
        // Create a reference to the document
        val docRef = database.getReference("$collectionPath/$documentId")

        // Use updateChildren() to only update the specified fields
        docRef.updateChildren(fields)
            .addOnSuccessListener {
                Log.d("FirebaseDatabase", "Fields updated successfully for $collectionPath/$documentId")
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Error updating fields: ${e.message}", e)
                continuation.resume(false)
            }
    }


    override suspend fun updateUserData(uid: String, updates: Map<String, Any?>): Boolean = suspendCoroutine { continuation ->
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
            Log.d("UsernameCheck", "Blank username, returning unavailable")
            return false
        }

        return try {
            Log.d("UsernameCheck", "Checking availability for username: $username")

            // Query the usernames collection to check if this username exists
            val snapshot = database.reference
                .child("usernames")
                .child(username)
                .get()
                .await()

            val isAvailable = !snapshot.exists()
            Log.d("UsernameCheck", "Username '$username' exists in DB: ${snapshot.exists()}, isAvailable: $isAvailable")

            isAvailable
        } catch (e: Exception) {
            Log.e("UsernameCheck", "Error checking username availability: ${e.message}", e)
            // During initial app usage when the usernames node might not exist yet,
            // we should consider usernames available instead of unavailable
            true // Changed to true to allow first users to register
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

    private fun logRequestDetails(methodName: String, path: String) {
        Log.d("FirebaseDatabase", "⭐⭐⭐ $methodName called for path: $path ⭐⭐⭐")
        //Log.d("FirebaseDatabase", "Current user: ${auth.getCurrentUser()?.email}")
    }

    override suspend fun <T> getCollection(path: String, serializer: KSerializer<List<T>>): List<T> = suspendCoroutine { continuation ->
        logRequestDetails("getCollection", path)
        val ref = database.getReference(path)
        ref.get().addOnSuccessListener { snapshot ->
            try {
                val items = deserializeSnapshotToList<T>(snapshot, serializer.descriptor)
                continuation.resume(items)
            } catch (e: Exception) {
                Log.e("FirebaseDatabase", "Error deserializing collection: ${e.message}", e)
                continuation.resume(emptyList())
            }
        }.addOnFailureListener { e ->
            Log.e("FirebaseDatabase", "Error fetching collection: ${e.message}", e)
            continuation.resume(emptyList())
        }
    }

    override suspend fun <T> getCollectionFiltered(
        path: String,
        field: String,
        value: Any?,
        serializer: KSerializer<List<T>>
    ): List<T> = suspendCoroutine { continuation ->
        logRequestDetails("getCollectionFiltered", "$path (field: $field, value: $value)")
        val query = when (value) {
            is String -> database.getReference(path).orderByChild(field).equalTo(value)
            is Boolean -> database.getReference(path).orderByChild(field).equalTo(value)
            is Double -> database.getReference(path).orderByChild(field).equalTo(value)
            is Int -> database.getReference(path).orderByChild(field).equalTo(value.toDouble())
            null -> database.getReference(path).orderByChild(field).equalTo(null)
            else -> database.getReference(path).orderByChild(field)
        }

        query.get().addOnSuccessListener { snapshot ->
            try {
                val items = deserializeSnapshotToList<T>(snapshot, serializer.descriptor)
                continuation.resume(items)
            } catch (e: Exception) {
                Log.e("FirebaseDatabase", "Error deserializing filtered collection: ${e.message}", e)
                continuation.resume(emptyList())
            }
        }.addOnFailureListener { e ->
            Log.e("FirebaseDatabase", "Error fetching filtered collection: ${e.message}", e)
            continuation.resume(emptyList())
        }
    }

    private fun <T> deserializeSnapshotToList(snapshot: DataSnapshot, descriptor: SerialDescriptor): List<T> {
        val elementType = descriptor.getElementDescriptor(0).serialName
        val items = mutableListOf<T>()

        for (childSnapshot in snapshot.children) {
            val key = childSnapshot.key ?: continue

            // Check element type rather than full serializer name
            when {
                elementType.endsWith("data.model.User") -> {
                    val user = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})?.let { valueMap ->
                        val userId = childSnapshot.key ?: ""
                        User(
                            id = userId,
                            name = valueMap["name"] as? String ?: "",
                            email = valueMap["email"] as? String ?: "",
                            username = valueMap["username"] as? String ?: "",
                            globalRole = when (valueMap["globalRole"] as? String) {
                                "ADMIN" -> UserRole.ADMIN
                                "SUPER_ADMIN" -> UserRole.SUPER_ADMIN
                                else -> UserRole.USER
                            },
                            teamMembership = null, // Add logic to extract team membership if needed
                            profileImage = valueMap["profileImage"] as? String ?: "",
                            isBanned = valueMap["isBanned"] as? Boolean ?: false,
                            createdAt = valueMap["createdAt"] as? String ?: ""
                        )
                    }
                    if (user != null) items.add(user as T)
                }

                elementType.endsWith("data.model.Post") -> {
                    val post = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})?.let { valueMap ->
                        val postId = childSnapshot.key ?: ""
                        Post(
                            id = postId,
                            authorId = valueMap["authorId"] as? String ?: "",
                            authorName = valueMap["authorName"] as? String ?: "",
                            content = valueMap["content"] as? String ?: "",
                            mediaUrls = (valueMap["mediaUrls"] as? List<String>) ?: emptyList(),
                            likeCount = (valueMap["likeCount"] as? Long)?.toInt() ?: 0,
                            parentPostId = valueMap["parentPostId"] as? String,
                            createdAt = valueMap["createdAt"] as? String ?: Date().toString(),
                            isLikedByCurrentUser = false
                        )
                    }
                    if (post != null) items.add(post as T)
                }

                elementType.endsWith("data.model.Team") -> {
                    val team = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})?.let { valueMap ->
                        Team(
                            id = key,
                            name = valueMap["name"] as? String ?: "",
                            description = valueMap["description"] as? String ?: "",
                            presidentId = valueMap["presidentId"] as? String ?: "",
                            vicePresidentId = valueMap["vicePresidentId"] as? String,
                            captainIds = (valueMap["captainIds"] as? List<String>) ?: emptyList(),
                            playerIds = (valueMap["playerIds"] as? List<String>) ?: emptyList(),
                            createdAt = valueMap["createdAt"] as? String ?: Date().toString()
                        )
                    }
                    if (team != null) items.add(team as T)
                }

                elementType.endsWith("data.model.TeamJoinRequest") -> {
                    val request = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})?.let { valueMap ->
                        TeamJoinRequest(
                            id = key,
                            userId = valueMap["userId"] as? String ?: "",
                            teamId = valueMap["teamId"] as? String ?: "",
                            timestamp = valueMap["timestamp"] as? String ?: "",
                            status = when (valueMap["status"] as? String) {
                                "APPROVED" -> RequestStatus.APPROVED
                                "REJECTED" -> RequestStatus.REJECTED
                                else -> RequestStatus.PENDING
                            },
                            responseBy = (valueMap["responseBy"] as? String).toString(),
                            responseTimestamp = (valueMap["responseTimestamp"] as? String).toString()
                        )
                    }
                    if (request != null) items.add(request as T)
                }

                elementType.endsWith("data.model.Like") -> {
                    val like = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})?.let { valueMap ->
                        Like(
                            userId = valueMap["userId"] as? String ?: "",
                            postId = valueMap["postId"] as? String ?: "",
                            timestamp = valueMap["timestamp"] as? String ?: ""
                        )
                    }
                    if (like != null) items.add(like as T)
                }

                elementType.endsWith("data.model.Tournament") -> {
                    val tournament = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})?.let { valueMap ->
                        val key = childSnapshot.key ?: ""
                        Tournament(
                            id = key,
                            name = valueMap["name"] as? String ?: "",
                            description = valueMap["description"] as? String ?: "",
                            creatorId = valueMap["creatorId"] as? String ?: "",
                            creatorType = when (valueMap["creatorType"] as? String) {
                                "TEAM_PRESIDENT" -> CreatorType.TEAM_PRESIDENT
                                else -> CreatorType.ADMIN
                            },
                            startDate = valueMap["startDate"] as? String ?: "",
                            endDate = valueMap["endDate"] as? String ?: "",
                            status = when (valueMap["status"] as? String) {
                                "REGISTRATION" -> TournamentStatus.REGISTRATION
                                "ACTIVE" -> TournamentStatus.ACTIVE
                                "COMPLETED" -> TournamentStatus.COMPLETED
                                "CANCELLED" -> TournamentStatus.CANCELLED
                                else -> TournamentStatus.UPCOMING
                            },
                            teamIds = (valueMap["teamIds"] as? List<String>) ?: emptyList(),
                            maxTeams = (valueMap["maxTeams"] as? Long)?.toInt() ?: 8,
                            bracketType = when (valueMap["bracketType"] as? String) {
                                "DOUBLE_ELIMINATION" -> BracketType.DOUBLE_ELIMINATION
                                "ROUND_ROBIN" -> BracketType.ROUND_ROBIN
                                else -> BracketType.SINGLE_ELIMINATION
                            }
                        )
                    }
                    if (tournament != null) items.add(tournament as T)
                }

                elementType.endsWith("data.model.TeamApplication") -> {
                    val application = childSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})?.let { valueMap ->
                        val key = childSnapshot.key ?: ""
                        TeamApplication(
                            id = key,
                            teamId = valueMap["teamId"] as? String ?: "",
                            tournamentId = valueMap["tournamentId"] as? String ?: "",
                            status = when (valueMap["status"] as? String) {
                                "APPROVED" -> ApplicationStatus.APPROVED
                                "REJECTED" -> ApplicationStatus.REJECTED
                                else -> ApplicationStatus.PENDING
                            },
                            appliedAt = valueMap["appliedAt"] as? String ?: ""
                        )
                    }
                    if (application != null) items.add(application as T)
                }

                else -> {
                    Log.e("FirebaseDatabase", "Unsupported element type: $elementType")
                    continue
                }
            }
        }

        return items
    }


    override suspend fun <T> getDocument(path: String): T? = suspendCoroutine { continuation ->
        database.getReference(path).get()
            .addOnSuccessListener { snapshot ->
                try {
                    // Check if this is a post path (most common case)
                    if (path.startsWith("posts/")) {
                        val id = snapshot.key ?: path.split("/").last()
                        // Directly access child fields instead of using generic Map conversion
                        val authorId = snapshot.child("authorId").getValue(String::class.java) ?: ""
                        val authorName = snapshot.child("authorName").getValue(String::class.java) ?: ""
                        val content = snapshot.child("content").getValue(String::class.java) ?: ""
                        val mediaUrls = snapshot.child("mediaUrls").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                        val likeCount = snapshot.child("likeCount").getValue(Int::class.java) ?: 0
                        val parentPostId = snapshot.child("parentPostId").getValue(String::class.java)
                        val createdAt = snapshot.child("createdAt").getValue(String::class.java) ?: ""

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
                        continuation.resume(post)
                    }else if (path.startsWith("teams/")) {
                        // Special handling for teams
                        val id = snapshot.key ?: path.split("/").last()
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val presidentId =
                            snapshot.child("presidentId").getValue(String::class.java) ?: ""
                        val vicePresidentId =
                            snapshot.child("vicePresidentId").getValue(String::class.java)
                        val captainIds = snapshot.child("captainIds")
                            .getValue(object : GenericTypeIndicator<List<String>>() {})
                            ?: emptyList()
                        val playerIds = snapshot.child("playerIds")
                            .getValue(object : GenericTypeIndicator<List<String>>() {})
                            ?: emptyList()
                        val pointsTotal =
                            snapshot.child("pointsTotal").getValue(Int::class.java) ?: 0
                        val createdAt =
                            snapshot.child("createdAt").getValue(String::class.java) ?: ""
                        val description =
                            snapshot.child("description").getValue(String::class.java) ?: ""
                        val logoUrl = snapshot.child("logoUrl").getValue(String::class.java) ?: ""
                        val location = snapshot.child("location").getValue(String::class.java) ?: ""
                        val ranking = snapshot.child("ranking").getValue(Int::class.java) ?: 0
                        val totalWins = snapshot.child("totalWins").getValue(Int::class.java) ?: 0
                        val totalLosses =
                            snapshot.child("totalLosses").getValue(Int::class.java) ?: 0

                        val team = data.model.Team(
                            id = id,
                            name = name,
                            presidentId = presidentId,
                            vicePresidentId = vicePresidentId,
                            captainIds = captainIds,
                            playerIds = playerIds,
                            pointsTotal = pointsTotal,
                            createdAt = createdAt,
                            description = description,
                            logoUrl = logoUrl,
                            location = location,
                            ranking = ranking,
                            totalWins = totalWins,
                            totalLosses = totalLosses
                        ) as T
                        continuation.resume(team)
                    }else if (path.startsWith("tournaments/")) {
                        val id = snapshot.key ?: path.split("/").last()
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val description = snapshot.child("description").getValue(String::class.java) ?: ""
                        val creatorId = snapshot.child("creatorId").getValue(String::class.java) ?: ""
                        val creatorTypeStr = snapshot.child("creatorType").getValue(String::class.java) ?: "ADMIN"
                        val startDate = snapshot.child("startDate").getValue(String::class.java) ?: ""
                        val endDate = snapshot.child("endDate").getValue(String::class.java) ?: ""
                        val statusStr = snapshot.child("status").getValue(String::class.java) ?: "UPCOMING"
                        val maxTeams = snapshot.child("maxTeams").getValue(Int::class.java) ?: 8
                        val bracketTypeStr = snapshot.child("bracketType").getValue(String::class.java) ?: "SINGLE_ELIMINATION"

                        val tournament = Tournament(
                            id = id,
                            name = name,
                            description = description,
                            creatorId = creatorId,
                            creatorType = CreatorType.valueOf(creatorTypeStr),
                            startDate = startDate,
                            endDate = endDate,
                            status = TournamentStatus.valueOf(statusStr),
                            maxTeams = maxTeams,
                            bracketType = BracketType.valueOf(bracketTypeStr)
                        ) as T
                        continuation.resume(tournament)
                    }
                    else {
                        // For other document types
                        val key = path.split("/").last()
                        val valueMap = snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        val item = deserializeToType<T>(valueMap, key)
                        continuation.resume(item)
                    }
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

        // For Post objects, we'll create a new instance without the id field
        val dataToSave = when (data) {
            is Post -> {
                // Convert to map and remove id field
                val postMap = data.toMap().toMutableMap()
                postMap.remove("id") // Remove id completely instead of setting to empty
                postMap
            }
            is Map<*, *> -> {
                // For maps, filter out the id field if present
                @Suppress("UNCHECKED_CAST")
                val mapData = (data as Map<String, Any>).toMutableMap()
                mapData.remove("id") // Remove id field if it exists
                mapData
            }
            else -> data
        }

        ref.setValue(dataToSave)
            .addOnSuccessListener {
                continuation.resume(id)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDatabase", "Failed to create document: ${e.message}")
                continuation.resume(id) // Still return the ID even if there was an error
            }
    }

    // Extension function to convert Post to Map
    private fun Post.toMap(): Map<String, Any?> {
        return mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "content" to content,
            "mediaUrls" to mediaUrls,
            "likeCount" to likeCount,
            "parentPostId" to parentPostId,
            "createdAt" to createdAt
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> deserializeToType(map: Map<String, Any>?, id: String): T? {
        if (map == null) return null

        return try {
            // Try to determine the type by checking fields
            val className = when {
                // For TeamJoinRequest
                map.containsKey("teamId") && map.containsKey("userId") &&
                        map.containsKey("status") -> "data.model.TeamJoinRequest"

                // For Post
                map.containsKey("authorId") && map.containsKey("content") -> "data.model.Post"

                // For Team
                map.containsKey("presidentId") && map.containsKey("playerIds") -> "data.model.Team"


                map.containsKey("tournamentId") && map.containsKey("teamId") &&
                        map.containsKey("appliedAt") -> "data.model.TeamApplication"

                map.containsKey("bracketType") && map.containsKey("maxTeams") &&
                        map.containsKey("startDate") && map.containsKey("endDate") -> "data.model.Tournament"


                // Use class field if specified
                else -> map["class"] as? String ?: "data.model.Post"
            }

            when (className) {
                "data.model.Post" -> {
                    // Existing Post code
                    data.model.Post(
                        id = id,
                        authorId = map["authorId"] as? String ?: "",
                        authorName = map["authorName"] as? String ?: "",
                        content = map["content"] as? String ?: "",
                        mediaUrls = map["mediaUrls"] as? List<String> ?: emptyList(),
                        likeCount = (map["likeCount"] as? Number)?.toInt() ?: 0,
                        parentPostId = map["parentPostId"] as? String,
                        createdAt = map["createdAt"] as? String ?: ""
                    ) as T
                }
                "data.model.Team" -> {
                    // Existing Team code
                    data.model.Team(/* your existing code */) as T
                }
                "data.model.TeamJoinRequest" -> {
                    // New case to handle TeamJoinRequest
                    val statusStr = map["status"] as? String ?: "PENDING"
                    val status = try {
                        data.model.RequestStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        Log.w("FirebaseDatabase", "Invalid status: $statusStr, defaulting to PENDING")
                        data.model.RequestStatus.PENDING
                    }

                    data.model.TeamJoinRequest(
                        id = id,
                        teamId = map["teamId"] as? String ?: "",
                        userId = map["userId"] as? String ?: "",
                        status = status,
                        timestamp = map["timestamp"] as? String ?: "",
                        responseTimestamp = map["responseTimestamp"] as? String ?: "",
                        responseBy = map["responseBy"] as? String ?: ""
                    ) as T
                }

                "data.model.Tournament" -> {
                    data.model.Tournament(
                        id = id,
                        name = map["name"] as? String ?: "",
                        description = map["description"] as? String ?: "",
                        creatorId = map["creatorId"] as? String ?: "",
                        creatorType = when (map["creatorType"] as? String) {
                            "TEAM_PRESIDENT" -> data.model.CreatorType.TEAM_PRESIDENT
                            else -> data.model.CreatorType.ADMIN
                        },
                        startDate = map["startDate"] as? String ?: "",
                        endDate = map["endDate"] as? String ?: "",
                        status = when (map["status"] as? String) {
                            "REGISTRATION" -> data.model.TournamentStatus.REGISTRATION
                            "ACTIVE" -> data.model.TournamentStatus.ACTIVE
                            "COMPLETED" -> data.model.TournamentStatus.COMPLETED
                            "CANCELLED" -> data.model.TournamentStatus.CANCELLED
                            else -> data.model.TournamentStatus.UPCOMING
                        },
                        teamIds = map["teamIds"] as? List<String> ?: emptyList(),
                        maxTeams = (map["maxTeams"] as? Number)?.toInt() ?: 8,
                        bracketType = when (map["bracketType"] as? String) {
                            "DOUBLE_ELIMINATION" -> data.model.BracketType.DOUBLE_ELIMINATION
                            "ROUND_ROBIN" -> data.model.BracketType.ROUND_ROBIN
                            else -> data.model.BracketType.SINGLE_ELIMINATION
                        }
                    ) as T
                }

                "data.model.TeamApplication" -> {
                    data.model.TeamApplication(
                        id = id,
                        teamId = map["teamId"] as? String ?: "",
                        tournamentId = map["tournamentId"] as? String ?: "",
                        appliedAt = map["appliedAt"] as? String ?: "",
                        status = when (map["status"] as? String) {
                            "APPROVED" -> data.model.ApplicationStatus.APPROVED
                            "REJECTED" -> data.model.ApplicationStatus.REJECTED
                            else -> data.model.ApplicationStatus.PENDING
                        }
                    ) as T
                }
                else -> {
                    Log.e("FirebaseDatabase", "Unknown class type: $className")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseDatabase", "Error converting map to object: ${e.message}", e)
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