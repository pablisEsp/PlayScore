package repository

import data.model.Post
import data.model.Report
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(forceRefresh: Boolean = false): Flow<List<Post>>
    fun getPostsByAuthor(authorId: String): Flow<List<Post>>
    fun getReplies(parentPostId: String): Flow<List<Post>>
    fun getReportedPosts(): Flow<List<Pair<Post, Report>>>
    suspend fun getPostById(postId: String): Flow<Post?>
    suspend fun createPost(post: Post): String
    suspend fun updatePost(post: Post)
    suspend fun deletePost(postId: String): Result<Boolean>
    suspend fun createReport(report: Report): Result<String>
    suspend fun likePost(postId: String)
    suspend fun getCommentsForPost(postId: String): Flow<List<Post>>
    suspend fun hasReplies(postId: String): Boolean
}