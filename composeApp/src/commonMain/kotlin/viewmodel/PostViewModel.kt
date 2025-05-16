package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Post
import data.model.User
import firebase.auth.FirebaseAuthInterface
import firebase.database.FirebaseDatabaseInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import repository.LikeRepository
import repository.PostRepository

class PostViewModel(
    private val postRepository: PostRepository,
    private val auth: FirebaseAuthInterface,
    private val likeRepository: LikeRepository,
    private val database: FirebaseDatabaseInterface
) : ViewModel() {
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        getAllPosts(forceRefresh = true)

    }

    fun createPost(content: String, mediaUrls: List<String> = emptyList(), parentPostId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = getCurrentUser()
                val post = Post(
                    authorId = currentUser.id,
                    authorName = currentUser.name,
                    content = content,
                    mediaUrls = mediaUrls,
                    parentPostId = parentPostId,
                    createdAt = Clock.System.now().toString()
                )

                val result = postRepository.createPost(post)

                // After successfully creating the post, refresh the posts list
                if (result.isNotEmpty()) {
                    getAllPosts(forceRefresh = true) // This will update _posts with the new post included
                } else {
                    _isLoading.value = false // Only set to false if we're not calling getAllPosts
                }
            } catch (e: Exception) {
                //Log.e("PostViewModel", "Error creating post: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            val user = getCurrentUser()
            val result = likeRepository.toggleLike(user.id, postId)
            if (result.isSuccess) {
                val isNowLiked = result.getOrNull() ?: return@launch
                _posts.update { list ->
                    list.map { post ->
                        if (post.id != postId) post
                        else post.copy(
                            isLikedByCurrentUser = isNowLiked,
                            likeCount = post.likeCount + if (isNowLiked) 1 else -1
                        )
                    }
                }
            } else {
                // handle error if needed
            }
        }
    }

    fun getAllPosts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get the current user ID for like status checking
                val currentUserId = auth.getCurrentUser()?.uid ?: ""

                postRepository.getAllPosts(forceRefresh = forceRefresh).collect { fetchedPosts ->
                    // Process each post to check if it's liked by current user
                    val enhancedPosts = fetchedPosts.map { post ->
                        println("Post ID: ${post.id}, Timestamp: ${post.createdAt}")

                        // Check if this post is liked by the current user
                        val isLiked = if (currentUserId.isNotEmpty()) {
                            likeRepository.isLikedByUser(currentUserId, post.id).getOrNull() ?: false
                        } else false

                        // Return the post with correct like status
                        post.copy(isLikedByCurrentUser = isLiked)
                    }

                    _posts.value = enhancedPosts
                }
            } catch (e: Exception) {
                // Handle any exceptions
                println("Error loading posts: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getCurrentUser(): User {
        val uid = auth.getCurrentUser()?.uid ?: throw IllegalStateException("User not logged in")
        return database.getUserData(uid) ?: throw IllegalStateException("User data not found")
    }
}

