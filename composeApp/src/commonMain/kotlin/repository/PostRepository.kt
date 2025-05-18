package repository

import data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(forceRefresh: Boolean = false): Flow<List<Post>>
    fun getPostsByAuthor(authorId: String): Flow<List<Post>>
    fun getReplies(parentPostId: String): Flow<List<Post>>
    suspend fun getPostById(postId: String): Flow<Post?>
    suspend fun createPost(post: Post): String
    suspend fun updatePost(post: Post)
    suspend fun deletePost(postId: String)
    suspend fun likePost(postId: String)
    suspend fun getCommentsForPost(postId: String): Flow<List<Post>>
    suspend fun hasReplies(postId: String): Boolean

}