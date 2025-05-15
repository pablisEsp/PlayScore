package repository

import data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(forceRefresh: Boolean = false): Flow<List<Post>>
    fun getPostsByAuthor(authorId: String): Flow<List<Post>>
    fun getReplies(parentPostId: String): Flow<List<Post>>
    suspend fun createPost(post: Post): String
    suspend fun updatePost(post: Post)
    suspend fun deletePost(postId: String)
    suspend fun likePost(postId: String)
}