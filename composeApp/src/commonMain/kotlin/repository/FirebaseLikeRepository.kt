package repository

import data.model.Post
import firebase.database.FirebaseDatabaseInterface
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class FirebaseLikeRepository(
    private val database: FirebaseDatabaseInterface
) : LikeRepository {

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleLike(userId: String, postId: String): Result<Boolean> {
        return try {
            // Check if this user already liked this post using postLikes collection
            val likePath = "postLikes/$postId/$userId"

            val likeExists = try {
                database.getDocument<Any>(likePath) != null
            } catch (e: ClassCastException) {
                true
            }

            // Get the post to update its like count
            val post = database.getDocument<Post>("posts/$postId") ?: return Result.failure(Exception("Post not found"))

            if (likeExists) {
                // User already liked this post, so unlike it
                val success = database.deleteDocument("postLikes/$postId", userId)
                // Also remove from userLikes
                if (success) {
                    database.deleteDocument("userLikes/$userId", postId)

                    // Only update the likeCount field
                    val newCount = maxOf(0, post.likeCount - 1)
                    database.updateFields("posts", postId, mapOf("likeCount" to newCount))
                }
                Result.success(false) // Post is now unliked
            } else {
                // User hasn't liked this post yet, so like it
                val timestamp = Clock.System.now().toString()

                // Add to postLikes collection
                val likeData = mapOf("timestamp" to timestamp)
                val success1 = database.updateDocument("postLikes/$postId", userId, likeData)

                // Also add to userLikes collection
                val success2 = database.updateDocument("userLikes/$userId", postId, likeData)

                // Only update the likeCount field
                if (success1 && success2) {
                    val newCount = post.likeCount + 1
                    database.updateFields("posts", postId, mapOf("likeCount" to newCount))
                }

                Result.success(success1 && success2) // Post is now liked
            }
        } catch (e: Exception) {
            println("Error toggling like: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getLikesForPost(postId: String): Result<Set<String>> {
        return try {
            val likes = database.getLikesForPost(postId)
            Result.success(likes.map { it.userId }.toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Alternative implementation if documentExists is not available
    override suspend fun isLikedByUser(userId: String, postId: String): Result<Boolean> {
        return try {
            // Just check if we can access the document, not parsing it
            val likePath = "postLikes/$postId/$userId"
            val exists = try {
                database.getDocument<Any>(likePath) != null
            } catch (e: ClassCastException) {
                // If there's a class cast exception, the document exists but with an unexpected type
                true
            }
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}