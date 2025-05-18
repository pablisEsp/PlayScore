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
                .sortedByDescending {
                    try {
                        if (it.createdAt.isNotEmpty())
                            kotlinx.datetime.Instant.parse(it.createdAt).toEpochMilliseconds()
                        else 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
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

    override suspend fun getCommentsForPost(postId: String): Flow<List<Post>> = flow {
        try {
            val allPosts = database.getCollection<Post>("posts")
            val comments = allPosts.filter { it.parentPostId == postId }
                .sortedByDescending {
                    try {
                        if (it.createdAt.isNotEmpty())
                            kotlinx.datetime.Instant.parse(it.createdAt).toEpochMilliseconds()
                        else 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
            emit(comments)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun hasReplies(postId: String): Boolean {
        val allPosts = database.getCollection<Post>("posts")
        return allPosts.any { it.parentPostId == postId }
    }

    override suspend fun getPostById(postId: String): Flow<Post?> = flow {
        try {
            val documentPath = "$postsPath/$postId"
            val post = database.getDocument<Post>(documentPath)
            emit(post)
        } catch (e: Exception) {
            println("Error fetching post by ID '$postId': ${e.message}")
            emit(null)
        }
    }

}