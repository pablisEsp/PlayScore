package repository

import data.model.Like
import firebase.database.FirebaseDatabaseInterface
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class FirebaseLikeRepository(
    private val database: FirebaseDatabaseInterface
) : LikeRepository {

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleLike(userId: String, postId: String): Result<Boolean> {
        return try {
            val isCurrentlyLiked = isLikedByUser(userId, postId).getOrDefault(false)

            if (isCurrentlyLiked) {
                // Unlike: remove the like
                database.deleteLike(userId, postId)
                Result.success(false)
            } else {
                // Like: add new like
                val like = Like(userId, postId, Clock.System.now().toString())
                database.createLike(like)
                Result.success(true)
            }
        } catch (e: Exception) {
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

    override suspend fun isLikedByUser(userId: String, postId: String): Result<Boolean> {
        return try {
            val isLiked = database.isPostLikedByUser(userId, postId)
            Result.success(isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}