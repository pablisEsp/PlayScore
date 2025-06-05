package firebase.database

import data.model.Like
import data.model.User
import kotlinx.serialization.KSerializer

interface FirebaseDatabaseInterface {
    //User methods
    suspend fun saveUserData(userData: User): Boolean
    suspend fun getUserData(uid: String?): User?
    suspend fun updateUserData(uid: String, updates: Map<String, Any?>): Boolean
    suspend fun deleteUser(uid: String): Boolean
    suspend fun updateUsername(userId: String, username: String): Boolean
    suspend fun checkUsernameAvailable(username: String): Boolean

    // Generic methods for posts and other collections
    suspend fun <T> getCollection(path: String, serializer: KSerializer<List<T>>): List<T>
    suspend fun <T> getCollectionFiltered(
        path: String,
        field: String,
        value: Any?,
        serializer: KSerializer<List<T>>
    ): List<T>
    suspend fun <T> getDocument(path: String): T?
    suspend fun <T> createDocument(path: String, data: T): String
    suspend fun <T> updateDocument(path: String, id: String, data: T): Boolean
    suspend fun updateFields(collectionPath: String, documentId: String, fields: Map<String, Any?>): Boolean
    suspend fun deleteDocument(path: String, id: String): Boolean


    // Like-related methods
    suspend fun createLike(like: Like): String
    suspend fun deleteLike(userId: String, postId: String): Boolean
    suspend fun getLikesForPost(postId: String): List<Like>
    suspend fun isPostLikedByUser(userId: String, postId: String): Boolean
    suspend fun getPostsLikedByUser(userId: String): List<String>

}