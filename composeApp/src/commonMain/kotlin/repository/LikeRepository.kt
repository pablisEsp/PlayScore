package repository

interface LikeRepository {
    // Single method to toggle like status
    suspend fun toggleLike(userId: String, postId: String): Result<Boolean> // Returns new like status

    // Get all likes for a post
    suspend fun getLikesForPost(postId: String): Result<Set<String>> // Set of userIds

    // Check if user has liked post
    suspend fun isLikedByUser(userId: String, postId: String): Result<Boolean>
}