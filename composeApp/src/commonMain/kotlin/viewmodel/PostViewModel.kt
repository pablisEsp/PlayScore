package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Post
import data.model.Report
import data.model.ReportStatus
import data.model.User
import data.model.UserRole
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

    private val _currentPost = MutableStateFlow<Post?>(null)
    val currentPost: StateFlow<Post?> = _currentPost.asStateFlow()

    private val _comments = MutableStateFlow<List<Post>>(emptyList())
    val comments: StateFlow<List<Post>> = _comments.asStateFlow()

    private val _parentPost = MutableStateFlow<Post?>(null)
    val parentPost: StateFlow<Post?> = _parentPost.asStateFlow()

    private val _reportedPosts = MutableStateFlow<List<Pair<Post, Report>>>(emptyList())
    val reportedPosts: StateFlow<List<Pair<Post, Report>>> = _reportedPosts.asStateFlow()

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
                    authorUsername = currentUser.username,
                    content = content,
                    mediaUrls = mediaUrls,
                    parentPostId = parentPostId,
                    createdAt = Clock.System.now().toString()
                )
                val postId = postRepository.createPost(post)
                if (postId.isNotEmpty()) {
                    if (parentPostId != null) {
                        // If this is a comment, refresh the comments for the parent post
                        getCommentsForPost(parentPostId)
                    } else {
                        // If this is a main post, refresh the main posts list
                        getAllPosts(forceRefresh = true)
                    }
                } else {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                println("Error creating post: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
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
                    // Update currentPost if it's the liked post
                    _currentPost.value?.let { post ->
                        if (post.id == postId) {
                            _currentPost.value = post.copy(
                                isLikedByCurrentUser = isNowLiked,
                                likeCount = post.likeCount + if (isNowLiked) 1 else -1
                            )
                        }
                    }

                    // Update comments list if the liked post is a comment
                    _comments.update { commentsList ->
                        commentsList.map { comment ->
                            if (comment.id != postId) comment
                            else comment.copy(
                                isLikedByCurrentUser = isNowLiked,
                                likeCount = comment.likeCount + if (isNowLiked) 1 else -1
                            )
                        }
                    }
                } else {
                    println("Error toggling like for post ID: $postId")
                }
            } catch (e: Exception) {
                println("Error liking post: ${e.message}")
            }
        }
    }

    fun getAllPosts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid ?: ""
                postRepository.getAllPosts(forceRefresh = forceRefresh).collect { fetchedPosts ->
                    val enhancedPosts = fetchedPosts.map { post ->
                        println("Post ID: ${post.id}, Timestamp: ${post.createdAt}")
                        val isLiked = if (currentUserId.isNotEmpty()) {
                            likeRepository.isLikedByUser(currentUserId, post.id).getOrNull() ?: false
                        } else false
                        post.copy(isLikedByCurrentUser = isLiked)
                    }
                    _posts.value = enhancedPosts
                }
            } catch (e: Exception) {
                println("Error loading posts: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getPostById(postId: String) {
        println("Fetching post with ID: $postId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid ?: ""
                postRepository.getPostById(postId).collect { post ->
                    if (post != null) {
                        val isLiked = if (currentUserId.isNotEmpty()) {
                            likeRepository.isLikedByUser(currentUserId, post.id).getOrNull() ?: false
                        } else false
                        _currentPost.value = post.copy(isLikedByCurrentUser = isLiked)
                        println("Fetched post: $post")
                    } else {
                        _currentPost.value = null
                        println("Post not found for ID: $postId")
                    }
                }
            } catch (e: Exception) {
                _currentPost.value = null
                println("Error loading post with ID: $postId, Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCommentsForPost(postId: String) {
        println("Fetching comments for post ID: $postId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = auth.getCurrentUser()?.uid ?: ""
                postRepository.getCommentsForPost(postId).collect { fetchedComments ->
                    val enhancedComments = fetchedComments.map { comment ->
                        val isLiked = if (currentUserId.isNotEmpty()) {
                            likeRepository.isLikedByUser(currentUserId, comment.id).getOrNull() ?: false
                        } else false
                        comment.copy(isLikedByCurrentUser = isLiked)
                    }
                    _comments.value = enhancedComments
                    println("Fetched ${enhancedComments.size} comments for post ID: $postId")
                }
            } catch (e: Exception) {
                _comments.value = emptyList()
                println("Error loading comments for post ID: $postId, Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getParentPostIfExists(postId: String) {
        println("Fetching parent post for post ID: $postId")
        viewModelScope.launch {
            try {
                postRepository.getPostById(postId).collect { post ->
                    if (post?.parentPostId != null) {
                        postRepository.getPostById(post.parentPostId).collect { parentPost ->
                            if (parentPost != null) {
                                val currentUserId = auth.getCurrentUser()?.uid ?: ""
                                val isLiked = if (currentUserId.isNotEmpty()) {
                                    likeRepository.isLikedByUser(currentUserId, parentPost.id).getOrNull() ?: false
                                } else false
                                _parentPost.value = parentPost.copy(isLikedByCurrentUser = isLiked)
                                println("Fetched parent post: $parentPost")
                            } else {
                                _parentPost.value = null
                                println("Parent post not found for post ID: ${post.parentPostId}")
                            }
                        }
                    } else {
                        _parentPost.value = null
                        println("No parent post for post ID: $postId")
                    }
                }
            } catch (e: Exception) {
                _parentPost.value = null
                println("Error loading parent post for post ID: $postId, Error: ${e.message}")
            }
        }
    }

    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = getCurrentUser()
                val report = Report(
                    postId = postId,
                    reporterId = user.id,
                    reason = reason,
                    timestamp = Clock.System.now().toString()
                )
                val result = postRepository.createReport(report)
                if (result.isSuccess) {
                    // Handle successful report
                }
            } catch (e: Exception) {
                println("Error reporting post: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = getCurrentUser()
                val post = _posts.value.find { it.id == postId }
                    ?: _currentPost.value
                    ?: _comments.value.find { it.id == postId }
                    ?: return@launch

                // Check if user has permission to delete
                val canDelete = post.authorId == user.id ||
                                user.globalRole == UserRole.ADMIN ||
                                user.globalRole == UserRole.SUPER_ADMIN

                if (canDelete) {
                    val result = postRepository.deletePost(postId)
                    if (result.isSuccess) {
                        // Update UI state based on which collection the post was in
                        _posts.update { list -> list.filter { it.id != postId } }
                        if (_currentPost.value?.id == postId) {
                            _currentPost.value = null
                        }
                        _comments.update { list -> list.filter { it.id != postId } }
                    }
                } else {
                    println("User doesn't have permission to delete post")
                }
            } catch (e: Exception) {
                println("Error deleting post: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getReportedPosts(showOnlyPending: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                postRepository.getReportedPosts().collect { reportedPostsList ->
                    // Filter for only pending reports if required
                    val filteredReports = if (showOnlyPending) {
                        reportedPostsList.filter { (_, report) -> report.status == ReportStatus.PENDING }
                    } else {
                        reportedPostsList // Show all reports regardless of status
                    }

                    println("Reports fetched: ${reportedPostsList.size}, filtered: ${filteredReports.size}")

                    // Sort with pending reports first
                    val sortedReports = filteredReports.sortedWith(compareBy { (_, report) ->
                        when (report.status) {
                            ReportStatus.PENDING -> 0
                            ReportStatus.ACCEPTED -> 1
                            ReportStatus.REVIEWED -> 2
                            ReportStatus.IGNORED -> 3
                        }
                    })

                    _reportedPosts.value = sortedReports
                }
            } catch (e: Exception) {
                println("Error loading reported posts: ${e.message}")
                _reportedPosts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteReportedPost(postId: String, reportId: String, showOnlyPending: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First update the report status
                postRepository.updateReportStatus(reportId, ReportStatus.ACCEPTED)

                // Then delete the post
                val result = postRepository.deletePost(postId)
                if (result.isSuccess) {
                    // Refresh the reported posts list with current filter setting
                    getReportedPosts(showOnlyPending)
                }
            } catch (e: Exception) {
                println("Error deleting reported post: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun ignoreReport(reportId: String, showOnlyPending: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                postRepository.updateReportStatus(reportId, ReportStatus.IGNORED)
                // Refresh the reported posts list with current filter setting
                getReportedPosts(showOnlyPending)
            } catch (e: Exception) {
                println("Error ignoring report: ${e.message}")
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