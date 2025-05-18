package data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Post(
    @Transient
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val mediaUrls: List<String> = emptyList(),
    val likeCount: Int = 0,
    val parentPostId: String? = null,
    val createdAt: String,
    // This could be computed property in your ViewModel
    @Transient
    val isLikedByCurrentUser: Boolean = false
)

// Like entity represents a user-post like relationship
@Serializable
data class Like(
    val userId: String,
    val postId: String,
    val timestamp: String
)