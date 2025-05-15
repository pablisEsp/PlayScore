package repository

import data.model.Post
import firebase.database.FirebaseDatabaseInterface
import firebase.database.createFirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FirebasePostRepository(
    private val database: FirebaseDatabaseInterface = createFirebaseDatabase()
) : PostRepository {
    private val postsPath = "posts"

    override fun getAllPosts(forceRefresh: Boolean): Flow<List<Post>> = flow {
        try {
            val posts = database.getCollection<Post>(postsPath)
            println("Fetched ${posts.size} posts from $postsPath")
            val filteredPosts = posts
                .filter { it.parentPostId.isNullOrEmpty() }
                .sortedByDescending { it.createdAt.toLongOrNull() ?: 0L }
            println("Filtered to ${filteredPosts.size} top-level posts")
            emit(filteredPosts)
        } catch (e: Exception) {
            println("Error fetching posts: ${e.message}")
            emit(emptyList())
        }
    }


    override fun getPostsByAuthor(authorId: String): Flow<List<Post>> = flow {
        val posts = database.getCollectionFiltered<Post>(
            path = postsPath,
            field = "authorId",
            value = authorId
        )
        emit(posts)
    }

    override fun getReplies(parentPostId: String): Flow<List<Post>> = flow {
        val replies = database.getCollectionFiltered<Post>(
            path = postsPath,
            field = "parentPostId",
            value = parentPostId
        ).sortedBy { it.createdAt }
        emit(replies)
    }

    override suspend fun createPost(post: Post): String {
        return database.createDocument(postsPath, post)
    }

    override suspend fun updatePost(post: Post) {
        database.updateDocument(postsPath, post.id, post)
    }

    override suspend fun deletePost(postId: String) {
        database.deleteDocument(postsPath, postId)
    }

    override suspend fun likePost(postId: String) {
        val post = database.getDocument<Post>("$postsPath/$postId") ?: return
        val updatedPost = post.copy(likeCount = post.likeCount + 1)
        database.updateDocument(postsPath, postId, updatedPost)
    }
}